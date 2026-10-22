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

import com.sun.prism.PixelFormat;
import com.sun.prism.impl.BaseResourcePool;
import com.sun.prism.impl.PrismSettings;
import com.sun.prism.impl.TextureResourcePool;

class VKVramPool extends BaseResourcePool<VKTextureData>
                 implements TextureResourcePool<VKTextureData> {

    private static final VKVramPool theInstance = new VKVramPool();

    static VKVramPool getInstance() {
        return theInstance;
    }

    private VKVramPool() {
        super(PrismSettings.targetVram, PrismSettings.maxVram);
    }

    @Override
    public long estimateTextureSize(int width, int height, PixelFormat format) {
        return (long) width * height * format.getBytesPerPixelUnit();
    }

    @Override
    public long estimateRTTextureSize(int width, int height, boolean hasDepth) {
        return (long) width * height * 4L;
    }

    @Override
    public long size(VKTextureData resource) {
        return resource.getSize();
    }

    @Override
    public String toString() {
        return "Vulkan Vram Pool";
    }
}
