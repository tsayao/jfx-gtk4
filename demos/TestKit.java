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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.robot.Robot;
import javafx.util.Duration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The tests of the labs: each lab declares a suite whose tests show OK, FAIL
 * or SKIP, run one at a time on a background thread, and drive the lab with
 * javafx.scene.robot.Robot when they need input. The tests launcher
 * (TestsDemo) runs the suites of every lab.
 * <p>
 * There are three kinds of tests:
 * <ul>
 * <li>automatic tests, which the lab can check alone;</li>
 * <li>robot tests, which need robot input. In the GNOME test session
 *     (-Dglass.gtk4.testSession=gnome, which the launcher passes under
 *     --compositor=gnome) the robot uses its remote desktop session; on any
 *     other desktop it moves the pointer and types on the desktop of the user,
 *     through the RemoteDesktop portal, which asks for the permission once.
 *     They are skipped with -Dtests.robot=false, or when the desktop has no
 *     robot input (no libei or portal, or the permission was refused);</li>
 * <li>manual tests, which need a device or a person, such as a touchpad:
 *     they pass when the lab sees the events they wait for, or with their OK
 *     and FAIL buttons, and are skipped by a run until then;</li>
 * <li>touch tests, which pass as manual tests, and which a run performs with
 *     the touch screen that the compositor offers to the robot through
 *     libei, when the robot runs and libei and the compositor support touch
 *     screens (checked at run time).</li>
 * </ul>
 * With -Dtests.run=true the tests run at startup, and with -Dtests.exit=true
 * the application then exits, with status 1 when a test failed.
 */
final class TestKit {

    enum Status {
        IDLE("IDLE", "idle"),
        QUEUED("QUEUED", "idle"),
        RUNNING("RUNNING", "running"),
        OK("OK", "ok"),
        FAIL("FAIL", "fail"),
        SKIP("SKIP", "skip");

        final String text;
        final String style;

        Status(String text, String style) {
            this.text = text;
            this.style = style;
        }
    }

    enum Kind {
        AUTO, ROBOT, MANUAL, TOUCH
    }

    @FunctionalInterface
    interface Step {
        /**
         * Runs the test on the test thread.
         *
         * @return what the test saw, shown next to OK
         */
        String run() throws Exception;
    }

    /** Thrown in a test when the run is stopped. */
    static final class Stopped extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Stopped() {
            super("Stopped", null, false, false);
        }
    }

    /** Thrown by a test that cannot run here, which is then skipped. */
    static final class Skipped extends RuntimeException {
        private static final long serialVersionUID = 1L;

        Skipped(String message) {
            super(message, null, false, false);
        }
    }

    record Result(int ok, int fail, int skip) {
        static final Result NONE = new Result(0, 0, 0);

        Result plus(Result r) {
            return new Result(ok + r.ok, fail + r.fail, skip + r.skip);
        }

        @Override
        public String toString() {
            return ok + " OK · " + fail + " FAIL · " + skip + " SKIP";
        }
    }

    static final long TIMEOUT_MS = 3000;

    private static final String CSS = """
        .test-status {
            -fx-background-radius: 999;
            -fx-padding: 1 9 1 9;
            -fx-font-size: 10px;
            -fx-font-weight: bold;
            -fx-min-width: 58;
            -fx-alignment: center;
        }
        .test-status.idle { -fx-background-color: rgba(255, 255, 255, 0.07); -fx-text-fill: #93a1b8; }
        .test-status.running { -fx-background-color: rgba(99, 102, 241, 0.35); -fx-text-fill: #c7d2fe; }
        .test-status.ok { -fx-background-color: rgba(34, 197, 94, 0.25); -fx-text-fill: #86efac; }
        .test-status.fail { -fx-background-color: rgba(244, 63, 94, 0.28); -fx-text-fill: #fda4af; }
        .test-status.skip { -fx-background-color: rgba(245, 158, 11, 0.2); -fx-text-fill: #fcd34d; }
        .test-name { -fx-font-weight: bold; }
        .test-kind {
            -fx-font-size: 10px;
            -fx-text-fill: #93a1b8;
            -fx-border-color: rgba(255, 255, 255, 0.18);
            -fx-border-radius: 6;
            -fx-padding: 0 5 0 5;
        }
        .test-detail { -fx-font-size: 11px; -fx-text-fill: #93a1b8; -fx-padding: 0 0 4 0; }
        .test-list, .test-list > .viewport { -fx-background-color: transparent; }
        .test-list { -fx-padding: 0; }
        .test-summary { -fx-font-size: 16px; -fx-font-weight: bold; }
        .test-summary.ok { -fx-text-fill: #86efac; }
        .test-summary.fail { -fx-text-fill: #fda4af; }
        .button.test-run {
            -fx-background-color: -fx-accent;
            -fx-border-color: transparent;
            -fx-text-fill: white;
            -fx-font-weight: bold;
        }
        .button.test-stop {
            -fx-background-color: #e11d48;
            -fx-border-color: transparent;
            -fx-text-fill: white;
            -fx-font-weight: bold;
        }
        .button.test-small { -fx-padding: 2 10 2 10; -fx-font-size: 11px; }
        .button:disabled { -fx-opacity: 0.4; }
        """;

    static final String STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));

    // One run at a time, as the robot tests share the pointer and the keyboard
    private static final AtomicBoolean LOCK = new AtomicBoolean();
    private static final BooleanProperty BUSY = new SimpleBooleanProperty();
    private static Robot robot;

    private TestKit() {
    }

    // ---------------------------------------------------------------------
    // Tests and suites

    static final class Test {
        final Suite suite;
        final String name;
        final String description;
        final Kind kind;
        final Step body;
        final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.IDLE);
        final StringProperty detail = new SimpleStringProperty();

        private Test(Suite suite, String name, String description, Kind kind, Step body) {
            this.suite = suite;
            this.name = name;
            this.description = description;
            this.kind = kind;
            this.body = body;
            detail.set(description);
        }

        /**
         * Passes a manual test, on the FX thread.
         */
        void pass(String detail) {
            if (status.get() != Status.OK) {
                set(Status.OK, detail);
                report(this);
            }
        }

        /**
         * Fails a manual test, on the FX thread.
         */
        void fail(String detail) {
            set(Status.FAIL, detail);
            report(this);
        }

        private void set(Status s, String d) {
            status.set(s);
            detail.set(d);
        }
    }

    static final class Suite {
        final String name;
        final List<Test> tests = new ArrayList<>();
        final StringProperty summary = new SimpleStringProperty("Not run");
        private final Label summaryLabel = new Label();
        private Runnable beforeRun = () -> { };
        private Runnable afterRobotFailure = () -> { };
        private Runnable afterEach = () -> { };

        Suite(String name) {
            this.name = name;
            darkDialogs();
        }

        Test auto(String name, String description, Step body) {
            return add(new Test(this, name, description, Kind.AUTO, body));
        }

        Test robot(String name, String description, Step body) {
            return add(new Test(this, name, description, Kind.ROBOT, body));
        }

        Test manual(String name, String description) {
            return add(new Test(this, name, description, Kind.MANUAL, null));
        }

        Test touch(String name, String description, Step body) {
            return add(new Test(this, name, description, Kind.TOUCH, body));
        }

        private Test add(Test t) {
            tests.add(t);
            return t;
        }

        /**
         * Sets what to do on the FX thread before a run, such as raising the
         * window so that the robot input reaches it.
         */
        void setBeforeRun(Runnable r) {
            beforeRun = r;
        }

        /**
         * Sets what to do on the FX thread after each test that ran, such as
         * closing the windows it opened.
         */
        void setAfterEach(Runnable r) {
            afterEach = r;
        }

        /**
         * Sets what to do on the FX thread after a robot test failed, such as
         * restoring the options it changed.
         */
        void setAfterRobotFailure(Runnable r) {
            afterRobotFailure = r;
        }

        Result result() {
            int ok = 0, fail = 0, skip = 0;
            for (Test t : tests) {
                switch (t.status.get()) {
                    case OK -> ok++;
                    case FAIL -> fail++;
                    case SKIP -> skip++;
                    default -> { }
                }
            }
            return new Result(ok, fail, skip);
        }

        /**
         * The table of the tests, with a button that runs them all and a
         * button per test.
         */
        Node view() {
            Button runAll = new Button("Run all tests");
            runAll.getStyleClass().add("test-run");
            runAll.disableProperty().bind(BUSY);
            runAll.setOnAction(e -> runAsync(tests));
            Button stop = stopButton(new Button("Stop"));
            summaryLabel.getStyleClass().add("test-summary");
            summaryLabel.textProperty().bind(summary);
            HBox top = new HBox(12, runAll, stop, summaryLabel);
            top.setAlignment(Pos.CENTER_LEFT);

            GridPane grid = new GridPane(10, 6);
            int row = 0;
            for (Test t : tests) {
                Node actions;
                if (t.kind == Kind.MANUAL || t.kind == Kind.TOUCH) {
                    Button ok = small("OK");
                    ok.setOnAction(e -> t.pass("Marked OK by hand"));
                    Button fail = small("FAIL");
                    fail.setOnAction(e -> t.fail("Marked FAIL by hand"));
                    actions = new HBox(4, ok, fail);
                    if (t.kind == Kind.TOUCH) {
                        ((HBox) actions).getChildren().addFirst(runOrStop(t));
                    }
                } else {
                    actions = runOrStop(t);
                }
                // The detail goes below the name, so that the table fits narrow columns
                Label detail = new Label();
                detail.textProperty().bind(t.detail);
                detail.getStyleClass().add("test-detail");
                detail.setWrapText(true);
                detail.setMinWidth(0);
                detail.setMaxWidth(Double.MAX_VALUE);
                Node name = nameLabel(t);
                GridPane.setHgrow(name, Priority.ALWAYS);
                grid.addRow(row++, statusPill(t), name, actions);
                grid.add(detail, 1, row++, 2, 1);
                followRunning(t, name, detail);
            }
            Label hint = new Label("Do not use the mouse or the keyboard while the robot tests run. Manual tests "
                    + "pass when the lab sees their events, or with their buttons.");
            hint.getStyleClass().add("test-detail");
            hint.setWrapText(true);
            // The table scrolls in its card, which grows with the window
            ScrollPane list = new ScrollPane(grid);
            list.getStyleClass().add("test-list");
            list.setFitToWidth(true);
            list.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            list.setPrefHeight(240);
            list.setMinHeight(0);
            VBox.setVgrow(list, Priority.ALWAYS);
            VBox box = new VBox(10, top, list, hint);
            VBox.setVgrow(box, Priority.ALWAYS);
            box.getStylesheets().add(STYLESHEET);
            return box;
        }

        /**
         * The Run button of a test, which stops the run while the test runs.
         */
        private Button runOrStop(Test t) {
            Button b = small("Run");
            javafx.beans.binding.BooleanBinding running = t.status.isEqualTo(Status.RUNNING);
            b.textProperty().bind(javafx.beans.binding.Bindings.when(running).then("Stop").otherwise("Run"));
            b.disableProperty().bind(BUSY.and(running.not()).or(STOPPING.and(running)));
            b.setOnAction(e -> {
                if (running.get()) {
                    stop();
                } else {
                    runAsync(List.of(t));
                }
            });
            return b;
        }

        /**
         * The tests with their status only, for an overview of the suites.
         */
        Node compactView() {
            GridPane grid = new GridPane(10, 5);
            int row = 0;
            for (Test t : tests) {
                Label detail = new Label();
                detail.textProperty().bind(t.detail);
                detail.getStyleClass().add("test-detail");
                detail.setMinWidth(0);
                detail.setWrapText(true);
                detail.setMaxHeight(48);
                GridPane.setHgrow(detail, Priority.ALWAYS);
                Node name = nameLabel(t);
                grid.addRow(row++, statusPill(t), name, detail);
                followRunning(t, name, detail);
            }
            grid.getStylesheets().add(STYLESHEET);
            return grid;
        }

        Label summaryLabel() {
            Label label = new Label();
            label.textProperty().bind(summary);
            label.getStyleClass().add("test-summary");
            summary.addListener((obs, o, n) -> style(label));
            label.getStylesheets().add(STYLESHEET);
            return label;
        }

        /**
         * Runs the tests on a new thread, unless another run is going on.
         */
        void runAsync(List<Test> selected) {
            if (!tryLock()) {
                return;
            }
            Thread.ofPlatform().daemon().name("tests-" + name).start(() -> {
                try {
                    run(selected);
                } finally {
                    unlock();
                }
            });
        }

        /**
         * Runs the tests on the calling thread, which is not the FX thread.
         */
        Result run(List<Test> selected) {
            fx(() -> {
                for (Test t : selected) {
                    if ((t.kind != Kind.MANUAL && t.kind != Kind.TOUCH) || !isVerdict(t.status.get())) {
                        t.set(Status.QUEUED, t.description);
                    }
                }
                summary.set("Running…");
                style(summaryLabel);
                beforeRun.run();
            });
            long start = System.nanoTime();
            try {
                pause(300);
                for (Test t : selected) {
                    runOne(t);
                }
            } catch (Stopped e) {
                // Between two tests: the next ones do not run
            }
            boolean stopped = isStopping();
            if (stopped) {
                fx(() -> {
                    for (Test t : selected) {
                        if (t.status.get() == Status.QUEUED || t.status.get() == Status.RUNNING) {
                            t.set(Status.IDLE, "Stopped: " + t.description);
                        }
                    }
                });
            }
            long ms = (System.nanoTime() - start) / 1_000_000;
            Result result = fx(() -> result());
            String text = (stopped ? "Stopped: " : "") + result + " in " + ms + " ms";
            System.out.println("[" + name + "] " + text);
            fx(() -> {
                summary.set(text);
                style(summaryLabel);
            });
            return result;
        }

        private void runOne(Test t) {
            if (t.kind == Kind.MANUAL) {
                fx(() -> {
                    if (!isVerdict(t.status.get())) {
                        t.set(Status.SKIP, "Not seen yet: " + t.description);
                    }
                });
                report(t);
                return;
            }
            if (t.kind == Kind.TOUCH && (!robotInput() || !touchAvailable())) {
                fx(() -> {
                    if (!isVerdict(t.status.get())) {
                        t.set(Status.SKIP, robotInput()
                                ? "No touch screen for the robot (libei or the compositor): touch it by hand"
                                : "Not seen yet: " + t.description);
                    }
                });
                report(t);
                return;
            }
            if ((t.kind == Kind.ROBOT || t.kind == Kind.TOUCH) && noRobotInput != null) {
                fx(() -> t.set(Status.SKIP, "No robot input: " + noRobotInput));
                report(t);
                return;
            }
            if (t.kind == Kind.ROBOT && !robotInput()) {
                fx(() -> t.set(Status.SKIP, "Robot tests turned off with -Dtests.robot=false"));
                report(t);
                return;
            }
            fx(() -> t.set(Status.RUNNING, t.description));
            // The robot reads the frames on screen: let them catch up with the layout
            pause(300);
            long t0 = System.nanoTime();
            Status status;
            String detail;
            try {
                detail = t.body.run();
                status = Status.OK;
            } catch (Stopped e) {
                // Leaves nothing pressed, nor windows of the test
                releaseInput();
                fx(afterRobotFailure);
                fx(afterEach);
                fx(() -> t.set(Status.IDLE, "Stopped (" + (System.nanoTime() - t0) / 1_000_000 + " ms)"));
                report(t);
                throw e;
            } catch (Skipped e) {
                detail = e.getMessage();
                status = Status.SKIP;
            } catch (AssertionError e) {
                detail = e.getMessage();
                status = Status.FAIL;
            } catch (UnsupportedOperationException e) {
                // The desktop has no robot input: the next robot tests are skipped
                if (t.kind == Kind.ROBOT || t.kind == Kind.TOUCH) {
                    noRobotInput = e.getMessage();
                    detail = "No robot input: " + e.getMessage();
                    status = Status.SKIP;
                } else {
                    detail = e.toString();
                    status = Status.FAIL;
                }
            } catch (Throwable e) {
                detail = e.toString();
                status = Status.FAIL;
            }
            if (status == Status.FAIL && (t.kind == Kind.ROBOT || t.kind == Kind.TOUCH)) {
                releaseInput();
                fx(afterRobotFailure);
            }
            fx(afterEach);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            String shown = (detail == null ? "" : detail) + " (" + ms + " ms)";
            Status s = status;
            fx(() -> t.set(s, shown));
            report(t);
            pause(150);
        }

        private void style(Label label) {
            label.getStyleClass().removeAll("ok", "fail");
            Result r = result();
            if (r.fail > 0) {
                label.getStyleClass().add("fail");
            } else if (r.ok > 0 && !summary.get().startsWith("Running")) {
                label.getStyleClass().add("ok");
            }
        }
    }

    private static boolean isVerdict(Status s) {
        return s == Status.OK || s == Status.FAIL;
    }

    private static void report(Test t) {
        Status s = t.status.get();
        System.out.println(String.format(Locale.ROOT, "%-4s %s / %s: %s", s.text, t.suite.name, t.name,
                t.detail.get()));
    }

    private static Label statusPill(Test t) {
        Label pill = new Label();
        pill.getStyleClass().add("test-status");
        pill.setMinWidth(Region.USE_PREF_SIZE);
        Consumer<Status> update = s -> {
            pill.setText(s.text);
            pill.getStyleClass().removeAll("idle", "running", "ok", "fail", "skip");
            pill.getStyleClass().add(s.style);
        };
        update.accept(t.status.get());
        t.status.addListener((obs, o, n) -> update.accept(n));
        return pill;
    }

    private static Node nameLabel(Test t) {
        Label name = new Label(t.name);
        name.getStyleClass().add("test-name");
        name.setMinWidth(Region.USE_PREF_SIZE);
        if (t.kind == Kind.AUTO) {
            return name;
        }
        Label kind = new Label(switch (t.kind) {
            case ROBOT -> "robot";
            case TOUCH -> "touch";
            default -> "manual";
        });
        kind.getStyleClass().add("test-kind");
        kind.setMinWidth(Region.USE_PREF_SIZE);
        HBox box = new HBox(6, name, kind);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static Button small(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("test-small");
        b.setMinWidth(Region.USE_PREF_SIZE);
        return b;
    }

    // ---------------------------------------------------------------------
    // Runs

    static boolean tryLock() {
        if (!LOCK.compareAndSet(false, true)) {
            return false;
        }
        stopRequested = false;
        fx(() -> {
            STOPPING.set(false);
            BUSY.set(true);
        });
        return true;
    }

    static void unlock() {
        fx(() -> {
            BUSY.set(false);
            STOPPING.set(false);
        });
        LOCK.set(false);
    }

    // A stop of the run, which the waits of the running test notice
    private static volatile boolean stopRequested;
    private static final BooleanProperty STOPPING = new SimpleBooleanProperty();

    /**
     * Stops the run: the running test ends at its next wait, and the next
     * tests do not run.
     */
    static void stop() {
        if (LOCK.get()) {
            stopRequested = true;
            fx(() -> STOPPING.set(true));
        }
    }

    static boolean isStopping() {
        return stopRequested;
    }

    /**
     * A Stop button, enabled while a run goes on.
     */
    static Button stopButton(Button b) {
        b.getStyleClass().add("test-stop");
        b.disableProperty().bind(BUSY.not().or(STOPPING));
        b.setOnAction(e -> stop());
        return b;
    }

    private static void checkStopped() {
        if (stopRequested && !Platform.isFxApplicationThread()) {
            throw new Stopped();
        }
    }

    static ReadOnlyBooleanProperty busyProperty() {
        return BUSY;
    }

    /**
     * Runs the suites at startup with -Dtests.run=true, and exits afterwards
     * with -Dtests.exit=true.
     */
    static void autorun(Runnable runAll) {
        if (Boolean.getBoolean("tests.run")) {
            new Timeline(new KeyFrame(Duration.seconds(1.5), e -> runAll.run())).play();
        }
    }

    /**
     * Runs a suite at startup with -Dtests.run=true.
     */
    static void autorun(Suite suite) {
        autorun(() -> {
            if (!tryLock()) {
                return;
            }
            Thread.ofPlatform().daemon().name("tests-" + suite.name).start(() -> {
                Result r;
                try {
                    r = suite.run(suite.tests);
                } finally {
                    unlock();
                }
                exitIfAsked(r);
            });
        });
    }

    static void exitIfAsked(Result r) {
        if (Boolean.getBoolean("tests.exit")) {
            System.out.println("TESTS " + r);
            System.out.flush();
            System.exit(r.fail > 0 ? 1 : 0);
        }
    }

    // Why the desktop has no robot input, once a robot test found out
    private static volatile String noRobotInput;

    /**
     * Whether the robot tests run, unless turned off with -Dtests.robot=false:
     * outside the GNOME test session the robot takes over the pointer and the
     * keyboard of the desktop, which asks once for the permission.
     */
    static boolean robotInput() {
        return !"false".equals(System.getProperty("tests.robot"));
    }


    static Robot robot() {
        return fx(() -> {
            if (robot == null) {
                robot = new Robot();
            }
            return robot;
        });
    }

    // Releases what a failed robot test may have left pressed
    private static void releaseInput() {
        try {
            fx(() -> {
                Robot r = robot();
                r.mouseRelease(MouseButton.PRIMARY);
                for (KeyCode k : new KeyCode[] {KeyCode.SHIFT, KeyCode.CONTROL, KeyCode.ALT}) {
                    r.keyRelease(k);
                }
            });
        } catch (RuntimeException ignored) {
            // Nothing was pressed
        }
    }

    // ---------------------------------------------------------------------
    // Helpers for the test thread

    static void fx(Runnable r) {
        fx(() -> {
            r.run();
            return null;
        });
    }

    static <T> T fx(Supplier<T> s) {
        if (Platform.isFxApplicationThread()) {
            return s.get();
        }
        CompletableFuture<T> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                future.complete(s.get());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        try {
            return future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException re) {
                throw re;
            }
            if (e.getCause() instanceof Error err) {
                throw err;
            }
            throw e;
        }
    }

    /**
     * Polls the condition on the FX thread until it holds, failing after the
     * timeout.
     */
    static void await(String what, BooleanSupplier condition) {
        await(what, TIMEOUT_MS, condition);
    }

    static void await(String what, long timeoutMs, BooleanSupplier condition) {
        long deadline = System.nanoTime() + timeoutMs * 1_000_000;
        while (System.nanoTime() < deadline) {
            if (fx(condition::getAsBoolean)) {
                return;
            }
            pause(20);
        }
        throw new AssertionError("Timed out waiting for " + what);
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void skip(String why) {
        throw new Skipped(why);
    }

    static void pause(long ms) {
        checkStopped();
        try {
            // In steps, so that a stop does not wait for a long pause
            long deadline = System.nanoTime() + ms * 1_000_000;
            for (long left = ms; left > 0; left = (deadline - System.nanoTime()) / 1_000_000) {
                Thread.sleep(Math.min(left, 50));
                checkStopped();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static Bounds screenBounds(Node node) {
        Bounds b = node.localToScreen(node.getLayoutBounds());
        if (b == null) {
            throw new IllegalStateException(node + " is not showing");
        }
        return b;
    }

    static Point2D center(Node node) {
        Bounds b = screenBounds(node);
        return new Point2D(b.getCenterX(), b.getCenterY());
    }

    /**
     * Scrolls the scroll panes around a node so that it is visible, on the
     * test thread, and waits for the layout.
     */
    static void reveal(Node node) {
        fx(() -> {
            for (Node p = node.getParent(); p != null; p = p.getParent()) {
                if (p instanceof ScrollPane sp && sp.getContent() != null) {
                    Node content = sp.getContent();
                    Bounds b = content.sceneToLocal(node.localToScene(node.getLayoutBounds()));
                    double contentHeight = content.getLayoutBounds().getHeight();
                    double viewport = sp.getViewportBounds().getHeight();
                    if (b != null && contentHeight > viewport) {
                        double top = sp.getVvalue() * (contentHeight - viewport);
                        if (b.getMinY() < top || b.getMaxY() > top + viewport) {
                            double y = Math.max(0, Math.min(b.getMinY() - 16, contentHeight - viewport));
                            sp.setVvalue(y / (contentHeight - viewport));
                        }
                    }
                }
            }
        });
        pause(250);
    }

    /**
     * Runs an action once the node is in a scene.
     */
    static void whenInScene(Node node, Consumer<Scene> action) {
        if (node.getScene() != null) {
            action.accept(node.getScene());
            return;
        }
        node.sceneProperty().addListener(new javafx.beans.value.ChangeListener<>() {
            @Override
            public void changed(javafx.beans.value.ObservableValue<? extends Scene> obs, Scene o, Scene n) {
                if (n != null) {
                    node.sceneProperty().removeListener(this);
                    action.accept(n);
                }
            }
        });
    }

    // ---------------------------------------------------------------------
    // Robot input

    static void move(Point2D p) {
        fx(() -> robot().mouseMove(p));
    }

    static void click(Point2D p) {
        move(p);
        pause(60);
        fx(() -> robot().mouseClick(MouseButton.PRIMARY));
    }

    static void click(Node node) {
        reveal(node);
        click(fx(() -> center(node)));
    }

    /**
     * Presses at a point, moves to another in steps and releases, holding the
     * given modifiers from the press to the release.
     */
    static void drag(Point2D from, Point2D to, KeyCode... modifiers) {
        Robot r = robot();
        // Hover first, so that the window has the pointer when the button goes down
        move(from.add(-2, 0));
        pause(150);
        move(from);
        pause(250);
        fx(() -> {
            for (KeyCode k : modifiers) {
                r.keyPress(k);
            }
            r.mousePress(MouseButton.PRIMARY);
        });
        pause(80);
        // GTK delivers the events at the next frame of the window, which an idle
        // compositor draws late: start slowly, so that the drag begins before
        // the pointer leaves the source
        double dx = Math.signum(to.getX() - from.getX()), dy = Math.signum(to.getY() - from.getY());
        for (int i = 1; i <= 4; i++) {
            move(from.add(dx * 3 * i, dy * 3 * i));
            pause(120);
        }
        int steps = 24;
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            Point2D p = new Point2D(from.getX() + (to.getX() - from.getX()) * t,
                    from.getY() + (to.getY() - from.getY()) * t);
            move(p);
            pause(25);
        }
        // Hover over the target, so that it accepts the drop before the release:
        // the compositor cancels a drop that the target has not accepted yet
        for (int i = 0; i < 10; i++) {
            move(to.add(i % 2, 0));
            pause(80);
        }
        fx(() -> {
            r.mouseRelease(MouseButton.PRIMARY);
            for (KeyCode k : modifiers) {
                r.keyRelease(k);
            }
        });
        // And motions after the release, so that the drop is delivered
        for (int i = 0; i < 4; i++) {
            pause(80);
            move(to.add((i + 1) % 2, 0));
        }
    }

    static void drag(Node from, Node to, KeyCode... modifiers) {
        reveal(from);
        Point2D a = fx(() -> center(from));
        reveal(to);
        Point2D b = fx(() -> center(to));
        drag(a, b, modifiers);
    }

    /**
     * Types a key with modifiers, such as Ctrl+C.
     */
    static void shortcut(KeyCode key, KeyCode... modifiers) {
        fx(() -> {
            Robot r = robot();
            for (KeyCode k : modifiers) {
                r.keyPress(k);
            }
            r.keyType(key);
            for (int i = modifiers.length - 1; i >= 0; i--) {
                r.keyRelease(modifiers[i]);
            }
        });
    }

    /**
     * Types letters, digits, spaces and the characters of paths (/ . - _),
     * with Shift for capitals and the underscore, on a US layout.
     */
    static void type(String text) {
        for (char c : text.toCharArray()) {
            KeyCode code = switch (c) {
                case ' ' -> KeyCode.SPACE;
                case '/' -> KeyCode.SLASH;
                case '.' -> KeyCode.PERIOD;
                case '-', '_' -> KeyCode.MINUS;
                case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> KeyCode.getKeyCode(String.valueOf(c));
                default -> KeyCode.getKeyCode(String.valueOf(Character.toUpperCase(c)));
            };
            if (code == null) {
                throw new IllegalArgumentException("Cannot type '" + c + "'");
            }
            if (Character.isUpperCase(c) || c == '_') {
                shortcut(code, KeyCode.SHIFT);
            } else {
                fx(() -> robot().keyType(code));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Touch input, through the touch robot of the GTK 4 glass

    private static java.lang.reflect.Method touchMethod(String name, Class<?>... types) {
        try {
            return Class.forName("com.sun.glass.ui.gtk4.Gtk4TouchRobot").getMethod(name, types);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static Object invokeTouch(String name, Class<?>[] types, Object... args) {
        java.lang.reflect.Method m = touchMethod(name, types);
        if (m == null) {
            throw new Skipped("This platform has no touch robot");
        }
        return fx(() -> {
            try {
                return m.invoke(null, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause() instanceof RuntimeException re ? re : new IllegalStateException(e.getCause());
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private static Boolean touchAvailable;

    /**
     * Whether the robot can touch: libei and the compositor support touch
     * screens, which is known at run time.
     */
    static boolean touchAvailable() {
        if (touchAvailable == null) {
            try {
                touchAvailable = Boolean.TRUE.equals(invokeTouch("isAvailable", new Class<?>[0]));
            } catch (RuntimeException e) {
                touchAvailable = false;
            }
        }
        return touchAvailable;
    }

    static void touchDown(int id, Point2D p) {
        invokeTouch("down", new Class<?>[] {int.class, double.class, double.class}, id, p.getX(), p.getY());
    }

    static void touchMove(int id, Point2D p) {
        invokeTouch("move", new Class<?>[] {int.class, double.class, double.class}, id, p.getX(), p.getY());
    }

    static void touchUp(int id) {
        invokeTouch("up", new Class<?>[] {int.class}, id);
    }

    /**
     * Waits for a touch test to be passed or failed by the events the lab sees.
     */
    static String observed(Test t) {
        await("the events of " + t.name, 4000, () -> isVerdict(t.status.get()));
        check(t.status.get() == Status.OK, fx(() -> t.detail.get()));
        return fx(() -> t.detail.get());
    }

    /**
     * Opaque lists for the popups of the combo and choice boxes, which take
     * the stylesheets of the lab, whose lists are transparent.
     */
    private static final String POPUP_CSS = """
        .combo-box-popup > .list-view {
            -fx-background-color: #181d2b;
            -fx-background-radius: 8;
            -fx-border-color: rgba(255, 255, 255, 0.16);
            -fx-border-radius: 8;
            -fx-padding: 2;
            -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.45), 14, 0, 0, 4);
        }
        .combo-box-popup > .list-view > .virtual-flow > .clipped-container > .sheet > .list-cell {
            -fx-background-color: transparent;
            -fx-text-fill: #e8eef7;
            -fx-padding: 4 10 4 10;
        }
        .combo-box-popup > .list-view > .virtual-flow > .clipped-container > .sheet > .list-cell:hover {
            -fx-background-color: rgba(255, 255, 255, 0.08);
        }
        .combo-box-popup > .list-view > .virtual-flow > .clipped-container > .sheet > .list-cell:filled:selected {
            -fx-background-color: rgba(99, 102, 241, 0.55);
        }
        .choice-box > .context-menu, .context-menu {
            -fx-background-color: #181d2b;
            -fx-background-radius: 8;
            -fx-border-color: rgba(255, 255, 255, 0.16);
            -fx-border-radius: 8;
        }
        .context-menu .menu-item .label { -fx-text-fill: #e8eef7; }
        .context-menu .menu-item:focused { -fx-background-color: rgba(99, 102, 241, 0.55); }
        """;
    static final String POPUP_STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(POPUP_CSS.getBytes(StandardCharsets.UTF_8));

    private static final String DIALOG_CSS = """
        .root {
            -fx-font-family: "Adwaita Sans", "Cantarell", "System";
            -fx-font-size: 13px;
            -fx-base: #1c2230;
            -fx-background: #161b28;
            -fx-control-inner-background: #11151f;
            -fx-accent: #6366f1;
            -fx-focus-color: #818cf8;
            -fx-faint-focus-color: #818cf822;
            -fx-text-base-color: #e8eef7;
            -fx-text-background-color: #e8eef7;
        }
        .dialog-pane { -fx-background-color: -fx-background; }
        .dialog-pane > .header-panel {
            -fx-background-color: derive(-fx-background, 10%);
            -fx-border-color: transparent transparent rgba(255, 255, 255, 0.08) transparent;
        }
        .dialog-pane .label { -fx-text-fill: #e8eef7; }
        .dialog-pane > .button-bar > .container > .button {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 9;
            -fx-text-fill: #e8eef7;
        }
        .dialog-pane > .button-bar > .container > .button:hover { -fx-background-color: rgba(255, 255, 255, 0.15); }
        .dialog-pane > .button-bar > .container > .button:default {
            -fx-background-color: #6366f1;
            -fx-border-color: transparent;
        }
        .dialog-pane > .button-bar > .container > .button:default:hover { -fx-background-color: #818cf8; }
        """;
    private static final String DIALOG_STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(DIALOG_CSS.getBytes(StandardCharsets.UTF_8));
    private static boolean darkDialogs;

    /**
     * Gives the dialogs of the labs (alerts, text input and choice dialogs)
     * the dark look of the labs, whose stylesheets only their own scenes have.
     */
    private static void darkDialogs() {
        if (darkDialogs || !Platform.isFxApplicationThread()) {
            return;
        }
        darkDialogs = true;
        javafx.collections.ListChangeListener<javafx.stage.Window> windows = c -> {
            while (c.next()) {
                for (javafx.stage.Window w : c.getAddedSubList()) {
                    darkDialog(w.getScene());
                    w.sceneProperty().addListener((obs, o, scene) -> darkDialog(scene));
                }
            }
        };
        javafx.stage.Window.getWindows().addListener(windows);
    }

    private static void darkDialog(javafx.scene.Scene scene) {
        if (scene != null && scene.getRoot() instanceof javafx.scene.control.DialogPane
                && !scene.getStylesheets().contains(DIALOG_STYLESHEET)) {
            scene.getStylesheets().add(DIALOG_STYLESHEET);
            scene.getPreferences().setColorScheme(javafx.application.ColorScheme.DARK);
        }
    }

    /**
     * Scrolls the list of a test, as it runs, so that its row shows.
     */
    private static void followRunning(Test t, Node first, Node last) {
        t.status.addListener((obs, o, n) -> {
            if (n == Status.RUNNING) {
                // After the layout of the new detail
                Platform.runLater(() -> scrollIntoView(first, last));
            }
        });
    }

    /**
     * Scrolls the nearest scroll pane around two nodes, the first above the
     * last, so that both show, with a little of what follows.
     */
    static void scrollIntoView(Node first, Node last) {
        for (Node p = first.getParent(); p != null; p = p.getParent()) {
            if (p instanceof ScrollPane sp && sp.getContent() != null) {
                Node content = sp.getContent();
                Bounds a = content.sceneToLocal(first.localToScene(first.getLayoutBounds()));
                Bounds b = content.sceneToLocal(last.localToScene(last.getLayoutBounds()));
                double contentHeight = content.getLayoutBounds().getHeight();
                double viewport = sp.getViewportBounds().getHeight();
                if (a == null || b == null || contentHeight <= viewport) {
                    return;
                }
                double range = contentHeight - viewport;
                double top = sp.getVvalue() * range;
                double margin = Math.min(48, viewport / 4);
                double y = top;
                if (b.getMaxY() + margin > top + viewport) {
                    y = b.getMaxY() + margin - viewport;
                }
                if (a.getMinY() < y) {
                    y = a.getMinY();
                }
                sp.setVvalue(Math.max(0, Math.min(y, range)) / range);
                return;
            }
        }
    }

    /**
     * Lets a card grow with its column, as the card of the tests does.
     */
    static <T extends Node> T grow(T node) {
        VBox.setVgrow(node, Priority.ALWAYS);
        return node;
    }

    static String fmt(Point2D p) {
        return p == null ? "none" : String.format(Locale.ROOT, "%.1f, %.1f", p.getX(), p.getY());
    }
}
