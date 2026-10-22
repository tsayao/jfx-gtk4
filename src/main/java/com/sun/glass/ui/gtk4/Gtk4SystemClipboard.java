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

import com.sun.glass.ui.Clipboard;
import com.sun.glass.ui.SystemClipboard;
import com.sun.glass.ui.gtk4.wrappers.GAsyncResult;
import com.sun.glass.ui.gtk4.wrappers.GInputStream;
import com.sun.glass.ui.gtk4.wrappers.GLib;
import com.sun.glass.ui.gtk4.wrappers.GObject;
import com.sun.glass.ui.gtk4.wrappers.GdkClipboard;
import com.sun.glass.ui.gtk4.wrappers.GdkContentFormats;
import com.sun.glass.ui.gtk4.wrappers.GdkContentProvider;
import com.sun.glass.ui.gtk4.wrappers.GdkDisplay;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;

/**
 * The system clipboard, backed by the display's {@code GdkClipboard}.
 */
final class Gtk4SystemClipboard extends SystemClipboard implements Gtk4DataTransfer.Source {

    private final GdkClipboard clipboard;
    private final Gtk4SignalHelper signals = new Gtk4SignalHelper(Arena.global(), MethodHandles.lookup());

    Gtk4SystemClipboard() {
        super(Clipboard.SYSTEM);
        clipboard = GdkDisplay.getDefault().getClipboard();
        signals.connect(clipboard, "changed", this, "onChanged", void.class, GObject.class);
    }

    @Override
    protected void close() {
        super.close();
        signals.disconnectAllSignals();
    }

    @SuppressWarnings("unused")
    void onChanged(GObject source) {
        if (!clipboard.isLocal()) {
            contentChanged();
        }
    }

    @Override
    protected boolean isOwner() {
        return clipboard.isLocal();
    }

    @Override
    protected void pushToSystem(HashMap<String, Object> cacheData, int supportedActions) {
        if (cacheData == null || cacheData.isEmpty()) {
            clipboard.setContent(null);
            return;
        }
        GdkContentProvider provider = Gtk4DataTransfer.createProvider(cacheData);
        boolean set = clipboard.setContent(provider);
        provider.unref();
        Gtk4Log.log(Gtk4Log.Category.CLIPBOARD, "pushToSystem: set=%b local=%b formats=%s",
                set, clipboard.isLocal(), clipboard.getFormats().toString_());
    }

    @Override
    protected void pushTargetActionToSystem(int actionDone) {
    }

    @Override
    protected Object popFromSystem(String mimeType) {
        return Gtk4DataTransfer.read(this, mimeType);
    }

    @Override
    protected int supportedSourceActionsFromSystem() {
        return ACTION_COPY;
    }

    @Override
    protected String[] mimesFromSystem() {
        return Gtk4DataTransfer.getMimeTypes(this);
    }

    // Gtk4DataTransfer.Source

    @Override
    public GdkContentFormats getFormats() {
        return clipboard.getFormats();
    }

    @Override
    public void readAsync(String[] mimeTypes, MemorySegment callback) {
        clipboard.readAsync(mimeTypes, GLib.PRIORITY_DEFAULT, null, callback, null);
    }

    @Override
    public GInputStream readFinish(GAsyncResult result) {
        return clipboard.readFinish(result, null);
    }
}
