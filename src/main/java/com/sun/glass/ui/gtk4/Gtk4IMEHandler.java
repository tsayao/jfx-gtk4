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

import com.sun.glass.ui.View;
import com.sun.glass.ui.gtk4.wrappers.GdkEvent;
import com.sun.glass.ui.gtk4.wrappers.GdkRectangle;
import com.sun.glass.ui.gtk4.wrappers.GtkIMContext;
import com.sun.glass.ui.gtk4.wrappers.GtkIMMulticontext;
import com.sun.glass.ui.gtk4.wrappers.GtkWidget;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;

/**
 * Input method support for a view, following the GTK 3 toolkit.
 */
final class Gtk4IMEHandler {

    private static final Gtk4Log.Category IME = Gtk4Log.Category.IME;

    private final Gtk4View view;
    private final GtkWidget clientWidget;
    private final Gtk4SignalHelper signals;

    private GtkIMContext imContext;
    private boolean imeEnabled;
    private boolean imeOnPreedit;
    private boolean imeOnKeyEvent;
    private boolean imeInPreeditWindow;
    private boolean imeSendKeypress;

    Gtk4IMEHandler(Gtk4View view, GtkWidget clientWidget, Arena arena) {
        this.view = view;
        this.clientWidget = clientWidget;
        this.signals = new Gtk4SignalHelper(arena, MethodHandles.lookup());
    }

    void enableOrResetIME() {
        Gtk4Log.log(IME, "enableOrResetIME: enabled=%b onPreedit=%b", imeEnabled, imeOnPreedit);
        if (imeOnPreedit && imContext != null) {
            imContext.focusOut();
        }
        if (imContext == null) {
            imContext = GtkIMMulticontext.create();
            imContext.setClientWidget(clientWidget);
            imContext.setUsePreedit(true);
            signals.connect(imContext, "preedit-start", this, "onPreeditStart", void.class, GtkIMContext.class);
            signals.connect(imContext, "preedit-changed", this, "onPreeditChanged", void.class, GtkIMContext.class);
            signals.connect(imContext, "preedit-end", this, "onPreeditEnd", void.class, GtkIMContext.class);
            signals.connect(imContext, "commit", this, "onCommit", void.class, GtkIMContext.class, String.class);
        }
        imContext.reset();
        imContext.focusIn();
        imeOnPreedit = false;
        imeEnabled = true;
        imeOnKeyEvent = false;
        imeInPreeditWindow = false;
    }

    void disable() {
        Gtk4Log.log(IME, "disable");
        if (imContext != null) {
            signals.disconnectAllSignals();
            imContext.focusOut();
            imContext.setClientWidget(null);
            imContext.unref();
            imContext = null;
        }
        imeEnabled = false;
    }

    boolean isEnabled() {
        return imeEnabled;
    }

    void focusIn() {
        if (imContext != null) {
            imContext.focusIn();
        }
    }

    void focusOut() {
        if (imContext != null) {
            imContext.focusOut();
        }
    }

    /**
     * Passes a key event to the input method.
     *
     * @return whether the input method consumed the event
     */
    boolean filter(GdkEvent event) {
        if (!imeEnabled || event == null || event.isNull()) {
            return false;
        }
        imeOnKeyEvent = true;
        imeInPreeditWindow = imeOnPreedit;
        boolean filtered = imContext.filterKeypress(event);
        imeOnKeyEvent = false;
        if (filtered && imeSendKeypress) {
            // A commit of the key's own character: deliver the key event instead
            imeSendKeypress = false;
            return false;
        }
        Gtk4Log.log(IME, "filter: filtered=%b onPreedit=%b", filtered, imeOnPreedit);
        return filtered;
    }

    @SuppressWarnings("unused")
    void onPreeditStart(GtkIMContext context) {
        Gtk4Log.log(IME, "onPreeditStart");
        imeOnPreedit = true;
    }

    @SuppressWarnings("unused")
    void onPreeditChanged(GtkIMContext context) {
        imeOnPreedit = true;
        updateCaretPos();
        String[] text = new String[1];
        int[] cursor = new int[1];
        imContext.getPreeditString(text, MemorySegment.NULL, cursor);
        Gtk4Log.log(IME, "onPreeditChanged: text='%s' cursor=%d", text[0], cursor[0]);
        view.notifyInputMethodLinux(text[0] != null ? text[0] : "", 0, cursor[0], View.IME_ATTR_INPUT);
    }

    @SuppressWarnings("unused")
    void onPreeditEnd(GtkIMContext context) {
        Gtk4Log.log(IME, "onPreeditEnd");
        imeOnPreedit = false;
    }

    @SuppressWarnings("unused")
    void onCommit(GtkIMContext context, String str) {
        if (str == null) {
            return;
        }
        Gtk4Log.log(IME, "onCommit: '%s' inPreeditWindow=%b onKeyEvent=%b",
                str, imeInPreeditWindow, imeOnKeyEvent);
        if (imeInPreeditWindow || !imeOnKeyEvent) {
            view.notifyInputMethodLinux(str, str.length(), str.length(), (byte) 0);
        } else {
            imeSendKeypress = true;
        }
    }

    private void updateCaretPos() {
        double[] pos = view.getInputMethodCandidatePosition(0);
        if (pos != null && pos.length >= 2) {
            imContext.setCursorLocation(new GdkRectangle((int) pos[0], (int) pos[1], 0, 0));
        }
    }
}
