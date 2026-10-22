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
import com.sun.glass.ui.GlassRobot;
import com.sun.glass.ui.gtk4.wrappers.GBytes;
import com.sun.glass.ui.gtk4.wrappers.Gdk;
import com.sun.glass.ui.gtk4.wrappers.GdkTexture;
import com.sun.glass.ui.gtk4.wrappers.GdkTextureDownloader;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A robot that generates input through the compositor and captures the frames
 * that the windows of the application present.
 * <p>
 * Wayland clients cannot inject input, so the robot generates it only in a
 * test session, where GNOME Shell emulates the devices through libei. The
 * events reach the application as any other input. Positions are global, in
 * the logical coordinates of the compositor.
 */
final class Gtk4Robot extends GlassRobot {

    private static final int BTN_LEFT = 0x110;
    private static final int BTN_RIGHT = 0x111;
    private static final int BTN_MIDDLE = 0x112;
    private static final int BTN_SIDE = 0x113;
    private static final int BTN_EXTRA = 0x114;

    private Gtk4EiInput input;
    private double mouseX;
    private double mouseY;

    @Override
    public void create() {
        if (Gtk4TestSession.isEnabled()) {
            input();
        }
    }

    @Override
    public void destroy() {
    }

    private Gtk4EiInput input() {
        if (!Gtk4TestSession.isEnabled()) {
            throw new UnsupportedOperationException(
                    "The GTK 4 robot generates input only with -Dglass.gtk4.testSession=gnome");
        }
        if (input == null) {
            input = Gtk4EiInput.connect();
        }
        Gtk4TestSession.awaitPlacements();
        return input;
    }

    @Override
    public void keyPress(KeyCode keyCode) {
        Application.checkEventThread();
        key(keyCode, true);
    }

    @Override
    public void keyRelease(KeyCode keyCode) {
        Application.checkEventThread();
        key(keyCode, false);
    }

    private void key(KeyCode keyCode, boolean press) {
        int keycode = Gtk4KeyMap.toEvdevKeycode(keyCode.getCode());
        if (keycode <= 0) {
            throw new IllegalArgumentException("The robot cannot type " + keyCode);
        }
        input().key(keycode, press);
    }

    @Override
    public double getMouseX() {
        Application.checkEventThread();
        return mouseX;
    }

    @Override
    public double getMouseY() {
        Application.checkEventThread();
        return mouseY;
    }

    @Override
    public void mouseMove(double x, double y) {
        Application.checkEventThread();
        input().moveTo(x, y);
        mouseX = x;
        mouseY = y;
    }

    @Override
    public void mousePress(MouseButton... buttons) {
        Application.checkEventThread();
        for (MouseButton button : buttons) {
            input().button(evdevButton(button), true);
        }
    }

    @Override
    public void mouseRelease(MouseButton... buttons) {
        Application.checkEventThread();
        for (MouseButton button : buttons) {
            input().button(evdevButton(button), false);
        }
    }

    @Override
    public void mouseWheel(int wheelAmt) {
        Application.checkEventThread();
        input().scroll(wheelAmt);
    }

    private static int evdevButton(MouseButton button) {
        return switch (button) {
            case PRIMARY -> BTN_LEFT;
            case SECONDARY -> BTN_RIGHT;
            case MIDDLE -> BTN_MIDDLE;
            case BACK -> BTN_SIDE;
            case FORWARD -> BTN_EXTRA;
            default -> throw new IllegalArgumentException("The robot cannot press " + button);
        };
    }

    // ---------------------------------------------------------------------
    // Screen capture

    @Override
    public Color getPixelColor(double x, double y) {
        Application.checkEventThread();
        int[] data = new int[1];
        double scale = scale();
        getScreenCapture((int) Math.floor(x * scale), (int) Math.floor(y * scale), 1, 1, data, false);
        return convertFromIntArgb(data[0]);
    }

    @Override
    public void getScreenCapture(int x, int y, int width, int height, int[] data, boolean scaleToFit) {
        Application.checkEventThread();
        double scale = scale();
        Map<Gtk4Window, Frame> frames = new HashMap<>();
        List<Gtk4Window> stackingOrder = Gtk4Window.getStackingOrder();
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {
                // Physical pixel to logical position
                int lx = (int) Math.floor((x + i) / scale);
                int ly = (int) Math.floor((y + j) / scale);
                Gtk4Window window = Gtk4Window.getWindowAt(stackingOrder, lx, ly);
                int argb = 0;
                if (window != null) {
                    Frame frame = frames.computeIfAbsent(window, Gtk4Robot::capture);
                    if (frame != null) {
                        double fx = (x + i - window.getScreenX() * scale) * frame.width / (window.getViewWidth() * scale);
                        double fy = (y + j - window.getScreenY() * scale) * frame.height / (window.getViewHeight() * scale);
                        argb = frame.get((int) fx, (int) fy);
                    }
                }
                data[j * width + i] = argb;
            }
        }
    }

    private double scale() {
        Gtk4Window window = Gtk4Window.getActiveWindow();
        return window != null && window.getOutputScaleX() > 0 ? window.getOutputScaleX() : 1.0;
    }

    private record Frame(int width, int height, int stride, ByteBuffer pixels) {
        int get(int x, int y) {
            if (x < 0 || y < 0 || x >= width || y >= height) {
                return 0;
            }
            return pixels.getInt(y * stride + x * 4);
        }
    }

    /**
     * Downloads the frame a window presents as BGRA, which read as
     * little-endian ints is ARGB.
     */
    private static Frame capture(Gtk4Window window) {
        Gtk4Compositor compositor = window.getCompositor();
        GdkTexture texture = compositor != null ? compositor.getCurrentTexture() : null;
        if (texture == null || texture.isNull()) {
            return null;
        }
        GdkTextureDownloader downloader = GdkTextureDownloader.create(texture);
        downloader.setFormat(Gdk.MEMORY_B8G8R8A8);
        long[] stride = new long[1];
        GBytes bytes = downloader.downloadBytes(stride);
        downloader.free();
        byte[] data = bytes.getData();
        bytes.unref();
        return new Frame(texture.getWidth(), texture.getHeight(), (int) stride[0],
                ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
    }
}
