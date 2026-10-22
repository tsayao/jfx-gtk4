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
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class NativeSymbolLookup {
    static final boolean VERBOSE = Boolean.getBoolean("jdk.gtk.verbose");
    static final SymbolLookup GTK = lookupFirstAvailable("libgtk-4.so.1", "libgtk-4.so");
    static final SymbolLookup GDK = lookupFirstAvailable("libgdk-4.so.1", "libgdk-4.so");
    static final SymbolLookup GLIB = lookupFirstAvailable("libglib-2.0.so.0", "libglib-2.0.so");
    static final SymbolLookup GIO = lookupFirstAvailable("libgio-2.0.so.0", "libgio-2.0.so");
    static final SymbolLookup GOBJECT = lookupFirstAvailable("libgobject-2.0.so.0", "libgobject-2.0.so");
    static final SymbolLookup ADW = lookupFirstAvailable("libadwaita-1.so.0", "libadwaita-1.so");
    static final SymbolLookup GTK_STACK;
    private static final Linker LINKER = Linker.nativeLinker();

    static {
        SymbolLookup stack = SymbolLookup.loaderLookup();
        if (GTK != null) {
            stack = stack.or(GTK);
        }
        if (GDK != null) {
            stack = stack.or(GDK);
        }
        if (GLIB != null) {
            stack = stack.or(GLIB);
        }
        if (GIO != null) {
            stack = stack.or(GIO);
        }
        if (GOBJECT != null) {
            stack = stack.or(GOBJECT);
        }
        if (ADW != null) {
            stack = stack.or(ADW);
        }
        GTK_STACK = stack;
        if (VERBOSE) {
            logStatus();
        }
    }

    private NativeSymbolLookup() {
    }

    static SymbolLookup lookupFirstAvailable(String... libraryNames) {
        for (String libraryName : libraryNames) {
            try {
                SymbolLookup lookup = SymbolLookup.libraryLookup(libraryName, Arena.global());
                if (VERBOSE) {
                    System.err.println("[GTK4 FFI] Loaded library: " + libraryName);
                }
                return lookup;
            } catch (IllegalArgumentException | UnsatisfiedLinkError ignored) {
            }
        }
        if (VERBOSE) {
            System.err.println("[GTK4 FFI] Warning: none of the library candidates could be loaded: "
                    + String.join(", ", libraryNames));
        }
        return null;
    }

    static MethodHandle load(String name, FunctionDescriptor fd) {
        return load(name, fd, GTK_STACK);
    }

    static MethodHandle load(String name, FunctionDescriptor fd, SymbolLookup lookup) {
        MemorySegment sym = lookup.find(name)
                .orElseThrow(() -> new UnsatisfiedLinkError("Missing native symbol: " + name));
        if (VERBOSE) {
            System.err.println("[GTK4 FFI] Resolved symbol: " + name);
        }
        return LINKER.downcallHandle(sym, fd);
    }

    static MethodHandle loadOptional(String name, FunctionDescriptor fd) {
        return loadOptional(name, fd, GTK_STACK);
    }

    static MethodHandle loadOptional(String name, FunctionDescriptor fd, SymbolLookup lookup) {
        return lookup.find(name)
                .map(addr -> {
                    if (VERBOSE) {
                        System.err.println("[GTK4 FFI] Resolved optional symbol: " + name);
                    }
                    return LINKER.downcallHandle(addr, fd);
                })
                .orElseGet(() -> {
                    if (VERBOSE) {
                        System.err.println("[GTK4 FFI] Optional symbol not found: " + name);
                    }
                    return null;
                });
    }

    static Linker linker() {
        return LINKER;
    }

    static RuntimeException rethrow(Throwable t) {
        if (t instanceof Error e) {
            throw e;
        }
        if (t instanceof NativeCallException e) {
            return e;
        }
        return new NativeCallException("Native call failed", t);
    }

    static boolean isSymbolAvailable(String name) {
        return GTK_STACK.find(name).isPresent();
    }

    static String readString(MemorySegment str) {
        return (str != null && str.address() != 0) ? str.reinterpret(Long.MAX_VALUE).getString(0) : "";
    }

    static String readString(MemorySegment str, boolean free) {
        String result = readString(str);
        if (free && str != null && str.address() != 0) {
            free(str);
        }
        return result;
    }

    /**
     * Throws a {@link GErrorException} if the {@code GError*} stored in
     * {@code errorRef} is set, freeing the error.
     */
    static void checkError(MemorySegment errorRef) {
        MemorySegment error = errorRef.get(ValueLayout.ADDRESS, 0);
        if (error.address() == 0) {
            return;
        }
        MemorySegment e = error.reinterpret(GERROR_SIZE);
        int domain = e.get(ValueLayout.JAVA_INT, 0);
        int code = e.get(ValueLayout.JAVA_INT, 4);
        String message = readString(e.get(ValueLayout.ADDRESS, 8));
        String domainName;
        try {
            domainName = readString((MemorySegment) QUARK_TO_STRING.get().invokeExact(domain));
            ERROR_FREE.get().invokeExact(error);
        } catch (Throwable t) {
            throw rethrow(t);
        }
        throw new GErrorException(domainName, code, message);
    }

    // struct GError { GQuark domain; gint code; gchar *message; }
    private static final long GERROR_SIZE = 16;

    private static final LazyHandle FREE = new LazyHandle("g_free",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    private static final LazyHandle ERROR_FREE = new LazyHandle("g_error_free",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    private static final LazyHandle QUARK_TO_STRING = new LazyHandle("g_quark_to_string",
            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_INT));

    static String[] readStringArray(MemorySegment strv, boolean free) {
        if (strv == null || strv.address() == 0) {
            return new String[0];
        }
        MemorySegment array = strv.reinterpret(Long.MAX_VALUE);
        List<String> result = new ArrayList<>();
        for (long offset = 0; ; offset += ValueLayout.ADDRESS.byteSize()) {
            MemorySegment str = array.get(ValueLayout.ADDRESS, offset);
            if (str.address() == 0) {
                break;
            }
            result.add(readString(str));
        }
        if (free) {
            try {
                STRFREEV.get().invokeExact(strv);
            } catch (Throwable t) {
                throw rethrow(t);
            }
        }
        return result.toArray(new String[0]);
    }

    static byte[] readBytes(MemorySegment data, long length, boolean free) {
        if (data == null || data.address() == 0) {
            return new byte[0];
        }
        byte[] result = data.reinterpret(length).toArray(ValueLayout.JAVA_BYTE);
        if (free) {
            free(data);
        }
        return result;
    }

    static MemorySegment allocateBytes(Arena arena, byte[] bytes) {
        return bytes == null ? MemorySegment.NULL : arena.allocateFrom(ValueLayout.JAVA_BYTE, bytes);
    }

    static <T> MemorySegment allocatePointers(Arena arena, T[] items, Function<T, MemorySegment> pointer) {
        if (items == null) {
            return MemorySegment.NULL;
        }
        MemorySegment array = arena.allocate(ValueLayout.ADDRESS, Math.max(items.length, 1));
        for (int i = 0; i < items.length; i++) {
            array.setAtIndex(ValueLayout.ADDRESS, i, items[i] != null ? pointer.apply(items[i]) : MemorySegment.NULL);
        }
        return array;
    }

    static MemorySegment allocateStringArray(Arena arena, String[] strings) {
        if (strings == null) {
            return MemorySegment.NULL;
        }
        MemorySegment array = arena.allocate(ValueLayout.ADDRESS, strings.length + 1L);
        for (int i = 0; i < strings.length; i++) {
            array.setAtIndex(ValueLayout.ADDRESS, i, arena.allocateFrom(strings[i]));
        }
        array.setAtIndex(ValueLayout.ADDRESS, strings.length, MemorySegment.NULL);
        return array;
    }

    private static final LazyHandle STRFREEV = new LazyHandle("g_strfreev",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));

    private static void free(MemorySegment mem) {
        try {
            FREE.get().invokeExact(mem);
        } catch (Throwable t) {
            throw rethrow(t);
        }
    }

    private static void logStatus() {
        System.err.println("[GTK4 FFI] === Native Library Status ===");
        logLib("GTK4", GTK);
        logLib("GDK4", GDK);
        logLib("GLib", GLIB);
        logLib("GIO", GIO);
        logLib("GObject", GOBJECT);
        System.err.println("[GTK4 FFI] =============================");
    }

    private static void logLib(String label, Object handle) {
        System.err.println("[GTK4 FFI]   " + label + ": " + (handle != null ? "OK" : "NOT AVAILABLE"));
    }
}
