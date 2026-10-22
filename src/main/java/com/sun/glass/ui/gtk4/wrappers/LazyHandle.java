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
package com.sun.glass.ui.gtk4.wrappers;

import java.lang.foreign.FunctionDescriptor;
import java.lang.invoke.MethodHandle;

final class LazyHandle {

    private volatile MethodHandle handle;
    private volatile boolean loaded;
    private final String symbolName;
    private final FunctionDescriptor descriptor;
    private final boolean optional;

    LazyHandle(String symbolName, FunctionDescriptor descriptor) {
        this(symbolName, descriptor, false);
    }

    LazyHandle(String symbolName, FunctionDescriptor descriptor, boolean optional) {
        this.symbolName = symbolName;
        this.descriptor = descriptor;
        this.optional = optional;
    }

    MethodHandle get() {
        MethodHandle mh = resolve();
        if (mh == null) {
            throw new NativeCallException("Native function not available: " + symbolName);
        }
        return mh;
    }

    boolean isAvailable() {
        return resolve() != null;
    }

    private MethodHandle resolve() {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    handle = optional
                            ? NativeSymbolLookup.loadOptional(symbolName, descriptor)
                            : NativeSymbolLookup.load(symbolName, descriptor);
                    loaded = true;
                }
            }
        }
        return handle;
    }
}

