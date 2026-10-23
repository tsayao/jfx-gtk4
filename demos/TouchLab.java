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

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.ColorScheme;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.GestureEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.RotateEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.SwipeEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.input.ZoomEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * A laboratory for touch input: paints the touch points of a touch screen
 * with their ids and trails, and moves, zooms and rotates a card with the
 * scroll, zoom, rotate and swipe gestures, of a touch screen (direct) or of a
 * touchpad (indirect). Counters show which events arrive, and whether mouse
 * events are synthesized from touches.
 * <p>
 * The robot cannot touch, so its tests (see TestKit) are manual: they pass
 * when the lab sees the events of a touch screen or touchpad.
 */
public class TouchLab {

    private static final String CSS = """
        .root {
            -fx-font-family: "Adwaita Sans", "Cantarell", "System";
            -fx-font-size: 13px;
            -fx-base: #1c2230;
            -fx-background: transparent;
            -fx-control-inner-background: #151a26;
            -fx-accent: #22c55e;
            -fx-focus-color: #4ade80;
            -fx-faint-focus-color: #4ade8022;
            -fx-text-fill: #e8eef7;
        }
        .label, .check-box { -fx-text-fill: #e8eef7; }
        .muted { -fx-text-fill: #93a1b8; }
        .mono { -fx-font-family: "Adwaita Mono", "Monospace"; }
        .glass {
            -fx-background-color: rgba(10, 14, 24, 0.52);
            -fx-background-radius: 18;
            -fx-border-color: rgba(255, 255, 255, 0.16) rgba(255, 255, 255, 0.07)
                rgba(255, 255, 255, 0.04) rgba(255, 255, 255, 0.07);
            -fx-border-radius: 18;
            -fx-padding: 16;
        }
        .section { -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #93a1b8; }
        .brand { -fx-font-size: 15px; -fx-font-weight: bold; }
        .pill {
            -fx-background-color: rgba(255, 255, 255, 0.07);
            -fx-background-radius: 999;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 999;
            -fx-padding: 5 12 5 12;
            -fx-alignment: center;
        }
        .pill .label { -fx-font-size: 12px; }
        .surface {
            -fx-background-color: rgba(255, 255, 255, 0.035);
            -fx-background-radius: 14;
            -fx-border-color: rgba(255, 255, 255, 0.12);
            -fx-border-radius: 14;
        }
        .card-photo {
            -fx-background-color: linear-gradient(to bottom right, #22c55e, #0ea5e9 55%, #6366f1);
            -fx-background-radius: 16;
            -fx-border-color: rgba(255, 255, 255, 0.5);
            -fx-border-radius: 16;
            -fx-border-width: 2;
        }
        .card-photo .label { -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16px; }
        .key { -fx-text-fill: #93a1b8; -fx-font-size: 12px; }
        .value { -fx-font-family: "Adwaita Mono", "Monospace"; -fx-font-size: 12px; }
        .value.hot { -fx-text-fill: #4ade80; }
        .swipe { -fx-font-size: 54px; -fx-font-weight: bold; -fx-text-fill: white; }
        .check-box > .box {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 5;
            -fx-border-color: rgba(255, 255, 255, 0.22);
            -fx-border-radius: 5;
        }
        .check-box:selected > .box { -fx-background-color: #22c55e; -fx-border-color: transparent; }
        .check-box:selected > .box > .mark { -fx-background-color: white; }
        .button {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 9;
            -fx-text-fill: #e8eef7;
            -fx-padding: 6 14 6 14;
        }
        .button:hover { -fx-background-color: rgba(255, 255, 255, 0.15); }
        .list-view { -fx-background-color: transparent; -fx-border-color: transparent; }
        .list-view .list-cell { -fx-background-color: transparent; -fx-text-fill: #e8eef7; }
        .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-bar { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: rgba(255, 255, 255, 0.25); -fx-background-radius: 6; }
        .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 0; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-shape: " "; -fx-padding: 0; }
        """;

    static final String STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));

    private static final Color[] FINGERS = {
        Color.web("#22c55e"), Color.web("#0ea5e9"), Color.web("#f59e0b"), Color.web("#d946ef"),
        Color.web("#f43f5e"), Color.web("#a3e635"), Color.web("#6366f1"), Color.web("#fb923c"),
        Color.web("#2dd4bf"), Color.web("#e879f9")
    };
    private static final int LOG_SIZE = 200;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private boolean backdropSupported;

    // Touch canvas
    private final Pane touchPane = new Pane();
    private final Canvas trails = new Canvas();
    private final Group markers = new Group();
    private final Map<Integer, Node> markerById = new HashMap<>();
    private final Map<Integer, Point2D> lastById = new HashMap<>();
    private final Label pointsPill = new Label("0 touch points");
    private final CheckBox keepTrails = new CheckBox("Keep trails");

    // Gesture card
    private final StackPane photo = new StackPane();
    private final Label swipeArrow = new Label();
    private final Map<String, Label> readouts = new LinkedHashMap<>();

    // Event counters
    private final Map<EventType<?>, Label> counters = new LinkedHashMap<>();
    private final Map<EventType<?>, Integer> counts = new HashMap<>();
    private final ObservableList<String> log = FXCollections.observableArrayList();
    private final ListView<String> logView = new ListView<>(log);

    // Tests
    final TestKit.Suite suite = new TestKit.Suite("Touch");
    private StackPane gestureArea;
    private final TestKit.Test touchTest = suite.touch("Touch points",
            "A finger pressed and released on the touch canvas, with the same id", () -> {
                tap(touchPane, 0, 0);
                return TestKit.observed(touchTest());
            });
    private final TestKit.Test multiTest = suite.touch("Several fingers", "Two fingers on the touch canvas at once",
            () -> {
                Point2D c = TestKit.fx(() -> TestKit.center(touchPane));
                TestKit.touchDown(1, c.add(-60, 0));
                TestKit.pause(50);
                TestKit.touchDown(2, c.add(60, 0));
                TestKit.pause(100);
                TestKit.touchMove(1, c.add(-70, 10));
                TestKit.touchMove(2, c.add(70, 10));
                TestKit.pause(100);
                TestKit.touchUp(2);
                TestKit.touchUp(1);
                return TestKit.observed(multiTest());
            });
    private final TestKit.Test synthTest = suite.touch("Mouse from touch",
            "A touch also delivers a MOUSE_PRESSED flagged as synthesized", () -> {
                tap(touchPane, 40, 30);
                return TestKit.observed(synthTest());
            });
    private final TestKit.Test touchScrollTest = suite.touch("Touch scroll",
            "A finger dragged on the gesture area scrolls the card (a direct SCROLL)", () -> {
                Point2D c = TestKit.fx(() -> TestKit.center(gestureArea));
                TestKit.touchDown(1, c.add(0, -60));
                for (int i = 1; i <= 15; i++) {
                    TestKit.pause(16);
                    TestKit.touchMove(1, c.add(0, -60 + i * 8));
                }
                TestKit.pause(50);
                TestKit.touchUp(1);
                return TestKit.observed(touchScrollTest());
            });
    private final TestKit.Test zoomTest = suite.touch("Zoom", "Pinch (spread two fingers) on the gesture area", () -> {
        Point2D c = TestKit.fx(() -> TestKit.center(gestureArea));
        twoFingers(c, i -> 30 + i * 5.0, i -> 0.0);
        return TestKit.observed(zoomTest());
    });
    private final TestKit.Test rotateTest = suite.touch("Rotate", "Rotate two fingers on the gesture area", () -> {
        Point2D c = TestKit.fx(() -> TestKit.center(gestureArea));
        twoFingers(c, i -> 60.0, i -> Math.toRadians(i * 3.0));
        return TestKit.observed(rotateTest());
    });
    private final TestKit.Test swipeTest = suite.manual("Swipe", "Swipe on the gesture area (three fingers on a touchpad)");

    private TestKit.Test touchTest() {
        return touchTest;
    }

    private TestKit.Test multiTest() {
        return multiTest;
    }

    private TestKit.Test synthTest() {
        return synthTest;
    }

    private TestKit.Test touchScrollTest() {
        return touchScrollTest;
    }

    private TestKit.Test zoomTest() {
        return zoomTest;
    }

    private TestKit.Test rotateTest() {
        return rotateTest;
    }

    /**
     * Touches a node with one finger of the touch robot, at an offset from its
     * center, on the test thread.
     */
    private static void tap(Node node, double dx, double dy) {
        TestKit.reveal(node);
        Point2D p = TestKit.fx(() -> TestKit.center(node)).add(dx, dy);
        TestKit.touchDown(1, p);
        TestKit.pause(120);
        TestKit.touchUp(1);
    }

    /**
     * Moves two fingers of the touch robot in 20 steps, opposite each other
     * around a point, at the given radius and angle for each step.
     */
    private static void twoFingers(Point2D c, java.util.function.IntFunction<Double> radius,
            java.util.function.IntFunction<Double> angle) {
        Point2D[] p = new Point2D[2];
        for (int i = 0; i <= 20; i++) {
            double r = radius.apply(i), a = angle.apply(i);
            p[0] = c.add(-r * Math.cos(a), -r * Math.sin(a));
            p[1] = c.add(r * Math.cos(a), r * Math.sin(a));
            if (i == 0) {
                TestKit.touchDown(1, p[0]);
                TestKit.touchDown(2, p[1]);
            } else {
                TestKit.touchMove(1, p[0]);
                TestKit.touchMove(2, p[1]);
            }
            TestKit.pause(16);
        }
        TestKit.pause(50);
        TestKit.touchUp(2);
        TestKit.touchUp(1);
    }

    /**
     * The lab without its header bar, which the tests launcher shows in a tab.
     */
    Region content(Stage stage) {
        backdropSupported = Boolean.getBoolean("glass.gtk4.backdrop.active");
        VBox left = column(touchCard(), TestKit.grow(card("Tests", suite.view())));
        VBox right = column(gestureCard(), eventsCard());
        VBox.setVgrow(left.getChildren().getFirst(), Priority.ALWAYS);
        HBox columns = new HBox(14, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        right.setPrefWidth(420);
        right.setMinWidth(380);
        columns.setPadding(new Insets(4, 18, 18, 18));
        // Short of room, the cards are cut at the bottom, the header stays
        columns.setMinHeight(0);
        return columns;
    }

    private static VBox column(Node... cards) {
        VBox box = new VBox(14, cards);
        box.setFillWidth(true);
        box.setMinWidth(0);
        return box;
    }

    private static VBox card(String title, Node... content) {
        Label label = new Label(title.toUpperCase(Locale.ROOT));
        label.getStyleClass().add("section");
        VBox box = new VBox(10);
        box.getChildren().add(label);
        box.getChildren().addAll(content);
        box.getStyleClass().add("glass");
        return box;
    }

    private static Label muted(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        l.setWrapText(true);
        return l;
    }

    // ---------------------------------------------------------------------
    // Touch points

    private Node touchCard() {
        touchPane.getStyleClass().add("surface");
        touchPane.setMinSize(200, 200);
        trails.widthProperty().bind(touchPane.widthProperty());
        trails.heightProperty().bind(touchPane.heightProperty());
        trails.widthProperty().addListener((obs, o, n) -> clearTrails());
        trails.heightProperty().addListener((obs, o, n) -> clearTrails());
        markers.setManaged(false);
        Label placeholder = muted("Touch here with one or more fingers");
        placeholder.layoutXProperty().bind(touchPane.widthProperty().subtract(placeholder.widthProperty()).divide(2));
        placeholder.layoutYProperty().bind(touchPane.heightProperty().divide(2));
        placeholder.setMouseTransparent(true);
        touchPane.getChildren().addAll(placeholder, trails, markers);
        VBox.setVgrow(touchPane, Priority.ALWAYS);

        touchPane.addEventHandler(TouchEvent.ANY, e -> {
            count(e.getEventType());
            TouchPoint tp = e.getTouchPoint();
            Point2D p = touchPane.sceneToLocal(tp.getSceneX(), tp.getSceneY());
            int id = tp.getId();
            switch (tp.getState()) {
                case PRESSED -> {
                    placeholder.setVisible(false);
                    if (e.getTouchCount() >= 2) {
                        multiTest.pass(e.getTouchCount() + " touch points at once");
                    }
                    if (!keepTrails.isSelected() && e.getTouchCount() == 1) {
                        clearTrails();
                    }
                    lastById.put(id, p);
                    Node marker = marker(id);
                    markerById.put(id, marker);
                    markers.getChildren().add(marker);
                    marker.relocate(p.getX(), p.getY());
                    log(String.format(Locale.ROOT, "TOUCH_PRESSED   id=%d at %.0f, %.0f (%d points)", id,
                            p.getX(), p.getY(), e.getTouchCount()));
                }
                case MOVED -> {
                    Point2D last = lastById.put(id, p);
                    if (last != null) {
                        GraphicsContext g = trails.getGraphicsContext2D();
                        g.setStroke(finger(id).deriveColor(0, 1, 1, 0.8));
                        g.setLineWidth(4);
                        g.setLineCap(StrokeLineCap.ROUND);
                        g.strokeLine(last.getX(), last.getY(), p.getX(), p.getY());
                    }
                    Node marker = markerById.get(id);
                    if (marker != null) {
                        marker.relocate(p.getX(), p.getY());
                    }
                }
                case RELEASED -> {
                    if (lastById.containsKey(id)) {
                        touchTest.pass("id " + id + " pressed and released");
                    } else {
                        touchTest.fail("TOUCH_RELEASED for id " + id + ", which was not pressed");
                    }
                    lastById.remove(id);
                    Node marker = markerById.remove(id);
                    if (marker != null) {
                        FadeTransition fade = new FadeTransition(Duration.millis(300), marker);
                        fade.setToValue(0);
                        fade.setOnFinished(f -> markers.getChildren().remove(marker));
                        fade.play();
                    }
                    log(String.format(Locale.ROOT, "TOUCH_RELEASED  id=%d at %.0f, %.0f", id, p.getX(), p.getY()));
                }
                default -> { }
            }
            pointsPill.setText(markerById.size() + (markerById.size() == 1 ? " touch point" : " touch points"));
            e.consume();
        });
        touchPane.addEventHandler(MouseEvent.ANY, e -> {
            if (e.getEventType() == MouseEvent.MOUSE_PRESSED || e.getEventType() == MouseEvent.MOUSE_RELEASED
                    || e.getEventType() == MouseEvent.MOUSE_DRAGGED) {
                count(e.isSynthesized() ? SYNTHESIZED_MOUSE : e.getEventType());
                if (e.getEventType() == MouseEvent.MOUSE_PRESSED && e.isSynthesized()) {
                    synthTest.pass("MOUSE_PRESSED synthesized from a touch");
                }
                if (e.getEventType() != MouseEvent.MOUSE_DRAGGED) {
                    log(e.getEventType() + (e.isSynthesized() ? " (synthesized from touch)" : " (pointer)"));
                }
            }
        });

        Button clear = new Button("Clear");
        clear.setOnAction(e -> clearTrails());
        HBox tools = new HBox(12, clear, keepTrails);
        tools.setAlignment(Pos.CENTER_LEFT);
        Label hint = muted("Each finger gets a color and its id. The glass reports touch screen points; "
                + "a touchpad does not send touch points, only the gestures on the right.");
        VBox box = card("Touch points", touchPane, tools, hint);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private static final EventType<Event> SYNTHESIZED_MOUSE = new EventType<>("MOUSE_SYNTHESIZED");

    private Node marker(int id) {
        Color c = finger(id);
        Circle ring = new Circle(30, c.deriveColor(0, 1, 1, 0.25));
        ring.setStroke(c);
        ring.setStrokeWidth(3);
        ring.setEffect(new DropShadow(18, c));
        Text label = new Text(String.valueOf(id));
        label.setFill(Color.WHITE);
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 15px;");
        StackPane marker = new StackPane(ring, label);
        marker.setMouseTransparent(true);
        marker.setTranslateX(-30);
        marker.setTranslateY(-30);
        return marker;
    }

    private static Color finger(int id) {
        return FINGERS[Math.floorMod(id - 1, FINGERS.length)];
    }

    private void clearTrails() {
        trails.getGraphicsContext2D().clearRect(0, 0, trails.getWidth(), trails.getHeight());
    }

    // ---------------------------------------------------------------------
    // Gestures

    private Node gestureCard() {
        Label title = new Label("Pinch · rotate · drag · swipe");
        photo.getChildren().add(title);
        photo.getStyleClass().add("card-photo");
        photo.setMaxSize(220, 140);
        photo.setPrefSize(220, 140);
        swipeArrow.getStyleClass().add("swipe");
        swipeArrow.setOpacity(0);
        swipeArrow.setMouseTransparent(true);

        StackPane area = new StackPane(photo, swipeArrow);
        gestureArea = area;
        area.getStyleClass().add("surface");
        area.setMinHeight(260);
        area.setPrefHeight(280);
        area.setClip(new javafx.scene.shape.Rectangle());
        area.layoutBoundsProperty().addListener((obs, o, b) -> {
            javafx.scene.shape.Rectangle r = (javafx.scene.shape.Rectangle) area.getClip();
            r.setWidth(b.getWidth());
            r.setHeight(b.getHeight());
            r.setArcWidth(28);
            r.setArcHeight(28);
        });

        area.addEventHandler(ZoomEvent.ANY, e -> {
            count(e.getEventType());
            if (e.getEventType() == ZoomEvent.ZOOM) {
                zoomTest.pass(String.format(Locale.ROOT, "factor %.3f, %s", e.getZoomFactor(), source(e)));
                double scale = clamp(photo.getScaleX() * e.getZoomFactor(), 0.2, 6);
                photo.setScaleX(scale);
                photo.setScaleY(scale);
            }
            gesture(e, String.format(Locale.ROOT, "factor %.3f, total %.3f", e.getZoomFactor(), e.getTotalZoomFactor()));
            e.consume();
        });
        area.addEventHandler(RotateEvent.ANY, e -> {
            count(e.getEventType());
            if (e.getEventType() == RotateEvent.ROTATE) {
                rotateTest.pass(String.format(Locale.ROOT, "angle %.2f°, %s", e.getAngle(), source(e)));
                photo.setRotate(photo.getRotate() + e.getAngle());
            }
            gesture(e, String.format(Locale.ROOT, "angle %.2f°, total %.2f°", e.getAngle(), e.getTotalAngle()));
            e.consume();
        });
        area.addEventHandler(ScrollEvent.ANY, e -> {
            count(e.getEventType());
            if (e.getEventType() == ScrollEvent.SCROLL && e.isDirect() && e.getTouchCount() > 0) {
                touchScrollTest.pass(String.format(Locale.ROOT, "delta %.1f, %.1f with %d touches",
                        e.getDeltaX(), e.getDeltaY(), e.getTouchCount()));
            }
            photo.setTranslateX(photo.getTranslateX() + e.getDeltaX());
            photo.setTranslateY(photo.getTranslateY() + e.getDeltaY());
            gesture(e, String.format(Locale.ROOT, "delta %.1f, %.1f · total %.1f, %.1f · %d touches%s",
                    e.getDeltaX(), e.getDeltaY(), e.getTotalDeltaX(), e.getTotalDeltaY(), e.getTouchCount(),
                    e.isInertia() ? " · inertia" : ""));
            e.consume();
        });
        area.addEventHandler(SwipeEvent.ANY, e -> {
            count(e.getEventType());
            swipeTest.pass(e.getEventType() + " with " + e.getTouchCount() + " touches, " + source(e));
            showSwipe(e.getEventType());
            gesture(e, e.getTouchCount() + " touches");
            log(e.getEventType() + " with " + e.getTouchCount() + " touches"
                    + (e.isDirect() ? " (touch screen)" : " (touchpad)"));
            e.consume();
        });
        for (EventType<?> type : new EventType<?>[] {
                ZoomEvent.ZOOM_STARTED, ZoomEvent.ZOOM_FINISHED, RotateEvent.ROTATION_STARTED,
                RotateEvent.ROTATION_FINISHED, ScrollEvent.SCROLL_STARTED, ScrollEvent.SCROLL_FINISHED}) {
            @SuppressWarnings("unchecked")
            EventType<GestureEvent> t = (EventType<GestureEvent>) type;
            area.addEventFilter(t, e -> log(e.getEventType() + (e.isDirect() ? " (touch screen)" : " (touchpad)")
                    + (e.isInertia() ? " inertia" : "")));
        }

        GridPane grid = new GridPane(12, 4);
        int row = 0;
        for (String key : new String[] {"Last", "Source", "Zoom", "Rotate", "Scroll", "Swipe", "Card"}) {
            Label k = new Label(key);
            k.getStyleClass().add("key");
            Label v = new Label("–");
            v.getStyleClass().add("value");
            readouts.put(key, v);
            grid.addRow(row++, k, v);
        }

        Button reset = new Button("Reset card");
        reset.setOnAction(e -> {
            photo.setScaleX(1);
            photo.setScaleY(1);
            photo.setRotate(0);
            photo.setTranslateX(0);
            photo.setTranslateY(0);
            updateCardReadout();
        });
        Label hint = muted("On a touch screen use two fingers; on a touchpad pinch, rotate, scroll with two "
                + "fingers and swipe with three. Ctrl + wheel does not zoom: only real gestures do.");
        return card("Gestures", area, grid, reset, hint);
    }

    private static String source(GestureEvent e) {
        return e.isDirect() ? "touch screen" : "touchpad";
    }

    private void gesture(GestureEvent e, String detail) {
        Label last = readouts.get("Last");
        last.setText(e.getEventType().getName());
        readouts.get("Source").setText((e.isDirect() ? "touch screen (direct)" : "touchpad / wheel (indirect)")
                + (e.isInertia() ? ", inertia" : ""));
        String key = e instanceof ZoomEvent ? "Zoom" : e instanceof RotateEvent ? "Rotate"
                : e instanceof ScrollEvent ? "Scroll" : "Swipe";
        Label value = readouts.get(key);
        value.setText(detail);
        readouts.values().forEach(l -> l.getStyleClass().remove("hot"));
        value.getStyleClass().add("hot");
        updateCardReadout();
    }

    private void updateCardReadout() {
        readouts.get("Card").setText(String.format(Locale.ROOT, "scale %.2f · %.1f° · at %.0f, %.0f",
                photo.getScaleX(), photo.getRotate(), photo.getTranslateX(), photo.getTranslateY()));
    }

    private void showSwipe(EventType<?> type) {
        String arrow;
        double dx = 0, dy = 0;
        if (type == SwipeEvent.SWIPE_LEFT) {
            arrow = "←";
            dx = -60;
        } else if (type == SwipeEvent.SWIPE_RIGHT) {
            arrow = "→";
            dx = 60;
        } else if (type == SwipeEvent.SWIPE_UP) {
            arrow = "↑";
            dy = -60;
        } else {
            arrow = "↓";
            dy = 60;
        }
        swipeArrow.setText(arrow);
        swipeArrow.setTranslateX(0);
        swipeArrow.setTranslateY(0);
        FadeTransition fade = new FadeTransition(Duration.millis(700), swipeArrow);
        fade.setFromValue(1);
        fade.setToValue(0);
        TranslateTransition move = new TranslateTransition(Duration.millis(700), swipeArrow);
        move.setByX(dx);
        move.setByY(dy);
        new ParallelTransition(fade, move).play();
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    // ---------------------------------------------------------------------
    // Counters and log

    private Node eventsCard() {
        GridPane grid = new GridPane(12, 2);
        EventType<?>[][] rows = {
            {TouchEvent.TOUCH_PRESSED, TouchEvent.TOUCH_MOVED},
            {TouchEvent.TOUCH_STATIONARY, TouchEvent.TOUCH_RELEASED},
            {ZoomEvent.ZOOM, RotateEvent.ROTATE},
            {ScrollEvent.SCROLL, SwipeEvent.ANY},
            {MouseEvent.MOUSE_PRESSED, SYNTHESIZED_MOUSE},
        };
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < 2; c++) {
                EventType<?> type = rows[r][c];
                Label k = new Label(type == SwipeEvent.ANY ? "SWIPE_*" : type.getName());
                k.getStyleClass().add("key");
                Label v = new Label("0");
                v.getStyleClass().add("value");
                v.setMinWidth(48);
                counters.put(type, v);
                grid.add(k, c * 2, r);
                grid.add(v, c * 2 + 1, r);
            }
        }

        logView.setPrefHeight(150);
        logView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty ? null : s);
                getStyleClass().add("mono");
                setStyle("-fx-padding: 1 6 1 6; -fx-font-size: 11px;");
            }
        });
        VBox.setVgrow(logView, Priority.ALWAYS);
        Button clear = new Button("Clear");
        clear.setOnAction(e -> {
            log.clear();
            counts.clear();
            counters.values().forEach(l -> l.setText("0"));
        });
        VBox box = card("Events", grid, logView, clear);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private void count(EventType<?> type) {
        EventType<?> key = type.getSuperType() == SwipeEvent.ANY ? SwipeEvent.ANY : type;
        Label label = counters.get(key);
        if (label != null) {
            int n = counts.merge(key, 1, Integer::sum);
            label.setText(String.valueOf(n));
        }
    }

    private void log(String text) {
        log.add(LocalTime.now().format(CLOCK) + "  " + text);
        if (log.size() > LOG_SIZE) {
            log.remove(0, log.size() - LOG_SIZE);
        }
        logView.scrollTo(log.size() - 1);
    }
}
