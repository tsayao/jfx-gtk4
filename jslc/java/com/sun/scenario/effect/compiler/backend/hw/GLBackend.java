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
import com.sun.scenario.effect.compiler.tree.JSLVisitor;

/**
 * Generates OpenGL 4.5 core GLSL fragment shaders. They are those of the
 * Vulkan backend without the descriptor sets, which OpenGL does not have: the
 * parameters are in the std140 uniform block at binding 0 and the samplers at
 * binding {@code 1 + register}, with the same layout. The OpenGL pipeline
 * renders with an upper-left origin (glClipControl), so pixcoord is computed
 * as for Vulkan.
 */
public class GLBackend extends VKBackend {

    public GLBackend(JSLParser parser, JSLVisitor visitor) {
        super(parser, visitor);
    }

    @Override
    public String getShader() {
        return super.getShader()
            .replace("#version 450\n", "#version 450 core\n")
            .replace("set = 0, ", "");
    }
}
