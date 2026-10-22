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

package com.sun.prism.vk;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SegmentAllocator;

/**
 * Allocates the short-lived native structures of the Vulkan calls from a
 * reusable block of memory. Callers take a mark before allocating and reset
 * to it when the call returns, which also makes nested use safe. It is only
 * used by the thread that renders.
 */
final class VKScratch implements SegmentAllocator, AutoCloseable {

    private static final long SIZE = 256 * 1024;

    private final Arena arena = Arena.ofShared();
    private final MemorySegment block = arena.allocate(SIZE, 16);
    private long offset;

    long mark() {
        return offset;
    }

    void reset(long mark) {
        offset = mark;
    }

    @Override
    public MemorySegment allocate(long byteSize, long byteAlignment) {
        long start = (offset + byteAlignment - 1) & -byteAlignment;
        if (start + byteSize > SIZE) {
            // Rare large requests are freed by the garbage collector
            return Arena.ofAuto().allocate(byteSize, byteAlignment);
        }
        offset = start + byteSize;
        return block.asSlice(start, byteSize);
    }

    @Override
    public void close() {
        arena.close();
    }
}
