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

import com.sun.glass.ui.gtk4.wrappers.Ei;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.TimeUnit;

final class Gtk4EiInput {

    private static final Gtk4Log.Category LOG = Gtk4Log.Category.FOCUS;

    private static final long CONNECT_TIMEOUT = TimeUnit.SECONDS.toNanos(5);
    private static final long SYNC_TIMEOUT = TimeUnit.SECONDS.toNanos(2);
    private static final int SCROLL_DETENT = 120;
    private static final long POINTER_FOCUS_DELAY = 40;

    private final MemorySegment ei;
    private MemorySegment absolute;
    private MemorySegment buttons;
    private MemorySegment scroll;
    private MemorySegment keyboard;
    private int sequence;
    private boolean disconnected;
    private MemorySegment pendingPing;

    private Gtk4EiInput(MemorySegment ei) {
        this.ei = ei;
    }

    static Gtk4EiInput connect() {
        if (!Ei.isAvailable()) {
            throw new IllegalStateException("The robot requires libei");
        }
        int fd = Gtk4TestSession.connectToEis();
        MemorySegment ei = Ei.newSender();
        Ei.configureName(ei, "JavaFX Robot");
        int status = Ei.setupBackendFd(ei, fd);
        if (status != 0) {
            Ei.unref(ei);
            throw new IllegalStateException("ei_setup_backend_fd failed: " + status);
        }
        Gtk4EiInput input = new Gtk4EiInput(ei);
        input.awaitDevices();
        return input;
    }

    void moveTo(double x, double y) {
        dispatch();
        MemorySegment device = require(absolute, "absolute pointer");
        Ei.devicePointerMotionAbsolute(device, x, y);
        Ei.deviceFrame(device, Ei.now(ei));
        sync();
        awaitPointerFocus();
    }

    void button(int evdevButton, boolean press) {
        dispatch();
        MemorySegment device = require(buttons, "button");
        Ei.deviceButtonButton(device, evdevButton, press);
        Ei.deviceFrame(device, Ei.now(ei));
        sync();
    }

    void scroll(int detents) {
        dispatch();
        MemorySegment device = require(scroll, "scroll");
        Ei.deviceScrollDiscrete(device, 0, detents * SCROLL_DETENT);
        Ei.deviceFrame(device, Ei.now(ei));
        sync();
    }

    void key(int evdevKeycode, boolean press) {
        dispatch();
        MemorySegment device = require(keyboard, "keyboard");
        Ei.deviceKeyboardKey(device, evdevKeycode, press);
        Ei.deviceFrame(device, Ei.now(ei));
        sync();
    }

    /**
     * Waits for the compositor to find the surface under the pointer, which
     * it does when it draws its next frame, so that the events that follow
     * the motion go to that surface.
     */
    private static void awaitPointerFocus() {
        try {
            Thread.sleep(POINTER_FOCUS_DELAY);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static MemorySegment require(MemorySegment device, String name) {
        if (device == null) {
            throw new IllegalStateException("The robot " + name + " device is not available");
        }
        return device;
    }

    private void sync() {
        MemorySegment ping = Ei.newPing(ei);
        pendingPing = ping;
        try {
            Ei.ping(ping);
            long deadline = System.nanoTime() + SYNC_TIMEOUT;
            while (pendingPing != null) {
                dispatch();
                if (pendingPing == null) {
                    break;
                }
                if (System.nanoTime() > deadline) {
                    throw new IllegalStateException("The compositor did not process the robot input");
                }
                Thread.onSpinWait();
            }
        } finally {
            pendingPing = null;
            Ei.pingUnref(ping);
        }
    }

    private void awaitDevices() {
        long deadline = System.nanoTime() + CONNECT_TIMEOUT;
        while (absolute == null || buttons == null || scroll == null || keyboard == null) {
            dispatch();
            if (System.nanoTime() > deadline) {
                throw new IllegalStateException("The compositor did not provide the robot devices:"
                        + " absolute=" + (absolute != null) + " buttons=" + (buttons != null)
                        + " scroll=" + (scroll != null) + " keyboard=" + (keyboard != null));
            }
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
        for (long i = 0; ; i++) {
            MemorySegment region = Ei.deviceGetRegion(absolute, i);
            if (region.equals(MemorySegment.NULL)) {
                break;
            }
            Gtk4Log.log(LOG, "robot region %d,%d %dx%d", Ei.regionGetX(region), Ei.regionGetY(region),
                    Ei.regionGetWidth(region), Ei.regionGetHeight(region));
        }
        Gtk4Log.log(LOG, "robot devices ready");
    }

    private void dispatch() {
        if (disconnected) {
            throw new IllegalStateException("The compositor disconnected the robot");
        }
        Ei.dispatch(ei);
        for (MemorySegment event = Ei.getEvent(ei); !event.equals(MemorySegment.NULL); event = Ei.getEvent(ei)) {
            try {
                handle(event);
            } finally {
                Ei.eventUnref(event);
            }
        }
    }

    private void handle(MemorySegment event) {
        int type = Ei.eventGetType(event);
        switch (type) {
            case Ei.EVENT_SEAT_ADDED -> Ei.seatBindCapabilities(Ei.eventGetSeat(event),
                    Ei.DEVICE_CAP_POINTER_ABSOLUTE, Ei.DEVICE_CAP_BUTTON,
                    Ei.DEVICE_CAP_SCROLL, Ei.DEVICE_CAP_KEYBOARD);
            case Ei.EVENT_DEVICE_RESUMED -> resumed(Ei.eventGetDevice(event));
            case Ei.EVENT_DEVICE_PAUSED, Ei.EVENT_DEVICE_REMOVED -> removed(Ei.eventGetDevice(event));
            case Ei.EVENT_DISCONNECT -> disconnected = true;
            case Ei.EVENT_PONG -> {
                if (Ei.eventPongGetPing(event).equals(pendingPing)) {
                    pendingPing = null;
                }
            }
            default -> { }
        }
        Gtk4Log.log(LOG, "ei event %d", type);
    }

    private void resumed(MemorySegment device) {
        boolean used = false;
        if (absolute == null && Ei.deviceHasCapability(device, Ei.DEVICE_CAP_POINTER_ABSOLUTE)) {
            absolute = device;
            used = true;
        }
        if (buttons == null && Ei.deviceHasCapability(device, Ei.DEVICE_CAP_BUTTON)) {
            buttons = device;
            used = true;
        }
        if (scroll == null && Ei.deviceHasCapability(device, Ei.DEVICE_CAP_SCROLL)) {
            scroll = device;
            used = true;
        }
        if (keyboard == null && Ei.deviceHasCapability(device, Ei.DEVICE_CAP_KEYBOARD)) {
            keyboard = device;
            used = true;
        }
        if (used) {
            Ei.deviceRef(device);
            Ei.deviceStartEmulating(device, ++sequence);
        }
    }

    private void removed(MemorySegment device) {
        boolean used = false;
        if (device.equals(absolute)) {
            absolute = null;
            used = true;
        }
        if (device.equals(buttons)) {
            buttons = null;
            used = true;
        }
        if (device.equals(scroll)) {
            scroll = null;
            used = true;
        }
        if (device.equals(keyboard)) {
            keyboard = null;
            used = true;
        }
        if (used) {
            Ei.deviceUnref(device);
        }
    }
}
