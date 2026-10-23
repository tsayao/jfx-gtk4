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

import com.sun.prism.ps.Shader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * A fragment shader of the JSL OpenGL backend, linked with the vertex shader
 * of the quads. Its parameters are stored in a std140 uniform block, whose
 * layout is generated along with the shader, as for Vulkan.
 */
class GLShader implements Shader {

    private record Uniform(int offset, int components, int arraySize, int arrayStride, boolean isFloat) {}

    private final GLContext context;
    private final GLDevice device;
    private final String name;
    private final boolean pixcoordUsed;
    private final Map<String, Uniform> uniforms;
    private final int blockSize;
    // The uniform block, copied to the ring buffer for each draw
    private final MemorySegment block;
    private int program;

    private GLShader(GLContext context, String name, int program, int blockSize, Map<String, Uniform> uniforms,
                     boolean pixcoordUsed) {
        this.context = context;
        this.device = context.getDevice();
        this.name = name;
        this.program = program;
        this.blockSize = blockSize;
        this.block = Arena.ofAuto().allocate(Math.max(blockSize, 16), 16);
        this.uniforms = uniforms;
        this.pixcoordUsed = pixcoordUsed;
    }

    /**
     * Creates a shader from its GLSL source and layout.
     */
    static GLShader create(GLContext context, String name, InputStream code, InputStream layout,
                           boolean pixcoordUsed) throws IOException {
        String source = new String(code.readAllBytes(), StandardCharsets.UTF_8);
        Map<String, Uniform> uniforms = new HashMap<>();
        int blockSize = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(layout, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] f = line.trim().split("\\s+");
                switch (f[0]) {
                    case "block" -> blockSize = Integer.parseInt(f[1]);
                    case "uniform" -> uniforms.put(f[1], new Uniform(Integer.parseInt(f[2]),
                            Integer.parseInt(f[3]), Integer.parseInt(f[4]), Integer.parseInt(f[5]),
                            "float".equals(f[6])));
                    default -> {
                        // Samplers use the binding of their texture unit
                    }
                }
            }
        }
        int program = context.getDevice().createQuadProgram(source, name);
        if (program == 0) {
            throw new IOException("Cannot build the OpenGL shader " + name);
        }
        return new GLShader(context, name, program, blockSize, uniforms, pixcoordUsed);
    }

    int getBlockSize() {
        return blockSize;
    }

    MemorySegment getUniforms() {
        return block;
    }

    int getProgram() {
        return program;
    }

    private void writeFloats(int offset, float[] values, int count) {
        if (offset < 0 || offset + count * 4L > blockSize) {
            return;
        }
        MemorySegment.copy(values, 0, block, ValueLayout.JAVA_FLOAT, offset, count);
    }

    private void writeInts(int offset, int[] values, int count) {
        if (offset < 0 || offset + count * 4L > blockSize) {
            return;
        }
        MemorySegment.copy(values, 0, block, ValueLayout.JAVA_INT, offset, count);
    }

    boolean isPixcoordUsed() {
        return pixcoordUsed;
    }

    @Override
    public void enable() {
        context.setShader(this);
    }

    @Override
    public void disable() {
    }

    @Override
    public boolean isValid() {
        return program != 0;
    }

    private void setFloats(String name, float... values) {
        Uniform u = uniforms.get(name);
        if (u == null || program == 0) {
            return;
        }
        if (u.isFloat()) {
            writeFloats(u.offset(), values, Math.min(values.length, u.components()));
        } else {
            int[] ints = new int[Math.min(values.length, u.components())];
            for (int i = 0; i < ints.length; i++) {
                ints[i] = (int) values[i];
            }
            writeInts(u.offset(), ints, ints.length);
        }
    }

    private void setInts(String name, int... values) {
        Uniform u = uniforms.get(name);
        if (u == null || program == 0) {
            // Samplers are bound to their texture unit, not set as constants
            return;
        }
        if (u.isFloat()) {
            float[] floats = new float[Math.min(values.length, u.components())];
            for (int i = 0; i < floats.length; i++) {
                floats[i] = values[i];
            }
            writeFloats(u.offset(), floats, floats.length);
        } else {
            writeInts(u.offset(), values, Math.min(values.length, u.components()));
        }
    }

    @Override
    public void setConstant(String name, int i0) {
        setInts(name, i0);
    }

    @Override
    public void setConstant(String name, int i0, int i1) {
        setInts(name, i0, i1);
    }

    @Override
    public void setConstant(String name, int i0, int i1, int i2) {
        setInts(name, i0, i1, i2);
    }

    @Override
    public void setConstant(String name, int i0, int i1, int i2, int i3) {
        setInts(name, i0, i1, i2, i3);
    }

    @Override
    public void setConstants(String name, IntBuffer buf, int off, int count) {
        Uniform u = uniforms.get(name);
        if (u == null || program == 0) {
            return;
        }
        // count four-component values, from the position of the buffer
        int[] values = new int[4 * count];
        buf.get(buf.position(), values, 0, values.length);
        for (int i = 0; i < count && (u.arraySize() == 0 || i < u.arraySize()); i++) {
            int[] element = new int[u.components()];
            System.arraycopy(values, 4 * i, element, 0, element.length);
            writeInts(u.offset() + i * Math.max(u.arrayStride(), 16), element, element.length);
        }
    }

    @Override
    public void setConstant(String name, float f0) {
        setFloats(name, f0);
    }

    @Override
    public void setConstant(String name, float f0, float f1) {
        setFloats(name, f0, f1);
    }

    @Override
    public void setConstant(String name, float f0, float f1, float f2) {
        setFloats(name, f0, f1, f2);
    }

    @Override
    public void setConstant(String name, float f0, float f1, float f2, float f3) {
        setFloats(name, f0, f1, f2, f3);
    }

    @Override
    public void setConstants(String name, FloatBuffer buf, int off, int count) {
        Uniform u = uniforms.get(name);
        if (u == null || program == 0) {
            return;
        }
        // count four-component values, from the position of the buffer
        int n = u.arraySize() > 0 ? Math.min(count, u.arraySize()) : Math.min(count, 1);
        float[] values = new float[4 * n];
        buf.get(buf.position(), values, 0, values.length);
        if (u.components() == 4) {
            // std140 arrays of vec4 are contiguous
            writeFloats(u.offset(), values, values.length);
        } else {
            for (int i = 0; i < n; i++) {
                float[] element = new float[u.components()];
                System.arraycopy(values, 4 * i, element, 0, element.length);
                writeFloats(u.offset() + i * u.arrayStride(), element, element.length);
            }
        }
    }

    @Override
    public void dispose() {
        if (program != 0) {
            if (device.getShader() == this) {
                device.setShader(null);
            }
            device.deleteProgram(program);
            program = 0;
        }
    }

    @Override
    public String toString() {
        return "GLShader[" + name + "]";
    }
}
