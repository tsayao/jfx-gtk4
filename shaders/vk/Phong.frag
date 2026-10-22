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

// The Phong fragment shaders of the ES2 pipeline in one shader: the
// material and the number of lights are specialization constants

layout(constant_id = 0) const int DIFFUSE_MODE = 1;   // 1: color, 2: texture
layout(constant_id = 1) const int SPECULAR_MODE = 0;  // 0: none, 1: texture, 2: color, 3: mix
layout(constant_id = 2) const bool SELF_ILLUM = false;
layout(constant_id = 3) const bool BUMP = false;
layout(constant_id = 4) const int NUM_LIGHTS = 0;

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

layout(set = 0, binding = 1) uniform sampler2D diffuseTexture;
layout(set = 0, binding = 2) uniform sampler2D specularMap;
layout(set = 0, binding = 3) uniform sampler2D normalMap;
layout(set = 0, binding = 4) uniform sampler2D selfIllumTexture;

layout(location = 0) in vec2 oTexCoords;
layout(location = 1) in vec3 eyePos;
layout(location = 2) in vec4 lightTangentSpacePositions[3];
layout(location = 5) in vec4 lightTangentSpaceDirections[3];

layout(location = 0) out vec4 fragColor;

vec4 apply_diffuse() {
    if (DIFFUSE_MODE == 2) {
        return texture(diffuseTexture, oTexCoords) * u.diffuseColor;
    }
    return u.diffuseColor;
}

vec4 apply_specular() {
    if (SPECULAR_MODE == 1) {
        vec3 tSpec = texture(specularMap, oTexCoords).rgb;
        float sPower = u.specularColor.a * dot(tSpec, vec3(0.299, 0.587, 0.114));
        return vec4(tSpec, sPower);
    } else if (SPECULAR_MODE == 2) {
        return u.specularColor;
    } else if (SPECULAR_MODE == 3) {
        vec3 tSpec = texture(specularMap, oTexCoords).rgb;
        float sPower = u.specularColor.a * dot(tSpec, vec3(0.299, 0.587, 0.114));
        return vec4(tSpec * u.specularColor.rgb, sPower);
    }
    // 32 is the default specular power; pow(0, 0) is undefined
    return vec4(0.0, 0.0, 0.0, 32.0);
}

vec3 apply_normal() {
    if (BUMP) {
        return normalize(texture(normalMap, oTexCoords).xyz * 2.0 - 1.0);
    }
    return vec3(0.0, 0.0, 1.0);
}

vec4 apply_selfIllum() {
    if (SELF_ILLUM) {
        return texture(selfIllumTexture, oTexCoords);
    }
    return vec4(0.0);
}

float computeSpotlightFactor(vec3 l, vec3 lightDir, float cosOuter, float denom, float falloff) {
    if (falloff == 0.0 && cosOuter == -1.0) {
        // Point light (the outer angle is 180)
        return 1.0;
    }
    float cosAngle = dot(normalize(-lightDir), l);
    float cutoff = cosAngle - cosOuter;
    if (falloff != 0.0) {
        return pow(clamp(cutoff / denom, 0.0, 1.0), falloff);
    }
    return cutoff >= 0.0 ? 1.0 : 0.0;
}

void computeLight(int i, vec3 n, vec3 refl, float specPower, inout vec3 d, inout vec3 s) {
    Light light = u.lights[i];
    vec3 lightDir = lightTangentSpaceDirections[i].xyz;
    if (light.attn.w < 0.5) {
        // Directional light
        d += clamp(dot(n, -lightDir), 0.0, 1.0) * light.color.rgb;
        s += pow(clamp(dot(-refl, -lightDir), 0.0, 1.0), specPower) * light.color.rgb;
        return;
    }

    vec3 pos = lightTangentSpacePositions[i].xyz;
    float dist = length(pos);
    if (dist > light.spot.x) {
        return;
    }
    vec3 l = normalize(pos);

    float spotlightFactor = computeSpotlightFactor(l, lightDir, light.spot.y, light.spot.z, light.spot.w);
    float invAttnFactor = light.attn.x + light.attn.y * dist + light.attn.z * dist * dist;

    vec3 attenuatedColor = light.color.rgb * spotlightFactor / invAttnFactor;
    d += clamp(dot(n, l), 0.0, 1.0) * attenuatedColor;
    s += pow(clamp(dot(-refl, l), 0.0, 1.0), specPower) * attenuatedColor;
}

void main() {
    vec4 diffuse = apply_diffuse();
    if (diffuse.a == 0.0) {
        discard;
    }

    vec3 rez;
    if (NUM_LIGHTS == 0) {
        rez = u.ambientColor.rgb * diffuse.rgb;
    } else {
        vec3 n = apply_normal();
        vec3 refl = reflect(normalize(eyePos), n);
        vec3 d = vec3(0.0);
        vec3 s = vec3(0.0);
        vec4 specular = apply_specular();
        for (int i = 0; i < NUM_LIGHTS; i++) {
            computeLight(i, n, refl, specular.a, d, s);
        }
        rez = (u.ambientColor.rgb + d) * diffuse.rgb + s * specular.rgb;
    }
    rez += apply_selfIllum().rgb;

    fragColor = vec4(clamp(rez, 0.0, 1.0), diffuse.a);
}
