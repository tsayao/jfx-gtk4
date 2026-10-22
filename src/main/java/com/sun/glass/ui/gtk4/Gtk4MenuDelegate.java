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

import com.sun.glass.ui.Pixels;
import com.sun.glass.ui.delegate.MenuDelegate;
import com.sun.glass.ui.delegate.MenuItemDelegate;

final class Gtk4MenuDelegate implements MenuDelegate {
    @Override
    public boolean createMenu(String title, boolean enabled) {
        return true;
    }

    @Override
    public boolean setTitle(String title) {
        return true;
    }

    @Override
    public boolean setEnabled(boolean enabled) {
        return true;
    }

    @Override
    public boolean setPixels(Pixels pixels) {
        return true;
    }

    @Override
    public boolean insert(MenuItemDelegate item, int pos) {
        return true;
    }

    @Override
    public boolean insert(MenuDelegate menu, int pos) {
        return true;
    }

    @Override
    public boolean remove(MenuItemDelegate item, int pos) {
        return true;
    }

    @Override
    public boolean remove(MenuDelegate menu, int pos) {
        return true;
    }
}

