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

import com.sun.prism.Graphics;
import com.sun.prism.Material;
import com.sun.prism.impl.BaseMeshView;
import com.sun.prism.impl.Disposer;

/**
 * A mesh with a material, lights and render state.
 */
class VKMeshView extends BaseMeshView {

    private static int count;

    private final VKContext context;
    private final VKMesh mesh;
    private VKPhongMaterial material;
    private int cullingMode = VKDevice.CULL_BACK;
    private boolean wireframe;
    private float ambientLightRed;
    private float ambientLightGreen;
    private float ambientLightBlue;
    // Up to three lights, as in the other pipelines
    private final VKLight[] lights = new VKLight[3];

    private VKMeshView(VKContext context, VKMesh mesh) {
        super(new Disposer.Record() {
            @Override
            public void dispose() {
            }
        });
        this.context = context;
        this.mesh = mesh;
        count++;
    }

    static VKMeshView create(VKContext context, VKMesh mesh) {
        return new VKMeshView(context, mesh);
    }

    VKMesh getMesh() {
        return mesh;
    }

    VKPhongMaterial getMaterial() {
        return material;
    }

    int getCullingMode() {
        return cullingMode;
    }

    boolean isWireframe() {
        return wireframe;
    }

    float getAmbientLightRed() {
        return ambientLightRed;
    }

    float getAmbientLightGreen() {
        return ambientLightGreen;
    }

    float getAmbientLightBlue() {
        return ambientLightBlue;
    }

    VKLight[] getLights() {
        return lights;
    }

    @Override
    public void setCullingMode(int mode) {
        if (mode == CULL_BACK) {
            cullingMode = VKDevice.CULL_BACK;
        } else if (mode == CULL_FRONT) {
            cullingMode = VKDevice.CULL_FRONT;
        } else {
            cullingMode = VKDevice.CULL_NONE;
        }
    }

    @Override
    public void setMaterial(Material material) {
        this.material = (VKPhongMaterial) material;
    }

    @Override
    public void setWireframe(boolean wireframe) {
        this.wireframe = wireframe;
    }

    @Override
    public void setAmbientLight(float r, float g, float b) {
        ambientLightRed = r;
        ambientLightGreen = g;
        ambientLightBlue = b;
    }

    @Override
    public void setLight(int index, float x, float y, float z, float r, float g, float b, float w,
                         float ca, float la, float qa, float isAttenuated, float maxRange,
                         float dirX, float dirY, float dirZ, float innerAngle, float outerAngle, float falloff) {
        if (index >= 0 && index < lights.length) {
            lights[index] = new VKLight(x, y, z, r, g, b, w, ca, la, qa, isAttenuated, maxRange,
                    dirX, dirY, dirZ, innerAngle, outerAngle, falloff);
        }
    }

    @Override
    public void render(Graphics g) {
        if (material == null) {
            return;
        }
        material.lockTextureMaps();
        context.renderMeshView(this, g);
        material.unlockTextureMaps();
    }

    public int getCount() {
        return count;
    }

    @Override
    public void dispose() {
        material = null;
        disposerRecord.dispose();
        count--;
    }
}
