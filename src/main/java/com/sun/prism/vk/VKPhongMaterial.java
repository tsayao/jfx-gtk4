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

import com.sun.javafx.PlatformUtil;
import com.sun.javafx.logging.PlatformLogger;
import com.sun.prism.Image;
import com.sun.prism.PhongMaterial;
import com.sun.prism.Texture;
import com.sun.prism.TextureMap;
import com.sun.prism.impl.BasePhongMaterial;
import com.sun.prism.impl.Disposer;
import com.sun.prism.paint.Color;

/**
 * A Phong material: colors and texture maps.
 */
class VKPhongMaterial extends BasePhongMaterial {

    private static int count;

    private final VKContext context;
    private final TextureMap[] maps = new TextureMap[MAX_MAP_TYPE];
    private final Texture[] textures = new Texture[MAX_MAP_TYPE];

    Color diffuseColor = Color.WHITE;
    Color specularColor = Color.WHITE;
    boolean specularColorSet;

    private VKPhongMaterial(VKContext context) {
        super(new Disposer.Record() {
            @Override
            public void dispose() {
            }
        });
        this.context = context;
        count++;
    }

    static VKPhongMaterial create(VKContext context) {
        return new VKPhongMaterial(context);
    }

    /**
     * The specular mode of the shader: none, texture, color or both.
     */
    int getSpecularMode(boolean hasMap) {
        if (hasMap) {
            return specularColorSet ? 3 : 1;
        }
        return specularColorSet ? 2 : 0;
    }

    /** The textures of the maps, valid while they are locked. */
    Texture[] getTextures() {
        for (int i = 0; i < MAX_MAP_TYPE; i++) {
            textures[i] = maps[i] != null ? maps[i].getTexture() : null;
        }
        return textures;
    }

    @Override
    public void setDiffuseColor(float r, float g, float b, float a) {
        diffuseColor = new Color(r, g, b, a);
    }

    @Override
    public void setSpecularColor(boolean set, float r, float g, float b, float a) {
        specularColorSet = set;
        specularColor = new Color(r, g, b, a);
    }

    @Override
    public void setTextureMap(TextureMap map) {
        maps[map.getType().ordinal()] = map;
    }

    private Texture setupTexture(TextureMap map, boolean useMipmap) {
        Image image = map.getImage();
        return image == null ? null
                : context.getResourceFactory().getCachedTexture(image, Texture.WrapMode.REPEAT, useMipmap);
    }

    @Override
    public void lockTextureMaps() {
        for (int i = 0; i < MAX_MAP_TYPE; i++) {
            if (maps[i] == null) {
                continue;
            }
            Texture texture = maps[i].getTexture();
            if (!maps[i].isDirty() && texture != null) {
                texture.lock();
                if (!texture.isSurfaceLost()) {
                    continue;
                }
            }
            boolean useMipmap = !PlatformUtil.isEmbedded()
                    && (i == PhongMaterial.DIFFUSE || i == PhongMaterial.SELF_ILLUM);
            texture = setupTexture(maps[i], useMipmap);
            maps[i].setTexture(texture);
            maps[i].setDirty(false);
            if (maps[i].getImage() != null && texture == null) {
                PlatformLogger.getLogger(PhongMaterial.class.getName()).warning(
                        "Warning: Low on texture resources. Cannot create texture.");
            }
        }
    }

    @Override
    public void unlockTextureMaps() {
        for (int i = 0; i < MAX_MAP_TYPE; i++) {
            Texture texture = maps[i] != null ? maps[i].getTexture() : null;
            if (texture != null) {
                texture.unlock();
            }
        }
    }

    public int getCount() {
        return count;
    }

    @Override
    public void dispose() {
        disposerRecord.dispose();
        count--;
    }
}
