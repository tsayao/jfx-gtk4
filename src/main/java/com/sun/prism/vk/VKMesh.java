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

import com.sun.prism.impl.BaseMesh;
import com.sun.prism.impl.Disposer;

/**
 * The vertex and index buffers of a mesh.
 */
class VKMesh extends BaseMesh {

    private static int count;

    private final MeshRecord record;

    private VKMesh(MeshRecord record) {
        super(record);
        this.record = record;
        count++;
    }

    static VKMesh create(VKContext context) {
        return new VKMesh(new MeshRecord(context.getDevice()));
    }

    VKDevice.MeshBuffers getBuffers() {
        return record.buffers;
    }

    @Override
    public boolean buildNativeGeometry(float[] vertexBuffer, int vertexBufferLength,
                                       int[] indexBuffer, int indexBufferLength) {
        return record.build(vertexBuffer, vertexBufferLength, indexBuffer, indexBufferLength);
    }

    @Override
    public boolean buildNativeGeometry(float[] vertexBuffer, int vertexBufferLength,
                                       short[] indexBuffer, int indexBufferLength) {
        return record.build(vertexBuffer, vertexBufferLength, indexBuffer, indexBufferLength);
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public void dispose() {
        disposerRecord.dispose();
        count--;
    }

    private static final class MeshRecord implements Disposer.Record {
        private final VKDevice device;
        VKDevice.MeshBuffers buffers;

        MeshRecord(VKDevice device) {
            this.device = device;
        }

        boolean build(float[] vertices, int vertexCount, Object indices, int indexCount) {
            VKDevice.MeshBuffers built = device.createMeshBuffers(vertices, vertexCount, indices, indexCount);
            if (built == null) {
                return false;
            }
            // The previous buffers may still be used by frames in flight
            device.destroyMeshBuffers(buffers);
            buffers = built;
            return true;
        }

        @Override
        public void dispose() {
            device.destroyMeshBuffers(buffers);
            buffers = null;
        }
    }
}
