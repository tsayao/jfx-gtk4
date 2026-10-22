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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

import static com.sun.glass.ui.gtk4.wrappers.NativeSymbolLookup.rethrow;

/**
 * GObject signal connection, which is not part of the introspected API.
 */
public final class GSignal {

    private GSignal() {
    }

    /**
     * Connects an upcall stub to a signal and returns the handler id.
     */
    public static long connect(GObject instance, String signal, MemorySegment callback) {
        try (Arena arena = Arena.ofConfined()) {
            return (long) H.CONNECT_DATA.get().invokeExact(instance.getPointer(), arena.allocateFrom(signal),
                    callback, MemorySegment.NULL, MemorySegment.NULL, 0);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    public static void disconnect(GObject instance, long handlerId) {
        try {
            H.HANDLER_DISCONNECT.get().invokeExact(instance.getPointer(), handlerId);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    private static final class H {
        static final LazyHandle CONNECT_DATA = new LazyHandle("g_signal_connect_data",
                FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
        static final LazyHandle HANDLER_DISCONNECT = new LazyHandle("g_signal_handler_disconnect",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG));
    }
}
