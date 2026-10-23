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

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.Optional;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BYTE;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_FLOAT;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

/**
 * The native functions of the OpenGL pipeline, through the foreign function
 * and memory API: EGL and GBM from their libraries, the OpenGL functions from
 * eglGetProcAddress, and a few functions of libc. Generated from a list of
 * signatures; the functions that read or write Java arrays are critical
 * downcalls with heap access.
 * <p>
 * The handles are resolved when the classes are initialized: a function that
 * is not found throws UnsupportedOperationException when it is called.
 */
final class GLNative {

    private static final Linker LINKER = Linker.nativeLinker();
    private static final Arena ARENA = Arena.global();
    private static final SymbolLookup LIBEGL = library("libEGL.so.1");
    private static final SymbolLookup LIBGBM = library("libgbm.so.1");

    private GLNative() {
    }

    private static SymbolLookup library(String name) {
        try {
            return SymbolLookup.libraryLookup(name, ARENA);
        } catch (IllegalArgumentException e) {
            return n -> Optional.empty();
        }
    }

    private static MethodHandle bind(Optional<MemorySegment> address, String name, FunctionDescriptor descriptor,
                                     boolean critical) {
        if (address.isEmpty() || address.get().address() == 0) {
            return null;
        }
        return critical
                ? LINKER.downcallHandle(address.get(), descriptor, Linker.Option.critical(true))
                : LINKER.downcallHandle(address.get(), descriptor);
    }

    private static MethodHandle egl(String name, FunctionDescriptor d, boolean critical) {
        return bind(LIBEGL.find(name), name, d, critical);
    }

    private static MethodHandle gbm(String name, FunctionDescriptor d, boolean critical) {
        return bind(LIBGBM.find(name), name, d, critical);
    }

    private static MethodHandle libc(String name, FunctionDescriptor d, boolean critical) {
        return bind(LINKER.defaultLookup().find(name), name, d, critical);
    }

    /** Extensions and OpenGL functions, from eglGetProcAddress. */
    private static MethodHandle proc(String name, FunctionDescriptor d, boolean critical) {
        MemorySegment address = EGL.eglGetProcAddress(ARENA.allocateFrom(name));
        return bind(Optional.of(address), name, d, critical);
    }

    private static RuntimeException failure(String name, Throwable t) {
        if (t instanceof NullPointerException) {
            return new UnsupportedOperationException(name + " is not available");
        }
        return t instanceof RuntimeException re ? re : new IllegalStateException(name, t);
    }

    /** EGL 1.5, from libEGL. */
    static final class EGL {
        private EGL() {
        }

        private static final MethodHandle EGLGETPROCADDRESS = egl("eglGetProcAddress", FunctionDescriptor.of(ADDRESS, ADDRESS), false);
        private static final MethodHandle EGLGETPLATFORMDISPLAY = egl("eglGetPlatformDisplay", FunctionDescriptor.of(ADDRESS, JAVA_INT, ADDRESS, ADDRESS), false);
        private static final MethodHandle EGLINITIALIZE = egl("eglInitialize", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS), false);
        private static final MethodHandle EGLTERMINATE = egl("eglTerminate", FunctionDescriptor.of(JAVA_INT, ADDRESS), false);
        private static final MethodHandle EGLBINDAPI = egl("eglBindAPI", FunctionDescriptor.of(JAVA_INT, JAVA_INT), false);
        private static final MethodHandle EGLCHOOSECONFIG = egl("eglChooseConfig", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS), false);
        private static final MethodHandle EGLCREATECONTEXT = egl("eglCreateContext", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS), false);
        private static final MethodHandle EGLDESTROYCONTEXT = egl("eglDestroyContext", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS), false);
        private static final MethodHandle EGLMAKECURRENT = egl("eglMakeCurrent", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS), false);
        private static final MethodHandle EGLGETCURRENTCONTEXT = egl("eglGetCurrentContext", FunctionDescriptor.of(ADDRESS), false);
        private static final MethodHandle EGLQUERYSTRING = egl("eglQueryString", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT), false);
        private static final MethodHandle EGLGETERROR = egl("eglGetError", FunctionDescriptor.of(JAVA_INT), false);

        static MemorySegment eglGetProcAddress(MemorySegment a0) {
            try {
                return (MemorySegment) EGLGETPROCADDRESS.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("eglGetProcAddress", t);
            }
        }

        static MemorySegment eglGetPlatformDisplay(int a0, MemorySegment a1, MemorySegment a2) {
            try {
                return (MemorySegment) EGLGETPLATFORMDISPLAY.invokeExact(a0, a1, a2);
            } catch (Throwable t) {
                throw failure("eglGetPlatformDisplay", t);
            }
        }

        static int eglInitialize(MemorySegment a0, MemorySegment a1, MemorySegment a2) {
            try {
                return (int) EGLINITIALIZE.invokeExact(a0, a1, a2);
            } catch (Throwable t) {
                throw failure("eglInitialize", t);
            }
        }

        static int eglTerminate(MemorySegment a0) {
            try {
                return (int) EGLTERMINATE.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("eglTerminate", t);
            }
        }

        static int eglBindAPI(int a0) {
            try {
                return (int) EGLBINDAPI.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("eglBindAPI", t);
            }
        }

        static int eglChooseConfig(MemorySegment a0, MemorySegment a1, MemorySegment a2, int a3, MemorySegment a4) {
            try {
                return (int) EGLCHOOSECONFIG.invokeExact(a0, a1, a2, a3, a4);
            } catch (Throwable t) {
                throw failure("eglChooseConfig", t);
            }
        }

        static MemorySegment eglCreateContext(MemorySegment a0, MemorySegment a1, MemorySegment a2, MemorySegment a3) {
            try {
                return (MemorySegment) EGLCREATECONTEXT.invokeExact(a0, a1, a2, a3);
            } catch (Throwable t) {
                throw failure("eglCreateContext", t);
            }
        }

        static int eglDestroyContext(MemorySegment a0, MemorySegment a1) {
            try {
                return (int) EGLDESTROYCONTEXT.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("eglDestroyContext", t);
            }
        }

        static int eglMakeCurrent(MemorySegment a0, MemorySegment a1, MemorySegment a2, MemorySegment a3) {
            try {
                return (int) EGLMAKECURRENT.invokeExact(a0, a1, a2, a3);
            } catch (Throwable t) {
                throw failure("eglMakeCurrent", t);
            }
        }

        static MemorySegment eglGetCurrentContext() {
            try {
                return (MemorySegment) EGLGETCURRENTCONTEXT.invokeExact();
            } catch (Throwable t) {
                throw failure("eglGetCurrentContext", t);
            }
        }

        static MemorySegment eglQueryString(MemorySegment a0, int a1) {
            try {
                return (MemorySegment) EGLQUERYSTRING.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("eglQueryString", t);
            }
        }

        static int eglGetError() {
            try {
                return (int) EGLGETERROR.invokeExact();
            } catch (Throwable t) {
                throw failure("eglGetError", t);
            }
        }
    }
    /** The EGL extensions. */
    static final class EGLX {
        private EGLX() {
        }

        private static final MethodHandle EGLCREATEIMAGEKHR = proc("eglCreateImageKHR", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS), false);
        private static final MethodHandle EGLDESTROYIMAGEKHR = proc("eglDestroyImageKHR", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS), false);
        private static final MethodHandle EGLCREATESYNCKHR = proc("eglCreateSyncKHR", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, ADDRESS), false);
        private static final MethodHandle EGLDESTROYSYNCKHR = proc("eglDestroySyncKHR", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS), false);
        private static final MethodHandle EGLDUPNATIVEFENCEFDANDROID = proc("eglDupNativeFenceFDANDROID", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS), false);
        private static final MethodHandle EGLQUERYDISPLAYATTRIBEXT = proc("eglQueryDisplayAttribEXT", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, ADDRESS), false);
        private static final MethodHandle EGLQUERYDEVICESTRINGEXT = proc("eglQueryDeviceStringEXT", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT), false);

        static MemorySegment eglCreateImageKHR(MemorySegment a0, MemorySegment a1, int a2, MemorySegment a3, MemorySegment a4) {
            try {
                return (MemorySegment) EGLCREATEIMAGEKHR.invokeExact(a0, a1, a2, a3, a4);
            } catch (Throwable t) {
                throw failure("eglCreateImageKHR", t);
            }
        }

        static int eglDestroyImageKHR(MemorySegment a0, MemorySegment a1) {
            try {
                return (int) EGLDESTROYIMAGEKHR.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("eglDestroyImageKHR", t);
            }
        }

        static MemorySegment eglCreateSyncKHR(MemorySegment a0, int a1, MemorySegment a2) {
            try {
                return (MemorySegment) EGLCREATESYNCKHR.invokeExact(a0, a1, a2);
            } catch (Throwable t) {
                throw failure("eglCreateSyncKHR", t);
            }
        }

        static int eglDestroySyncKHR(MemorySegment a0, MemorySegment a1) {
            try {
                return (int) EGLDESTROYSYNCKHR.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("eglDestroySyncKHR", t);
            }
        }

        static int eglDupNativeFenceFDANDROID(MemorySegment a0, MemorySegment a1) {
            try {
                return (int) EGLDUPNATIVEFENCEFDANDROID.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("eglDupNativeFenceFDANDROID", t);
            }
        }

        static int eglQueryDisplayAttribEXT(MemorySegment a0, int a1, MemorySegment a2) {
            try {
                return (int) EGLQUERYDISPLAYATTRIBEXT.invokeExact(a0, a1, a2);
            } catch (Throwable t) {
                throw failure("eglQueryDisplayAttribEXT", t);
            }
        }

        static MemorySegment eglQueryDeviceStringEXT(MemorySegment a0, int a1) {
            try {
                return (MemorySegment) EGLQUERYDEVICESTRINGEXT.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("eglQueryDeviceStringEXT", t);
            }
        }
    }
    /** OpenGL 4.5 core and GL_OES_EGL_image. */
    static final class GL {
        private GL() {
        }

        private static final MethodHandle GLGETERROR = proc("glGetError", FunctionDescriptor.of(JAVA_INT), false);
        private static final MethodHandle GLGETSTRING = proc("glGetString", FunctionDescriptor.of(ADDRESS, JAVA_INT), false);
        private static final MethodHandle GLGETSTRINGI = proc("glGetStringi", FunctionDescriptor.of(ADDRESS, JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLGETINTEGERV = proc("glGetIntegerv", FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS), false);
        private static final MethodHandle GLENABLE = proc("glEnable", FunctionDescriptor.ofVoid(JAVA_INT), false);
        private static final MethodHandle GLDISABLE = proc("glDisable", FunctionDescriptor.ofVoid(JAVA_INT), false);
        private static final MethodHandle GLBLENDFUNC = proc("glBlendFunc", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLDEPTHMASK = proc("glDepthMask", FunctionDescriptor.ofVoid(JAVA_BYTE), false);
        private static final MethodHandle GLDEPTHFUNC = proc("glDepthFunc", FunctionDescriptor.ofVoid(JAVA_INT), false);
        private static final MethodHandle GLCULLFACE = proc("glCullFace", FunctionDescriptor.ofVoid(JAVA_INT), false);
        private static final MethodHandle GLFRONTFACE = proc("glFrontFace", FunctionDescriptor.ofVoid(JAVA_INT), false);
        private static final MethodHandle GLPOLYGONMODE = proc("glPolygonMode", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLSCISSOR = proc("glScissor", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLVIEWPORT = proc("glViewport", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLCLEARCOLOR = proc("glClearColor", FunctionDescriptor.ofVoid(JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT, JAVA_FLOAT), false);
        private static final MethodHandle GLCLEARDEPTH = proc("glClearDepth", FunctionDescriptor.ofVoid(JAVA_DOUBLE), false);
        private static final MethodHandle GLCLEAR = proc("glClear", FunctionDescriptor.ofVoid(JAVA_INT), false);
        private static final MethodHandle GLCLIPCONTROL = proc("glClipControl", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLPIXELSTOREI = proc("glPixelStorei", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLFINISH = proc("glFinish", FunctionDescriptor.ofVoid(), false);
        private static final MethodHandle GLFLUSH = proc("glFlush", FunctionDescriptor.ofVoid(), false);
        private static final MethodHandle GLGENTEXTURES = proc("glGenTextures", FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS), false);
        private static final MethodHandle GLDELETETEXTURES = proc("glDeleteTextures", FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS), false);
        private static final MethodHandle GLBINDTEXTURE = proc("glBindTexture", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLACTIVETEXTURE = proc("glActiveTexture", FunctionDescriptor.ofVoid(JAVA_INT), false);
        private static final MethodHandle GLTEXSTORAGE2D = proc("glTexStorage2D", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLTEXSTORAGE2DMULTISAMPLE = proc("glTexStorage2DMultisample", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_BYTE), false);
        private static final MethodHandle GLTEXSUBIMAGE2D = proc("glTexSubImage2D", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS), true);
        private static final MethodHandle GLTEXPARAMETERI = proc("glTexParameteri", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLTEXPARAMETERIV = proc("glTexParameteriv", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, ADDRESS), false);
        private static final MethodHandle GLGENFRAMEBUFFERS = proc("glGenFramebuffers", FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS), false);
        private static final MethodHandle GLDELETEFRAMEBUFFERS = proc("glDeleteFramebuffers", FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS), false);
        private static final MethodHandle GLBINDFRAMEBUFFER = proc("glBindFramebuffer", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLFRAMEBUFFERTEXTURE2D = proc("glFramebufferTexture2D", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLFRAMEBUFFERRENDERBUFFER = proc("glFramebufferRenderbuffer", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLCHECKFRAMEBUFFERSTATUS = proc("glCheckFramebufferStatus", FunctionDescriptor.of(JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLBLITFRAMEBUFFER = proc("glBlitFramebuffer", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLREADPIXELS = proc("glReadPixels", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS), true);
        private static final MethodHandle GLGENRENDERBUFFERS = proc("glGenRenderbuffers", FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS), false);
        private static final MethodHandle GLDELETERENDERBUFFERS = proc("glDeleteRenderbuffers", FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS), false);
        private static final MethodHandle GLBINDRENDERBUFFER = proc("glBindRenderbuffer", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLRENDERBUFFERSTORAGEMULTISAMPLE = proc("glRenderbufferStorageMultisample", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLGENSAMPLERS = proc("glGenSamplers", FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS), false);
        private static final MethodHandle GLDELETESAMPLERS = proc("glDeleteSamplers", FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS), false);
        private static final MethodHandle GLSAMPLERPARAMETERI = proc("glSamplerParameteri", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLBINDSAMPLER = proc("glBindSampler", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLCREATESHADER = proc("glCreateShader", FunctionDescriptor.of(JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLSHADERSOURCE = proc("glShaderSource", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, ADDRESS, ADDRESS), false);
        private static final MethodHandle GLCOMPILESHADER = proc("glCompileShader", FunctionDescriptor.ofVoid(JAVA_INT), false);
        private static final MethodHandle GLGETSHADERIV = proc("glGetShaderiv", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, ADDRESS), false);
        private static final MethodHandle GLGETSHADERINFOLOG = proc("glGetShaderInfoLog", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, ADDRESS, ADDRESS), false);
        private static final MethodHandle GLDELETESHADER = proc("glDeleteShader", FunctionDescriptor.ofVoid(JAVA_INT), false);
        private static final MethodHandle GLCREATEPROGRAM = proc("glCreateProgram", FunctionDescriptor.of(JAVA_INT), false);
        private static final MethodHandle GLATTACHSHADER = proc("glAttachShader", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLLINKPROGRAM = proc("glLinkProgram", FunctionDescriptor.ofVoid(JAVA_INT), false);
        private static final MethodHandle GLGETPROGRAMIV = proc("glGetProgramiv", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, ADDRESS), false);
        private static final MethodHandle GLGETPROGRAMINFOLOG = proc("glGetProgramInfoLog", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, ADDRESS, ADDRESS), false);
        private static final MethodHandle GLUSEPROGRAM = proc("glUseProgram", FunctionDescriptor.ofVoid(JAVA_INT), false);
        private static final MethodHandle GLDELETEPROGRAM = proc("glDeleteProgram", FunctionDescriptor.ofVoid(JAVA_INT), false);
        private static final MethodHandle GLGENBUFFERS = proc("glGenBuffers", FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS), false);
        private static final MethodHandle GLDELETEBUFFERS = proc("glDeleteBuffers", FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS), false);
        private static final MethodHandle GLBINDBUFFER = proc("glBindBuffer", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLBUFFERDATA = proc("glBufferData", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT), true);
        private static final MethodHandle GLBUFFERSUBDATA = proc("glBufferSubData", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_LONG, JAVA_LONG, ADDRESS), true);
        private static final MethodHandle GLBINDBUFFERBASE = proc("glBindBufferBase", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GLGENVERTEXARRAYS = proc("glGenVertexArrays", FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS), false);
        private static final MethodHandle GLDELETEVERTEXARRAYS = proc("glDeleteVertexArrays", FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS), false);
        private static final MethodHandle GLBINDVERTEXARRAY = proc("glBindVertexArray", FunctionDescriptor.ofVoid(JAVA_INT), false);
        private static final MethodHandle GLENABLEVERTEXATTRIBARRAY = proc("glEnableVertexAttribArray", FunctionDescriptor.ofVoid(JAVA_INT), false);
        private static final MethodHandle GLVERTEXATTRIBPOINTER = proc("glVertexAttribPointer", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_BYTE, JAVA_INT, JAVA_LONG), false);
        private static final MethodHandle GLDRAWELEMENTS = proc("glDrawElements", FunctionDescriptor.ofVoid(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_LONG), false);
        private static final MethodHandle GLEGLIMAGETARGETTEXTURE2DOES = proc("glEGLImageTargetTexture2DOES", FunctionDescriptor.ofVoid(JAVA_INT, ADDRESS), false);

        static int glGetError() {
            try {
                return (int) GLGETERROR.invokeExact();
            } catch (Throwable t) {
                throw failure("glGetError", t);
            }
        }

        static MemorySegment glGetString(int a0) {
            try {
                return (MemorySegment) GLGETSTRING.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glGetString", t);
            }
        }

        static MemorySegment glGetStringi(int a0, int a1) {
            try {
                return (MemorySegment) GLGETSTRINGI.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glGetStringi", t);
            }
        }

        static void glGetIntegerv(int a0, MemorySegment a1) {
            try {
                GLGETINTEGERV.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glGetIntegerv", t);
            }
        }

        static void glEnable(int a0) {
            try {
                GLENABLE.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glEnable", t);
            }
        }

        static void glDisable(int a0) {
            try {
                GLDISABLE.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glDisable", t);
            }
        }

        static void glBlendFunc(int a0, int a1) {
            try {
                GLBLENDFUNC.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glBlendFunc", t);
            }
        }

        static void glDepthMask(byte a0) {
            try {
                GLDEPTHMASK.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glDepthMask", t);
            }
        }

        static void glDepthFunc(int a0) {
            try {
                GLDEPTHFUNC.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glDepthFunc", t);
            }
        }

        static void glCullFace(int a0) {
            try {
                GLCULLFACE.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glCullFace", t);
            }
        }

        static void glFrontFace(int a0) {
            try {
                GLFRONTFACE.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glFrontFace", t);
            }
        }

        static void glPolygonMode(int a0, int a1) {
            try {
                GLPOLYGONMODE.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glPolygonMode", t);
            }
        }

        static void glScissor(int a0, int a1, int a2, int a3) {
            try {
                GLSCISSOR.invokeExact(a0, a1, a2, a3);
            } catch (Throwable t) {
                throw failure("glScissor", t);
            }
        }

        static void glViewport(int a0, int a1, int a2, int a3) {
            try {
                GLVIEWPORT.invokeExact(a0, a1, a2, a3);
            } catch (Throwable t) {
                throw failure("glViewport", t);
            }
        }

        static void glClearColor(float a0, float a1, float a2, float a3) {
            try {
                GLCLEARCOLOR.invokeExact(a0, a1, a2, a3);
            } catch (Throwable t) {
                throw failure("glClearColor", t);
            }
        }

        static void glClearDepth(double a0) {
            try {
                GLCLEARDEPTH.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glClearDepth", t);
            }
        }

        static void glClear(int a0) {
            try {
                GLCLEAR.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glClear", t);
            }
        }

        static void glClipControl(int a0, int a1) {
            try {
                GLCLIPCONTROL.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glClipControl", t);
            }
        }

        static void glPixelStorei(int a0, int a1) {
            try {
                GLPIXELSTOREI.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glPixelStorei", t);
            }
        }

        static void glFinish() {
            try {
                GLFINISH.invokeExact();
            } catch (Throwable t) {
                throw failure("glFinish", t);
            }
        }

        static void glFlush() {
            try {
                GLFLUSH.invokeExact();
            } catch (Throwable t) {
                throw failure("glFlush", t);
            }
        }

        static void glGenTextures(int a0, MemorySegment a1) {
            try {
                GLGENTEXTURES.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glGenTextures", t);
            }
        }

        static void glDeleteTextures(int a0, MemorySegment a1) {
            try {
                GLDELETETEXTURES.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glDeleteTextures", t);
            }
        }

        static void glBindTexture(int a0, int a1) {
            try {
                GLBINDTEXTURE.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glBindTexture", t);
            }
        }

        static void glActiveTexture(int a0) {
            try {
                GLACTIVETEXTURE.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glActiveTexture", t);
            }
        }

        static void glTexStorage2D(int a0, int a1, int a2, int a3, int a4) {
            try {
                GLTEXSTORAGE2D.invokeExact(a0, a1, a2, a3, a4);
            } catch (Throwable t) {
                throw failure("glTexStorage2D", t);
            }
        }

        static void glTexStorage2DMultisample(int a0, int a1, int a2, int a3, int a4, byte a5) {
            try {
                GLTEXSTORAGE2DMULTISAMPLE.invokeExact(a0, a1, a2, a3, a4, a5);
            } catch (Throwable t) {
                throw failure("glTexStorage2DMultisample", t);
            }
        }

        static void glTexSubImage2D(int a0, int a1, int a2, int a3, int a4, int a5, int a6, int a7, MemorySegment a8) {
            try {
                GLTEXSUBIMAGE2D.invokeExact(a0, a1, a2, a3, a4, a5, a6, a7, a8);
            } catch (Throwable t) {
                throw failure("glTexSubImage2D", t);
            }
        }

        static void glTexParameteri(int a0, int a1, int a2) {
            try {
                GLTEXPARAMETERI.invokeExact(a0, a1, a2);
            } catch (Throwable t) {
                throw failure("glTexParameteri", t);
            }
        }

        static void glTexParameteriv(int a0, int a1, MemorySegment a2) {
            try {
                GLTEXPARAMETERIV.invokeExact(a0, a1, a2);
            } catch (Throwable t) {
                throw failure("glTexParameteriv", t);
            }
        }

        static void glGenFramebuffers(int a0, MemorySegment a1) {
            try {
                GLGENFRAMEBUFFERS.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glGenFramebuffers", t);
            }
        }

        static void glDeleteFramebuffers(int a0, MemorySegment a1) {
            try {
                GLDELETEFRAMEBUFFERS.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glDeleteFramebuffers", t);
            }
        }

        static void glBindFramebuffer(int a0, int a1) {
            try {
                GLBINDFRAMEBUFFER.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glBindFramebuffer", t);
            }
        }

        static void glFramebufferTexture2D(int a0, int a1, int a2, int a3, int a4) {
            try {
                GLFRAMEBUFFERTEXTURE2D.invokeExact(a0, a1, a2, a3, a4);
            } catch (Throwable t) {
                throw failure("glFramebufferTexture2D", t);
            }
        }

        static void glFramebufferRenderbuffer(int a0, int a1, int a2, int a3) {
            try {
                GLFRAMEBUFFERRENDERBUFFER.invokeExact(a0, a1, a2, a3);
            } catch (Throwable t) {
                throw failure("glFramebufferRenderbuffer", t);
            }
        }

        static int glCheckFramebufferStatus(int a0) {
            try {
                return (int) GLCHECKFRAMEBUFFERSTATUS.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glCheckFramebufferStatus", t);
            }
        }

        static void glBlitFramebuffer(int a0, int a1, int a2, int a3, int a4, int a5, int a6, int a7, int a8, int a9) {
            try {
                GLBLITFRAMEBUFFER.invokeExact(a0, a1, a2, a3, a4, a5, a6, a7, a8, a9);
            } catch (Throwable t) {
                throw failure("glBlitFramebuffer", t);
            }
        }

        static void glReadPixels(int a0, int a1, int a2, int a3, int a4, int a5, MemorySegment a6) {
            try {
                GLREADPIXELS.invokeExact(a0, a1, a2, a3, a4, a5, a6);
            } catch (Throwable t) {
                throw failure("glReadPixels", t);
            }
        }

        static void glGenRenderbuffers(int a0, MemorySegment a1) {
            try {
                GLGENRENDERBUFFERS.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glGenRenderbuffers", t);
            }
        }

        static void glDeleteRenderbuffers(int a0, MemorySegment a1) {
            try {
                GLDELETERENDERBUFFERS.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glDeleteRenderbuffers", t);
            }
        }

        static void glBindRenderbuffer(int a0, int a1) {
            try {
                GLBINDRENDERBUFFER.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glBindRenderbuffer", t);
            }
        }

        static void glRenderbufferStorageMultisample(int a0, int a1, int a2, int a3, int a4) {
            try {
                GLRENDERBUFFERSTORAGEMULTISAMPLE.invokeExact(a0, a1, a2, a3, a4);
            } catch (Throwable t) {
                throw failure("glRenderbufferStorageMultisample", t);
            }
        }

        static void glGenSamplers(int a0, MemorySegment a1) {
            try {
                GLGENSAMPLERS.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glGenSamplers", t);
            }
        }

        static void glDeleteSamplers(int a0, MemorySegment a1) {
            try {
                GLDELETESAMPLERS.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glDeleteSamplers", t);
            }
        }

        static void glSamplerParameteri(int a0, int a1, int a2) {
            try {
                GLSAMPLERPARAMETERI.invokeExact(a0, a1, a2);
            } catch (Throwable t) {
                throw failure("glSamplerParameteri", t);
            }
        }

        static void glBindSampler(int a0, int a1) {
            try {
                GLBINDSAMPLER.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glBindSampler", t);
            }
        }

        static int glCreateShader(int a0) {
            try {
                return (int) GLCREATESHADER.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glCreateShader", t);
            }
        }

        static void glShaderSource(int a0, int a1, MemorySegment a2, MemorySegment a3) {
            try {
                GLSHADERSOURCE.invokeExact(a0, a1, a2, a3);
            } catch (Throwable t) {
                throw failure("glShaderSource", t);
            }
        }

        static void glCompileShader(int a0) {
            try {
                GLCOMPILESHADER.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glCompileShader", t);
            }
        }

        static void glGetShaderiv(int a0, int a1, MemorySegment a2) {
            try {
                GLGETSHADERIV.invokeExact(a0, a1, a2);
            } catch (Throwable t) {
                throw failure("glGetShaderiv", t);
            }
        }

        static void glGetShaderInfoLog(int a0, int a1, MemorySegment a2, MemorySegment a3) {
            try {
                GLGETSHADERINFOLOG.invokeExact(a0, a1, a2, a3);
            } catch (Throwable t) {
                throw failure("glGetShaderInfoLog", t);
            }
        }

        static void glDeleteShader(int a0) {
            try {
                GLDELETESHADER.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glDeleteShader", t);
            }
        }

        static int glCreateProgram() {
            try {
                return (int) GLCREATEPROGRAM.invokeExact();
            } catch (Throwable t) {
                throw failure("glCreateProgram", t);
            }
        }

        static void glAttachShader(int a0, int a1) {
            try {
                GLATTACHSHADER.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glAttachShader", t);
            }
        }

        static void glLinkProgram(int a0) {
            try {
                GLLINKPROGRAM.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glLinkProgram", t);
            }
        }

        static void glGetProgramiv(int a0, int a1, MemorySegment a2) {
            try {
                GLGETPROGRAMIV.invokeExact(a0, a1, a2);
            } catch (Throwable t) {
                throw failure("glGetProgramiv", t);
            }
        }

        static void glGetProgramInfoLog(int a0, int a1, MemorySegment a2, MemorySegment a3) {
            try {
                GLGETPROGRAMINFOLOG.invokeExact(a0, a1, a2, a3);
            } catch (Throwable t) {
                throw failure("glGetProgramInfoLog", t);
            }
        }

        static void glUseProgram(int a0) {
            try {
                GLUSEPROGRAM.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glUseProgram", t);
            }
        }

        static void glDeleteProgram(int a0) {
            try {
                GLDELETEPROGRAM.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glDeleteProgram", t);
            }
        }

        static void glGenBuffers(int a0, MemorySegment a1) {
            try {
                GLGENBUFFERS.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glGenBuffers", t);
            }
        }

        static void glDeleteBuffers(int a0, MemorySegment a1) {
            try {
                GLDELETEBUFFERS.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glDeleteBuffers", t);
            }
        }

        static void glBindBuffer(int a0, int a1) {
            try {
                GLBINDBUFFER.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glBindBuffer", t);
            }
        }

        static void glBufferData(int a0, long a1, MemorySegment a2, int a3) {
            try {
                GLBUFFERDATA.invokeExact(a0, a1, a2, a3);
            } catch (Throwable t) {
                throw failure("glBufferData", t);
            }
        }

        static void glBufferSubData(int a0, long a1, long a2, MemorySegment a3) {
            try {
                GLBUFFERSUBDATA.invokeExact(a0, a1, a2, a3);
            } catch (Throwable t) {
                throw failure("glBufferSubData", t);
            }
        }

        static void glBindBufferBase(int a0, int a1, int a2) {
            try {
                GLBINDBUFFERBASE.invokeExact(a0, a1, a2);
            } catch (Throwable t) {
                throw failure("glBindBufferBase", t);
            }
        }

        static void glGenVertexArrays(int a0, MemorySegment a1) {
            try {
                GLGENVERTEXARRAYS.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glGenVertexArrays", t);
            }
        }

        static void glDeleteVertexArrays(int a0, MemorySegment a1) {
            try {
                GLDELETEVERTEXARRAYS.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glDeleteVertexArrays", t);
            }
        }

        static void glBindVertexArray(int a0) {
            try {
                GLBINDVERTEXARRAY.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glBindVertexArray", t);
            }
        }

        static void glEnableVertexAttribArray(int a0) {
            try {
                GLENABLEVERTEXATTRIBARRAY.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("glEnableVertexAttribArray", t);
            }
        }

        static void glVertexAttribPointer(int a0, int a1, int a2, byte a3, int a4, long a5) {
            try {
                GLVERTEXATTRIBPOINTER.invokeExact(a0, a1, a2, a3, a4, a5);
            } catch (Throwable t) {
                throw failure("glVertexAttribPointer", t);
            }
        }

        static void glDrawElements(int a0, int a1, int a2, long a3) {
            try {
                GLDRAWELEMENTS.invokeExact(a0, a1, a2, a3);
            } catch (Throwable t) {
                throw failure("glDrawElements", t);
            }
        }

        static void glEGLImageTargetTexture2DOES(int a0, MemorySegment a1) {
            try {
                GLEGLIMAGETARGETTEXTURE2DOES.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("glEGLImageTargetTexture2DOES", t);
            }
        }
    }
    /** The buffers of the render node, from libgbm. */
    static final class GBM {
        private GBM() {
        }

        private static final MethodHandle GBM_CREATE_DEVICE = gbm("gbm_create_device", FunctionDescriptor.of(ADDRESS, JAVA_INT), false);
        private static final MethodHandle GBM_DEVICE_DESTROY = gbm("gbm_device_destroy", FunctionDescriptor.ofVoid(ADDRESS), false);
        private static final MethodHandle GBM_BO_CREATE = gbm("gbm_bo_create", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT), false);
        private static final MethodHandle GBM_BO_DESTROY = gbm("gbm_bo_destroy", FunctionDescriptor.ofVoid(ADDRESS), false);
        private static final MethodHandle GBM_BO_GET_FD = gbm("gbm_bo_get_fd", FunctionDescriptor.of(JAVA_INT, ADDRESS), false);
        private static final MethodHandle GBM_BO_GET_STRIDE = gbm("gbm_bo_get_stride", FunctionDescriptor.of(JAVA_INT, ADDRESS), false);

        static MemorySegment gbm_create_device(int a0) {
            try {
                return (MemorySegment) GBM_CREATE_DEVICE.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("gbm_create_device", t);
            }
        }

        static void gbm_device_destroy(MemorySegment a0) {
            try {
                GBM_DEVICE_DESTROY.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("gbm_device_destroy", t);
            }
        }

        static MemorySegment gbm_bo_create(MemorySegment a0, int a1, int a2, int a3, int a4) {
            try {
                return (MemorySegment) GBM_BO_CREATE.invokeExact(a0, a1, a2, a3, a4);
            } catch (Throwable t) {
                throw failure("gbm_bo_create", t);
            }
        }

        static void gbm_bo_destroy(MemorySegment a0) {
            try {
                GBM_BO_DESTROY.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("gbm_bo_destroy", t);
            }
        }

        static int gbm_bo_get_fd(MemorySegment a0) {
            try {
                return (int) GBM_BO_GET_FD.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("gbm_bo_get_fd", t);
            }
        }

        static int gbm_bo_get_stride(MemorySegment a0) {
            try {
                return (int) GBM_BO_GET_STRIDE.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("gbm_bo_get_stride", t);
            }
        }
    }
    /** libc. */
    static final class LibC {
        private LibC() {
        }

        private static final MethodHandle OPEN = libc("open", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT), false);
        private static final MethodHandle CLOSE = libc("close", FunctionDescriptor.of(JAVA_INT, JAVA_INT), false);
        private static final MethodHandle IOCTL = libc("ioctl", FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_LONG, ADDRESS), false);

        static int open(MemorySegment a0, int a1) {
            try {
                return (int) OPEN.invokeExact(a0, a1);
            } catch (Throwable t) {
                throw failure("open", t);
            }
        }

        static int close(int a0) {
            try {
                return (int) CLOSE.invokeExact(a0);
            } catch (Throwable t) {
                throw failure("close", t);
            }
        }

        static int ioctl(int a0, long a1, MemorySegment a2) {
            try {
                return (int) IOCTL.invokeExact(a0, a1, a2);
            } catch (Throwable t) {
                throw failure("ioctl", t);
            }
        }
    }
}
