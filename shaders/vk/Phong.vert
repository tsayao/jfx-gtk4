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

// The Phong vertex shader of the ES2 pipeline (main.vert)

struct Light {
    vec4 pos;
    vec4 color;
    vec4 attn;
    vec4 dir;
    vec4 spot; // range, cosOuter, denom, falloff
};

layout(std140, set = 0, binding = 0) uniform PhongParams {
    mat4 viewProjectionMatrix;
    mat4 worldMatrix;
    vec4 camPos;
    vec4 ambientColor;
    vec4 diffuseColor;
    vec4 specularColor;
    Light lights[3];
} u;

// The mesh vertex: position, texture coordinates and the tangent frame as a quaternion
layout(location = 0) in vec3 pos;
layout(location = 1) in vec2 texCoords;
layout(location = 2) in vec4 tangent;

layout(location = 0) out vec2 oTexCoords;
layout(location = 1) out vec3 eyePos;
layout(location = 2) out vec4 lightTangentSpacePositions[3];
layout(location = 5) out vec4 lightTangentSpaceDirections[3];

vec3 getLocalVector(vec3 global, vec3 tangentFrame[3]) {
    return vec3(dot(global, tangentFrame[1]), dot(global, tangentFrame[2]), dot(global, tangentFrame[0]));
}

void main() {
    vec3 tangentFrame[3];

    vec4 worldPos = u.worldMatrix * vec4(pos, 1.0);

    vec3 t1 = tangent.xyz * tangent.yzx * 2.0;
    vec3 t2 = tangent.zxy * tangent.www * 2.0;
    vec3 t3 = tangent.xyz * tangent.xyz * 2.0;
    vec3 t4 = 1.0 - (t3 + t3.yzx);

    vec3 r1 = t1 + t2;
    vec3 r2 = t1 - t2;

    tangentFrame[0] = vec3(t4.y, r1.x, r2.z);
    tangentFrame[1] = vec3(r2.x, t4.z, r1.y);
    tangentFrame[2] = vec3(r1.z, r2.y, t4.x);
    tangentFrame[2] *= (tangent.w >= 0.0) ? 1.0 : -1.0;

    mat3 sWorldMatrix = mat3(u.worldMatrix[0].xyz, u.worldMatrix[1].xyz, u.worldMatrix[2].xyz);

    // Translate the tangent frame to world space
    tangentFrame[0] = sWorldMatrix * tangentFrame[0];
    tangentFrame[1] = sWorldMatrix * tangentFrame[1];
    tangentFrame[2] = sWorldMatrix * tangentFrame[2];

    vec3 eye = u.camPos.xyz - worldPos.xyz;
    eyePos = getLocalVector(eye, tangentFrame);

    for (int i = 0; i < 3; i++) {
        vec3 l = u.lights[i].pos.xyz - worldPos.xyz;
        lightTangentSpacePositions[i] = vec4(getLocalVector(l, tangentFrame) * u.lights[i].pos.w, 1.0);
        lightTangentSpaceDirections[i] = vec4(getLocalVector(u.lights[i].dir.xyz, tangentFrame), 1.0);
    }

    oTexCoords = texCoords;
    gl_Position = u.viewProjectionMatrix * worldPos;
}
