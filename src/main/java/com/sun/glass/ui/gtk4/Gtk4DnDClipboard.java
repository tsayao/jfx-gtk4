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

import java.util.HashMap;

/**
 * The drag and drop clipboard: pushing data starts a drag, and the data of a
 * drop over one of the views is read from the drop.
 */
final class Gtk4DnDClipboard extends SystemClipboard {

    Gtk4DnDClipboard() {
        super(Clipboard.DND);
    }

    @Override
    protected void pushToSystem(HashMap<String, Object> cacheData, int supportedActions) {
        int performedAction = Gtk4DnD.executeDrag(cacheData, supportedActions);
        actionPerformed(performedAction);
    }

    @Override
    protected boolean isOwner() {
        return Gtk4DnD.isDragging() || Gtk4DnD.isLocalDrop();
    }

    @Override
    protected void pushTargetActionToSystem(int actionDone) {
    }

    @Override
    protected Object popFromSystem(String mimeType) {
        return Gtk4DataTransfer.read(Gtk4DnD.dropSource(), mimeType);
    }

    @Override
    protected int supportedSourceActionsFromSystem() {
        return Gtk4DnD.sourceActions();
    }

    @Override
    protected String[] mimesFromSystem() {
        return Gtk4DataTransfer.getMimeTypes(Gtk4DnD.dropSource());
    }
}
