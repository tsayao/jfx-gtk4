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
import com.sun.glass.ui.gtk4.wrappers.GBytes;
import com.sun.glass.ui.gtk4.wrappers.Gdk;
import com.sun.glass.ui.gtk4.wrappers.GdkMemoryTexture;
import com.sun.glass.ui.gtk4.wrappers.GdkTexture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

final class Gtk4Pixels extends Pixels {

    Gtk4Pixels(int width, int height, ByteBuffer data) {
        super(width, height, data);
    }

    Gtk4Pixels(int width, int height, ByteBuffer data, float scalex, float scaley) {
        super(width, height, data, scalex, scaley);
    }

    Gtk4Pixels(int width, int height, IntBuffer data) {
        super(width, height, data);
    }

    Gtk4Pixels(int width, int height, IntBuffer data, float scalex, float scaley) {
        super(width, height, data, scalex, scaley);
    }

    @Override
    protected void _fillDirectByteBuffer(ByteBuffer bb) {
        if (this.bytes != null) {
            this.bytes.rewind();
            bb.put(this.bytes);
            this.bytes.rewind();
        } else {
            this.ints.rewind();
            for (int i = 0; i < this.ints.capacity(); i++) {
                int data = this.ints.get();
                bb.put((byte) ((data) & 0xff));
                bb.put((byte) ((data >> 8) & 0xff));
                bb.put((byte) ((data >> 16) & 0xff));
                bb.put((byte) ((data >> 24) & 0xff));
            }
            this.ints.rewind();
        }
    }

    /**
     * Copies the pixels into a texture. The format is premultiplied BGRA in
     * memory order, which is what both {@code BYTE_BGRA_PRE} and little-endian
     * {@code INT_ARGB_PRE} produce.
     */
    static GdkTexture toTexture(Pixels pixels) {
        int width = pixels.getWidth();
        int height = pixels.getHeight();
        if (width <= 0 || height <= 0) {
            return null;
        }
        int count = width * height;
        byte[] data = new byte[count * 4];
        Object buffer = pixels.getPixels();
        if (buffer instanceof IntBuffer ints) {
            if (ints.capacity() < count) {
                return null;
            }
            ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(ints.slice(0, count));
        } else if (buffer instanceof ByteBuffer bytes) {
            if (bytes.capacity() < data.length) {
                return null;
            }
            bytes.get(0, data);
        } else {
            return null;
        }
        GBytes gbytes = GBytes.create(data);
        GdkMemoryTexture texture = GdkMemoryTexture.create(width, height,
                Gdk.MEMORY_B8G8R8A8_PREMULTIPLIED, gbytes, width * 4L);
        gbytes.unref();
        return texture.isNull() ? null : texture;
    }

    @Override
    protected void _attachInt(long ptr, int w, int h, IntBuffer ints, int[] array, int offset) {
        // No-op
    }

    @Override
    protected void _attachByte(long ptr, int w, int h, ByteBuffer bytes, byte[] array, int offset) {
        // No-op
    }
}

