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

import com.sun.glass.ui.Screen;
import com.sun.prism.Graphics;
import com.sun.prism.PixelFormat;
import com.sun.prism.RTTexture;
import com.sun.prism.ReadbackRenderTarget;
import com.sun.prism.Texture;

import java.lang.foreign.MemorySegment;
import java.nio.Buffer;
import java.nio.IntBuffer;

class GLRTTexture extends GLTexture<GLTextureData>
                  implements RTTexture, ReadbackRenderTarget, GLRenderTarget {

    private boolean opaque;
    private final boolean msaa;

    private GLRTTexture(GLContext context, GLTextureResource<GLTextureData> resource, WrapMode wrapMode,
                        int physicalWidth, int physicalHeight, int contentWidth, int contentHeight, boolean msaa) {
        super(context, resource, PixelFormat.BYTE_BGRA_PRE, wrapMode,
              physicalWidth, physicalHeight, 0, 0, contentWidth, contentHeight,
              contentWidth, contentHeight, false);
        this.msaa = msaa;
    }

    static GLRTTexture create(GLContext context, int physicalWidth, int physicalHeight,
                              int contentWidth, int contentHeight, WrapMode wrapMode, boolean msaa, long size) {
        GLDevice device = context.getDevice();
        int samples = msaa ? device.getMSAASamples() : 1;
        GLImage image = device.createImage(GLImage.FORMAT_BGRA8, physicalWidth, physicalHeight,
                samples, true, false);
        if (image == null) {
            return null;
        }
        GLTextureResource<GLTextureData> resource =
                new GLTextureResource<>(new GLTextureData(context, image, size));
        return new GLRTTexture(context, resource, wrapMode,
                physicalWidth, physicalHeight, contentWidth, contentHeight, samples > 1);
    }

    @Override
    public GLImage getTargetImage() {
        return getImage();
    }

    @Override
    public Texture getBackBuffer() {
        return this;
    }

    @Override
    public int[] getPixels() {
        int w = getContentWidth();
        int h = getContentHeight();
        int[] pixels = new int[w * h];
        readPixels(IntBuffer.wrap(pixels), 0, 0, w, h);
        return pixels;
    }

    @Override
    public boolean readPixels(Buffer pixels) {
        return readPixels(pixels, getContentX(), getContentY(), getContentWidth(), getContentHeight());
    }

    @Override
    public boolean readPixels(Buffer pixels, int x, int y, int width, int height) {
        if (!(pixels instanceof IntBuffer buf)) {
            return false;
        }
        getContext().flushVertexBuffer();
        if (buf.remaining() < width * height) {
            return false;
        }
        // B, G, R, A bytes are little-endian INT_ARGB_PRE values
        return getContext().getDevice().readImage(getImage(), MemorySegment.ofBuffer(buf), x, y, width, height);
    }

    @Override
    public boolean isVolatile() {
        return false;
    }

    @Override
    public Screen getAssociatedScreen() {
        return getContext().getAssociatedScreen();
    }

    @Override
    public Graphics createGraphics() {
        return GLGraphics.create(getContext(), this);
    }

    @Override
    public boolean isOpaque() {
        return opaque;
    }

    @Override
    public void setOpaque(boolean opaque) {
        this.opaque = opaque;
    }

    @Override
    public boolean isMSAA() {
        return msaa;
    }

    @Override
    public void update(com.sun.prism.Image img) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public void update(com.sun.prism.Image img, int dstx, int dsty) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public void update(com.sun.prism.Image img, int dstx, int dsty, int srcw, int srch) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public void update(com.sun.prism.Image img, int dstx, int dsty, int srcw, int srch, boolean skipFlush) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }

    @Override
    public void update(Buffer buffer, PixelFormat format, int dstx, int dsty, int srcx, int srcy,
                       int srcw, int srch, int srcscan, boolean skipFlush) {
        throw new UnsupportedOperationException("update() not supported for RTTextures");
    }
}
