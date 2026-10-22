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
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;

/**
 * A GObject type registered at runtime whose class and interface functions
 * are implemented by upcall stubs, which is not part of the introspected API.
 * Each instance has room for a key after the parent instance, which maps it
 * back to its Java peer.
 */
public final class GSubclass {

    private static final MethodHandle CALL_POINTER_FUNCTION =
            Linker.nativeLinker().downcallHandle(FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));

    private final long type;
    private final long keyOffset;

    private GSubclass(long type, long keyOffset) {
        this.type = type;
        this.keyOffset = keyOffset;
    }

    /**
     * Registers a static subtype of {@code parentType}, with an optional
     * class initializer.
     */
    public static GSubclass register(long parentType, String name, MemorySegment classInit) {
        try (Arena arena = Arena.ofConfined()) {
            GTypeQuery query = GTypeQuery.create(arena);
            GObjectFunctions.typeQuery(parentType, query);
            long keyOffset = (query.instanceSize() + ValueLayout.JAVA_LONG.byteSize() - 1)
                    & -ValueLayout.JAVA_LONG.byteSize();
            GTypeInfo info = GTypeInfo.create(arena);
            info.classSize((short) query.classSize());
            info.classInit(classInit != null ? classInit : MemorySegment.NULL);
            info.instanceSize((short) (keyOffset + ValueLayout.JAVA_LONG.byteSize()));
            long type = GObjectFunctions.typeRegisterStatic(parentType, name, info, 0);
            if (type == 0) {
                throw new IllegalStateException("Cannot register the type " + name);
            }
            return new GSubclass(type, keyOffset);
        }
    }

    public long getGType() {
        return type;
    }

    /**
     * Adds an interface whose virtual function table is set up by {@code interfaceInit}.
     */
    public void addInterface(long interfaceType, MemorySegment interfaceInit) {
        try (Arena arena = Arena.ofConfined()) {
            GInterfaceInfo info = GInterfaceInfo.create(arena);
            info.interfaceInit(interfaceInit);
            GObjectFunctions.typeAddInterfaceStatic(type, interfaceType, info);
        }
    }

    public GObject create() {
        return GObject.createV(type, 0, MemorySegment.NULL);
    }

    public void setKey(MemorySegment instance, long key) {
        instance.reinterpret(keyOffset + ValueLayout.JAVA_LONG.byteSize()).set(ValueLayout.JAVA_LONG, keyOffset, key);
    }

    public long getKey(MemorySegment instance) {
        return instance.reinterpret(keyOffset + ValueLayout.JAVA_LONG.byteSize()).get(ValueLayout.JAVA_LONG, keyOffset);
    }

    /**
     * Calls a virtual function that takes the instance and returns a pointer,
     * e.g. the implementation of the parent type that an upcall chains up to.
     */
    public static MemorySegment callPointerFunction(MemorySegment function, MemorySegment instance) {
        try {
            return (MemorySegment) CALL_POINTER_FUNCTION.invokeExact(function, instance);
        } catch (Throwable t) {
            throw new NativeCallException("Cannot call the function pointer", t);
        }
    }
}
