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

package com.sun.prism.gl;

import java.lang.foreign.MemorySegment;

/**
 * An OpenGL texture: a texture, a render target with its framebuffer, a
 * multisampled render target or a buffer of the render node exported to the
 * compositor as a dma-buf. Render targets get a depth renderbuffer when they
 * are first rendered with the depth test.
 */
final class GLImage {

    // Formats, as used by GLTexture
    static final int FORMAT_BGRA8 = 0;
    static final int FORMAT_ALPHA8 = 1;
    static final int FORMAT_GRAY8 = 2;
    static final int FORMAT_RGBA32F = 3;

    final GLDevice device;
    final int texFormat;
    final int width;
    final int height;
    final int samples;
    final boolean renderTarget;
    final boolean exportable;
    final int bytesPerPixel;

    int texture;
    // The framebuffer of a render target, or of a texture read back or blitted
    int framebuffer;
    int depthRenderbuffer;

    // Exported images: the buffer of the render node and its EGL image
    MemorySegment bo = MemorySegment.NULL;
    MemorySegment eglImage = MemorySegment.NULL;
    int fd = -1;
    int stride;
    int fourcc;

    boolean disposed;

    GLImage(GLDevice device, int texFormat, int width, int height, int samples,
            boolean renderTarget, boolean exportable) {
        this.device = device;
        this.texFormat = texFormat;
        this.width = width;
        this.height = height;
        this.samples = samples;
        this.renderTarget = renderTarget;
        this.exportable = exportable;
        this.bytesPerPixel = switch (texFormat) {
            case FORMAT_ALPHA8, FORMAT_GRAY8 -> 1;
            case FORMAT_RGBA32F -> 16;
            default -> 4;
        };
    }

    boolean hasDepth() {
        return depthRenderbuffer != 0;
    }

    void dispose() {
        if (!disposed) {
            disposed = true;
            device.destroyImage(this);
        }
    }
}
