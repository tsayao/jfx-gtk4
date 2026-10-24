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

import com.sun.glass.ui.Screen;
import com.sun.glass.ui.gtk4.wrappers.GMainContext;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A robot that generates input through the compositor and captures the frames
 * that the windows of the application present.
 * <p>
 * Wayland clients cannot inject input: the robot emulates the devices
 * through libei, in a remote desktop session of the compositor. In a test
 * session (-Dglass.gtk4.testSession=gnome) it is the session of GNOME Shell,
 * which also places the windows; elsewhere it is the session of the
 * RemoteDesktop portal, which the desktop asks the user to allow once. The
 * events reach the application as any other input. Positions are global, in
 * the logical coordinates of the compositor.
 * <p>
 * Outside a test session the compositor does not tell where the windows are.
 * The robot learns it from its pointer: when the pointer it moved to a screen
 * point is at a point of a view, the window is where both points meet. The
 * first move to a window whose position is not known yet looks for the
 * window over the screens, and is then made to the intended point of it.
 */
final class Gtk4Robot extends GlassRobot {

    private static final int BTN_LEFT = 0x110;
    private static final int BTN_RIGHT = 0x111;
    private static final int BTN_MIDDLE = 0x112;
    private static final int BTN_SIDE = 0x113;
    private static final int BTN_EXTRA = 0x114;

    // How long a view may take to report the pointer of a move of the robot
    private static final long POINTER_REPORT_NANOS = 500_000_000L;
    private static final long LOCATE_WAIT_NANOS = 60_000_000L;
    private static final int MAX_PROBES = 600;

    private Gtk4EiInput input;
    private double mouseX;
    private double mouseY;

    // The last point the robot moved the pointer to, until a view reports it
    private static double pendingX;
    private static double pendingY;
    private static long pendingTime;
    private static boolean pending;

    @Override
    public void create() {
        if (Gtk4TestSession.isEnabled()) {
            input();
        }
    }

    /**
     * Called by the views with the position of the pointer in them: right
     * after a move of the robot, it gives the screen position of the window.
     */
    static void pointerAt(Gtk4Window window, double x, double y) {
        if (!pending || window == null || Gtk4TestSession.isEnabled() || !window.isShowingToplevel()) {
            return;
        }
        pending = false;
        if (System.nanoTime() - pendingTime > POINTER_REPORT_NANOS) {
            // Too late to be the move of the robot: the user may have moved the pointer
            return;
        }
        window.setViewScreenPosition((int) Math.round(pendingX - x), (int) Math.round(pendingY - y));
    }

    @Override
    public void destroy() {
    }

    // One remote desktop session for the robots and the touch robot
    private static Gtk4EiInput sharedInput;
    // Why the robot has no input, which asking again would not change
    private static UnsupportedOperationException unavailable;

    static Gtk4EiInput sharedInput() {
        if (unavailable != null) {
            throw new UnsupportedOperationException(unavailable.getMessage(), unavailable);
        }
        if (sharedInput == null) {
            try {
                sharedInput = Gtk4EiInput.connect();
            } catch (UnsupportedOperationException e) {
                // No libei or no portal, or the user refused: do not ask on every move
                unavailable = e;
                throw e;
            }
        }
        if (Gtk4TestSession.isEnabled()) {
            Gtk4TestSession.awaitPlacements();
        }
        return sharedInput;
    }

    private Gtk4EiInput input() {
        if (input == null) {
            input = sharedInput();
        }
        if (Gtk4TestSession.isEnabled()) {
            Gtk4TestSession.awaitPlacements();
        }
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
        Gtk4EiInput in = input();
        double tx = x;
        double ty = y;
        List<Gtk4Window> hits = Gtk4Window.getToplevelsAt((int) Math.floor(x), (int) Math.floor(y));
        if (!hits.isEmpty()) {
            // The point was computed with the position the window had: one
            // assumed off the test session, or one the user has since changed
            // by moving the window in it
            Gtk4Window w = hits.getFirst();
            int assumedX = w.getScreenX();
            int assumedY = w.getScreenY();
            boolean moved = Gtk4TestSession.isEnabled()
                    ? w.refreshTestPosition()
                    : !w.isPositionKnown() && locate(in, w);
            if (moved) {
                tx = x - assumedX + w.getScreenX();
                ty = y - assumedY + w.getScreenY();
            }
        }
        moveTo(in, tx, ty);
        mouseX = tx;
        mouseY = ty;
    }

    private static void moveTo(Gtk4EiInput in, double x, double y) {
        pendingX = x;
        pendingY = y;
        pendingTime = System.nanoTime();
        pending = true;
        in.moveTo(x, y);
    }

    /**
     * Moves the pointer over the screens, from their centers outwards, until
     * the window reports it.
     */
    private static boolean locate(Gtk4EiInput in, Gtk4Window w) {
        int step = Math.max(24, Math.min(w.getViewWidth(), w.getViewHeight()) / 2);
        List<double[]> probes = new ArrayList<>();
        for (Screen screen : Screen.getScreens()) {
            double cx = screen.getX() + screen.getWidth() / 2.0;
            double cy = screen.getY() + screen.getHeight() / 2.0;
            List<double[]> points = new ArrayList<>();
            for (int y = screen.getY() + step / 2; y < screen.getY() + screen.getHeight(); y += step) {
                for (int x = screen.getX() + step / 2; x < screen.getX() + screen.getWidth(); x += step) {
                    points.add(new double[] {x, y});
                }
            }
            points.sort(Comparator.comparingDouble(p -> Math.hypot(p[0] - cx, p[1] - cy)));
            probes.addAll(points);
        }
        GMainContext context = GMainContext.default_();
        int count = 0;
        for (double[] p : probes) {
            if (++count > MAX_PROBES) {
                break;
            }
            moveTo(in, p[0], p[1]);
            long deadline = System.nanoTime() + LOCATE_WAIT_NANOS;
            while (pending && System.nanoTime() < deadline) {
                if (!context.iteration(false)) {
                    Thread.onSpinWait();
                }
            }
            if (w.isPositionKnown()) {
                Gtk4Log.log(Gtk4Log.Category.SIZING, "robot: found the window after %d moves", count);
                return true;
            }
        }
        Gtk4Log.log(Gtk4Log.Category.SIZING, "robot: the window was not found");
        return false;
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
