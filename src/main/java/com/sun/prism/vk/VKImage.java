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

import static com.sun.prism.vk.bindings.VK.*;

/**
 * A Vulkan image: a texture, a render target, a multisampled render target
 * or an image exported to the compositor as a dma-buf. Render targets get a
 * depth attachment when they are first rendered with the depth test.
 */
final class VKImage {

    // Formats, as used by VKTexture
    static final int FORMAT_BGRA8 = 0;
    static final int FORMAT_ALPHA8 = 1;
    static final int FORMAT_GRAY8 = 2;
    static final int FORMAT_RGBA32F = 3;

    final VKDevice device;
    final int texFormat;
    final int width;
    final int height;
    final int samples;
    final boolean renderTarget;
    final boolean exportable;
    final int format;
    final int bytesPerPixel;

    long image;
    long memory;
    long view;
    int layout = VK_IMAGE_LAYOUT_UNDEFINED;

    // Exported images
    int fd = -1;
    long rowPitch;
    long offset;
    // Whether the compositor owns the image
    boolean foreign;

    // The depth attachment of a render target
    long depthImage;
    long depthMemory;
    long depthView;
    int depthLayout = VK_IMAGE_LAYOUT_UNDEFINED;

    boolean disposed;

    VKImage(VKDevice device, int texFormat, int width, int height, int samples,
            boolean renderTarget, boolean exportable) {
        this.device = device;
        this.texFormat = texFormat;
        this.width = width;
        this.height = height;
        this.samples = samples;
        this.renderTarget = renderTarget;
        this.exportable = exportable;
        this.format = switch (texFormat) {
            case FORMAT_ALPHA8, FORMAT_GRAY8 -> VK_FORMAT_R8_UNORM;
            case FORMAT_RGBA32F -> VK_FORMAT_R32G32B32A32_SFLOAT;
            default -> VK_FORMAT_B8G8R8A8_UNORM;
        };
        this.bytesPerPixel = switch (texFormat) {
            case FORMAT_ALPHA8, FORMAT_GRAY8 -> 1;
            case FORMAT_RGBA32F -> 16;
            default -> 4;
        };
    }

    boolean hasDepth() {
        return depthView != 0L;
    }

    /** The index of the sample count: 1, 2, 4 or 8 samples. */
    int sampleIndex() {
        return Integer.numberOfTrailingZeros(samples);
    }

    void dispose() {
        if (!disposed) {
            disposed = true;
            device.destroyImage(this);
        }
    }
}
