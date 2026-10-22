/*
 * Copyright (c) 2026, Thiago Sayão. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  The copyright holder designates
 * this particular file as subject to the "Classpath" exception as provided
 * in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package com.sun.glass.ui.gtk4;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.EventLoop;
import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.gtk4.wrappers.GAsyncResult;
import com.sun.glass.ui.gtk4.wrappers.GBytes;
import com.sun.glass.ui.gtk4.wrappers.GInputStream;
import com.sun.glass.ui.gtk4.wrappers.GLib;
import com.sun.glass.ui.gtk4.wrappers.GObject;
import com.sun.glass.ui.gtk4.wrappers.Gdk;
import com.sun.glass.ui.gtk4.wrappers.GdkContentFormats;
import com.sun.glass.ui.gtk4.wrappers.GdkContentProvider;
import com.sun.glass.ui.gtk4.wrappers.GdkDevice;
import com.sun.glass.ui.gtk4.wrappers.GdkDisplay;
import com.sun.glass.ui.gtk4.wrappers.GdkDrag;
import com.sun.glass.ui.gtk4.wrappers.GdkDrop;
import com.sun.glass.ui.gtk4.wrappers.GdkMemoryTexture;
import com.sun.glass.ui.gtk4.wrappers.GdkTexture;
import com.sun.glass.ui.gtk4.wrappers.GtkDragIcon;
import com.sun.glass.ui.gtk4.wrappers.GtkPicture;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

/**
 * Drag and drop, following the GTK 3 toolkit: a drag started by the
 * application runs a nested event loop until it ends, and drops on the views
 * are delivered to Glass with their data read from the {@code GdkDrop}.
 */
final class Gtk4DnD {

    private static final Gtk4Log.Category DND = Gtk4Log.Category.DRAG_DROP;
    private static final int DRAG_IMAGE_MAX_SIZE = 320;

    // The drop currently over one of the views
    private static GdkDrop currentDrop;
    private static boolean dropped;

    // The drag started by the application
    private static GdkDrag currentDrag;
    private static int performedAction;
    private static EventLoop dragLoop;

    private Gtk4DnD() {
    }

    static int toGlassAction(int gdkActions) {
        int result = 0;
        if ((gdkActions & Gdk.ACTION_COPY) != 0) result |= Clipboard.ACTION_COPY;
        if ((gdkActions & Gdk.ACTION_MOVE) != 0) result |= Clipboard.ACTION_MOVE;
        if ((gdkActions & Gdk.ACTION_LINK) != 0) result |= Clipboard.ACTION_REFERENCE;
        return result;
    }

    static int toGdkActions(int glassActions) {
        int result = 0;
        if ((glassActions & Clipboard.ACTION_COPY) != 0) result |= Gdk.ACTION_COPY;
        if ((glassActions & Clipboard.ACTION_MOVE) != 0) result |= Gdk.ACTION_MOVE;
        if ((glassActions & Clipboard.ACTION_REFERENCE) != 0) result |= Gdk.ACTION_LINK;
        return result;
    }

    /**
     * Reduces actions to the single preferred one GDK expects.
     */
    static int preferredGdkAction(int glassActions) {
        if ((glassActions & Clipboard.ACTION_COPY) != 0) return Gdk.ACTION_COPY;
        if ((glassActions & Clipboard.ACTION_MOVE) != 0) return Gdk.ACTION_MOVE;
        if ((glassActions & Clipboard.ACTION_REFERENCE) != 0) return Gdk.ACTION_LINK;
        return 0;
    }

    // ---------------------------------------------------------------------
    // Target

    static void enter(GdkDrop drop) {
        currentDrop = drop;
        dropped = false;
    }

    static void leave(GdkDrop drop) {
        if (drop.equals(currentDrop) && !dropped) {
            currentDrop = null;
        }
    }

    static void drop(GdkDrop drop) {
        currentDrop = drop;
        dropped = true;
    }

    static void dropFinished(GdkDrop drop) {
        if (drop.equals(currentDrop)) {
            currentDrop = null;
            dropped = false;
        }
    }

    static boolean isDropDragged() {
        return currentDrop != null && dropped;
    }

    /**
     * The data source for the DnD clipboard while a drop is over a view.
     */
    static Gtk4DataTransfer.Source dropSource() {
        GdkDrop drop = currentDrop;
        if (drop == null) {
            throw new IllegalStateException(
                    "Cannot get the drag data. The drag pointer has not entered the application window");
        }
        return new Gtk4DataTransfer.Source() {
            @Override
            public GdkContentFormats getFormats() {
                return drop.getFormats();
            }

            @Override
            public void readAsync(String[] mimeTypes, MemorySegment callback) {
                drop.readAsync(mimeTypes, GLib.PRIORITY_DEFAULT, null, callback, null);
            }

            @Override
            public GInputStream readFinish(GAsyncResult result) {
                return drop.readFinish(result, null);
            }
        };
    }

    static int sourceActions() {
        return currentDrop != null ? toGlassAction(currentDrop.getActions()) : Clipboard.ACTION_NONE;
    }

    /**
     * Whether the current drop comes from a drag started by this application.
     */
    static boolean isLocalDrop() {
        return currentDrop != null && !currentDrop.getDrag().isNull();
    }

    // ---------------------------------------------------------------------
    // Source

    /**
     * Starts a drag with the given data and waits until it ends.
     *
     * @return the action performed by the target
     */
    static int executeDrag(Map<String, Object> data, int supportedActions) {
        int actions = toGdkActions(supportedActions);
        if (actions == 0) {
            return Clipboard.ACTION_NONE;
        }

        Gtk4View view = Gtk4View.getLastPressedView();
        double[] start = view != null ? view.getLastPressSurfacePosition() : null;
        Gtk4Window window = view != null && view.getWindow() instanceof Gtk4Window w ? w : null;
        if (start == null || window == null || window.getSurface() == null) {
            Gtk4Log.log(DND, "executeDrag: no pressed view to drag from");
            return Clipboard.ACTION_NONE;
        }

        GdkDevice pointer = GdkDisplay.getDefault().getDefaultSeat().getPointer_();
        GdkContentProvider content = Gtk4DataTransfer.createProvider(data);
        GdkDrag drag = GdkDrag.begin(window.getSurface(), pointer, content, actions, start[0], start[1]);
        content.unref();
        if (drag.isNull()) {
            Gtk4Log.log(DND, "executeDrag: gdk_drag_begin failed");
            return Clipboard.ACTION_NONE;
        }
        Gtk4Log.log(DND, "executeDrag: started actions=0x%x", actions);

        setDragIcon(drag, data);

        Gtk4SignalHelper signals = new Gtk4SignalHelper(Arena.ofConfined(), MethodHandles.lookup());
        DragListener listener = new DragListener();
        signals.connect(drag, "dnd-finished", listener, "onFinished", void.class, GObject.class);
        signals.connect(drag, "cancel", listener, "onCancel", void.class, GObject.class, int.class);

        currentDrag = drag;
        performedAction = Clipboard.ACTION_NONE;
        try {
            if (!listener.done) {
                dragLoop = Application.GetApplication().createEventLoop();
                dragLoop.enter();
            }
        } finally {
            dragLoop = null;
            currentDrag = null;
            signals.disconnectAllSignals();
            drag.unref();
            // The release that ended the drag was not delivered to the view
            view.resetPressedButtons();
        }
        Gtk4Log.log(DND, "executeDrag: finished action=%d", performedAction);
        return performedAction;
    }

    static boolean isDragging() {
        return currentDrag != null;
    }

    private static final class DragListener {
        boolean done;

        @SuppressWarnings("unused")
        void onFinished(GObject drag) {
            performedAction = toGlassAction(currentDrag.getSelectedAction());
            finish();
        }

        @SuppressWarnings("unused")
        void onCancel(GObject drag, int reason) {
            performedAction = Clipboard.ACTION_NONE;
            finish();
        }

        private void finish() {
            done = true;
            if (dragLoop != null) {
                dragLoop.leave(null);
            }
        }
    }

    /**
     * Shows the drag image, if any, as the drag icon.
     */
    private static void setDragIcon(GdkDrag drag, Map<String, Object> data) {
        GdkTexture texture = null;
        int width = 0;
        int height = 0;

        if (data.get(Clipboard.DRAG_IMAGE) instanceof ByteBuffer image) {
            // Two big-endian ints, width and height, followed by BGRA pixels
            // that are not premultiplied
            ByteBuffer buffer = image.duplicate().rewind().order(ByteOrder.BIG_ENDIAN);
            if (buffer.remaining() > 8) {
                width = buffer.getInt();
                height = buffer.getInt();
                if (width > 0 && height > 0 && (long) width * height * 4 <= buffer.remaining()) {
                    byte[] pixels = new byte[width * height * 4];
                    buffer.get(pixels);
                    GBytes bytes = GBytes.create(pixels);
                    texture = GdkMemoryTexture.create(width, height, Gdk.MEMORY_B8G8R8A8, bytes, width * 4L);
                    bytes.unref();
                }
            }
        } else if (data.get(Clipboard.RAW_IMAGE_TYPE) instanceof Pixels pixels) {
            texture = Gtk4Pixels.toTexture(pixels);
            width = pixels.getWidth();
            height = pixels.getHeight();
        }
        if (texture == null) {
            return;
        }

        int hotX = width / 2;
        int hotY = height / 2;
        if (data.get(Clipboard.DRAG_IMAGE_OFFSET) instanceof ByteBuffer offset) {
            ByteBuffer buffer = offset.duplicate().rewind().order(ByteOrder.BIG_ENDIAN);
            if (buffer.remaining() >= 8) {
                hotX = buffer.getInt();
                hotY = buffer.getInt();
            }
        }

        GtkPicture picture = GtkPicture.createForPaintable(texture);
        double scale = Math.min(1.0, Math.min((double) DRAG_IMAGE_MAX_SIZE / width,
                (double) DRAG_IMAGE_MAX_SIZE / height));
        picture.setSizeRequest((int) (width * scale), (int) (height * scale));
        GtkDragIcon.createGetForDrag(drag).setChild(picture);
        drag.setHotspot((int) (hotX * scale), (int) (hotY * scale));
        texture.unref();
    }
}
