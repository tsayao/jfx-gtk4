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
package com.sun.glass.ui.gtk4;

import com.sun.glass.ui.Application;
import com.sun.glass.ui.EventLoop;
import com.sun.glass.ui.gtk4.wrappers.GAsyncResult;
import com.sun.glass.ui.gtk4.wrappers.GErrorException;
import com.sun.glass.ui.gtk4.wrappers.GObject;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Runs GIO-style asynchronous operations synchronously, in a nested event loop.
 */
final class Gtk4Async {

    private static final FunctionDescriptor READY_CALLBACK =
            Gtk4SignalHelper.buildDescriptor(void.class, GObject.class, GAsyncResult.class, MemorySegment.class);

    private Gtk4Async() {
    }

    /**
     * Starts an operation and waits for it to complete.
     *
     * @param start starts the operation, given a {@code GAsyncReadyCallback}
     * @param finish completes the operation with its {@code GAsyncResult}
     * @return the result of {@code finish}
     * @throws GErrorException if the operation failed or was cancelled
     */
    static <T> T await(Consumer<MemorySegment> start, Function<GAsyncResult, T> finish) {
        Completion<T> completion = new Completion<>(finish);
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment callback = Gtk4SignalHelper.upcall(MethodHandles.lookup(),
                    MethodHandles.lookup().bind(completion, "ready",
                            MethodType.methodType(void.class, GObject.class, GAsyncResult.class)),
                    READY_CALLBACK, arena);
            start.accept(callback);
            if (!completion.done) {
                completion.loop = Application.GetApplication().createEventLoop();
                completion.loop.enter();
            }
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
        if (completion.error != null) {
            throw completion.error;
        }
        return completion.result;
    }

    private static final class Completion<T> {
        private final Function<GAsyncResult, T> finish;
        private T result;
        private RuntimeException error;
        private boolean done;
        private EventLoop loop;

        Completion(Function<GAsyncResult, T> finish) {
            this.finish = finish;
        }

        @SuppressWarnings("unused")
        void ready(GObject source, GAsyncResult res) {
            try {
                result = finish.apply(res);
            } catch (RuntimeException e) {
                error = e;
            }
            done = true;
            if (loop != null) {
                loop.leave(null);
            }
        }
    }
}
