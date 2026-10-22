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

package com.sun.scenario.effect.compiler.backend.hw;

import com.sun.scenario.effect.compiler.JSLParser;
import com.sun.scenario.effect.compiler.model.BaseType;
import com.sun.scenario.effect.compiler.model.Qualifier;
import com.sun.scenario.effect.compiler.model.Type;
import com.sun.scenario.effect.compiler.model.Variable;
import com.sun.scenario.effect.compiler.tree.JSLVisitor;
import com.sun.scenario.effect.compiler.tree.VarDecl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generates Vulkan GLSL (version 450) fragment shaders, to be compiled to
 * SPIR-V, together with a description of their resources.
 * <p>
 * The interface with the Prism vertex shader is:
 * <pre>
 *   location 0: vec4 perVertexColor
 *   location 1: vec2 texCoord0
 *   location 2: vec2 texCoord1
 * </pre>
 * The shader parameters are members of one std140 uniform block at binding 0
 * of descriptor set 0, and the samplers are combined image samplers at
 * binding {@code 1 + register}. The layout returned by {@link #getLayout()}
 * lists, one per line:
 * <pre>
 *   block &lt;size&gt;
 *   uniform &lt;name&gt; &lt;offset&gt; &lt;components&gt; &lt;arraySize&gt; &lt;arrayStride&gt; &lt;int|float&gt;
 *   sampler &lt;name&gt; &lt;binding&gt;
 * </pre>
 * Vulkan's framebuffer origin is the top-left corner, so {@code pixcoord} is
 * {@code gl_FragCoord.xy} minus the offset of the render target content.
 */
public class VKBackend extends ES2Backend {

    public static final int UNIFORM_BINDING = 0;
    public static final int FIRST_SAMPLER_BINDING = 1;

    private static final String PIXCOORD_OFFSET = "jsl_pixCoordOffset";

    private static final Map<String, String> VAR_MAP = Map.of(
        "pos0",            "texCoord0",
        "pos1",            "texCoord1",
        "color",           "jsl_fragColor",
        "jsl_vertexColor", "perVertexColor");

    private static final Map<String, String> FUNC_MAP = Map.of(
        "sample",  "texture",
        "ddx",     "dFdx",
        "ddy",     "dFdy",
        "intcast", "int");

    private record Uniform(String name, Type type, int arraySize) {}
    private record Sampler(String name, int binding) {}

    private final List<Uniform> uniforms = new ArrayList<>();
    private final List<Sampler> samplers = new ArrayList<>();

    public VKBackend(JSLParser parser, JSLVisitor visitor) {
        super(parser, visitor);
    }

    @Override
    protected String getVar(String v) {
        String s = VAR_MAP.get(v);
        return (s != null) ? s : v;
    }

    @Override
    protected String getFuncName(String f) {
        String s = FUNC_MAP.get(f);
        return (s != null) ? s : f;
    }

    @Override
    public void visitVarDecl(VarDecl d) {
        Variable var = d.getVariable();
        if (var.getQualifier() == Qualifier.PARAM) {
            // Declared in the header, in the uniform block or as samplers
            if (var.getType().getBaseType() == BaseType.SAMPLER) {
                samplers.add(new Sampler(var.getName(), FIRST_SAMPLER_BINDING + var.getReg()));
            } else {
                uniforms.add(new Uniform(var.getName(), var.getType(), var.isArray() ? var.getArraySize() : 0));
            }
            return;
        }
        super.visitVarDecl(d);
    }

    private List<Uniform> allUniforms() {
        List<Uniform> all = new ArrayList<>(uniforms);
        if (isPixcoordReferenced) {
            all.add(new Uniform(PIXCOORD_OFFSET, Type.FLOAT4, 0));
        }
        return all;
    }

    @Override
    protected String getHeader() {
        StringBuilder sb = new StringBuilder();
        sb.append("#version 450\n");
        sb.append("layout(location = 0) in vec4 perVertexColor;\n");
        sb.append("layout(location = 1) in vec2 texCoord0;\n");
        sb.append("layout(location = 2) in vec2 texCoord1;\n");
        sb.append("layout(location = 0) out vec4 jsl_fragColor;\n");

        List<Uniform> all = allUniforms();
        if (!all.isEmpty()) {
            sb.append("layout(std140, set = 0, binding = " + UNIFORM_BINDING + ") uniform JSLParams {\n");
            for (Uniform u : all) {
                sb.append("    " + getType(u.type()) + " " + u.name());
                if (u.arraySize() > 0) {
                    sb.append("[" + u.arraySize() + "]");
                }
                sb.append(";\n");
            }
            sb.append("};\n");
        }
        for (Sampler s : samplers) {
            sb.append("layout(set = 0, binding = " + s.binding() + ") uniform sampler2D " + s.name() + ";\n");
        }
        return sb.toString();
    }

    @Override
    public String getShader() {
        // Bypass the GLSL pixcoord handling, which assumes a bottom-left origin
        String shader = getHeader() + getBody();
        if (isPixcoordReferenced) {
            shader = shader.replace(MAIN, MAIN +
                "vec2 pixcoord = gl_FragCoord.xy - " + PIXCOORD_OFFSET + ".xy;\n");
        }
        return shader;
    }

    /**
     * The std140 layout of the uniform block and the sampler bindings.
     */
    public String getLayout() {
        StringBuilder sb = new StringBuilder();
        int offset = 0;
        StringBuilder members = new StringBuilder();
        for (Uniform u : allUniforms()) {
            int components = u.type().getNumFields();
            int size = 4 * components;
            int align = switch (components) {
                case 1 -> 4;
                case 2 -> 8;
                default -> 16;
            };
            int stride = 0;
            if (u.arraySize() > 0) {
                // std140: array elements are aligned to 16 bytes
                align = 16;
                stride = 16;
                size = stride * u.arraySize();
            }
            offset = (offset + align - 1) & -align;
            String kind = u.type().getBaseType() == BaseType.FLOAT ? "float" : "int";
            members.append("uniform " + u.name() + " " + offset + " " + components + " "
                + u.arraySize() + " " + stride + " " + kind + "\n");
            offset += size;
        }
        int blockSize = (offset + 15) & -16;
        sb.append("block " + blockSize + "\n");
        sb.append(members);
        for (Sampler s : samplers) {
            sb.append("sampler " + s.name() + " " + s.binding() + "\n");
        }
        return sb.toString();
    }
}
