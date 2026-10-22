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

import com.sun.glass.ui.gtk4.wrappers.Gtk;
import com.sun.glass.ui.gtk4.wrappers.GtkCssProvider;
import com.sun.glass.ui.gtk4.wrappers.GtkStyleContext;
import com.sun.glass.ui.gtk4.wrappers.GdkDisplay;

final class Gtk4CssUtils {
    private static boolean registered;

    private static final String CSS =
            """
            window.jfx-transparent,
            window.jfx-transparent * {
              background-color: transparent;
              background: none;
              border: none;
              border-radius: 0;
              box-shadow: none;
              margin: 0;
              padding: 0;
            }
            window.jfx-undecorated {
              border: none;
              border-radius: 0;
              box-shadow: none;
              margin: 0;
            }
            window.jfx-backdrop,
            window.jfx-backdrop headerbar {
              background-color: transparent;
              background: none;
              box-shadow: none;
            }
            window.jfx-dark windowcontrols > button {
              color: #ffffff;
            }
            window.jfx-light windowcontrols > button {
              color: rgba(0, 0, 6, 0.8);
            }
            popover.jfx-popover,
            popover.jfx-popover > contents {
              background-color: transparent;
              background: none;
              border: none;
              border-radius: 0;
              box-shadow: none;
              margin: 0;
              padding: 0;
              min-width: 0;
              min-height: 0;
            }""";

    private static final String BACKDROP_CSS =
            """
            window.jfx-backdrop {
              backdrop-filter: blur(24px);
            }""";

    private Gtk4CssUtils() {}

    static void ensureRegistered() {
        if (registered) {
            return;
        }
        registered = true;
        GtkCssProvider provider = GtkCssProvider.create();
        provider.loadFromString(Gtk4BackgroundEffect.isManagedByGtk() ? CSS + BACKDROP_CSS : CSS);
        GtkStyleContext.addProviderForDisplay(GdkDisplay.getDefault(), provider,
                Gtk.STYLE_PROVIDER_PRIORITY_USER);
    }
}

