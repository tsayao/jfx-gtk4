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

import com.sun.prism.impl.Disposer;

/**
 * The native image of a texture, released when the texture is disposed.
 */
class VKTextureData implements Disposer.Record {

    private final VKContext context;
    private VKImage image;
    private final long size;

    VKTextureData(VKContext context, VKImage image, long size) {
        if (image == null) {
            throw new IllegalArgumentException("Texture cannot be null");
        }
        this.context = context;
        this.image = image;
        this.size = size;
    }

    VKImage getResource() {
        return image;
    }

    long getSize() {
        return size;
    }

    @Override
    public void dispose() {
        if (image != null && !context.isDisposed()) {
            image.dispose();
        }
        image = null;
    }
}
