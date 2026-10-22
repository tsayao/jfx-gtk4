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

#version 450

// The Prism vertex: x, y, z, u0, v0, u1, v1 (binding 0) and an RGBA color (binding 1)
layout(location = 0) in vec3 position;
layout(location = 1) in vec2 inTexCoord0;
layout(location = 2) in vec2 inTexCoord1;
layout(location = 3) in vec4 inColor;

// The transform is stored row by row, so v * mvp applies it to v
layout(push_constant) uniform Transform {
    mat4 mvp;
} transform;

layout(location = 0) out vec4 perVertexColor;
layout(location = 1) out vec2 texCoord0;
layout(location = 2) out vec2 texCoord1;

void main() {
    gl_Position = vec4(position, 1.0) * transform.mvp;
    perVertexColor = inColor;
    texCoord0 = inTexCoord0;
    texCoord1 = inTexCoord1;
}
