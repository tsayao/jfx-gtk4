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

/**
 * A light of a mesh view, as used by the Phong shader.
 */
class GLLight {

    final float x, y, z;
    final float r, g, b, w;
    final float ca, la, qa, isAttenuated;
    final float maxRange;
    final float dirX, dirY, dirZ;
    final float innerAngle, outerAngle, falloff;

    GLLight(float x, float y, float z, float r, float g, float b, float w, float ca, float la, float qa,
            float isAttenuated, float maxRange, float dirX, float dirY, float dirZ,
            float innerAngle, float outerAngle, float falloff) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.r = r;
        this.g = g;
        this.b = b;
        this.w = w;
        this.ca = ca;
        this.la = la;
        this.qa = qa;
        this.isAttenuated = isAttenuated;
        this.maxRange = maxRange;
        this.dirX = dirX;
        this.dirY = dirY;
        this.dirZ = dirZ;
        this.innerAngle = innerAngle;
        this.outerAngle = outerAngle;
        this.falloff = falloff;
    }

    boolean isPointLight() {
        return falloff == 0 && outerAngle == 180 && isAttenuated > 0.5;
    }

    boolean isDirectionalLight() {
        // w is 0 or 1; avoid an exact comparison of floats
        return isAttenuated < 0.5;
    }
}
