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
import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
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
import javafx.scene.robot.Robot;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * A self test of javafx.scene.robot.Robot: moves the pointer, clicks, scrolls,
 * drags a slider, types into a text field and reads back pixels and a screen
 * capture of its own window, checking each step against the events the scene
 * receives.
 * <p>
 * The GTK 4 glass generates robot input through the remote desktop session of
 * GNOME, so the input tests need the nested GNOME Shell of the launcher and
 * -Dglass.gtk4.testSession=gnome, which the launcher passes to this demo:
 * <pre>./launcher.sh --apps=robot --compositor=gnome</pre>
 * With -Drobot.run=true (after -- in the launcher), the tests run at startup.
 */
public class RobotDemo extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private static final String CSS = """
        .root {
            -fx-font-family: "Adwaita Sans", "Cantarell", "System";
            -fx-font-size: 13px;
            -fx-base: #1c2230;
            -fx-background: transparent;
            -fx-control-inner-background: #151a26;
            -fx-accent: #6366f1;
            -fx-focus-color: #818cf8;
            -fx-faint-focus-color: #818cf822;
            -fx-text-fill: #e8eef7;
        }
        .label { -fx-text-fill: #e8eef7; }
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
        .warning {
            -fx-background-color: rgba(245, 158, 11, 0.16);
            -fx-background-radius: 12;
            -fx-border-color: rgba(245, 158, 11, 0.5);
            -fx-border-radius: 12;
            -fx-padding: 10 14 10 14;
        }
        .surface {
            -fx-background-color: rgba(255, 255, 255, 0.035);
            -fx-background-radius: 12;
            -fx-border-color: rgba(255, 255, 255, 0.12);
            -fx-border-radius: 12;
        }
        .status { -fx-background-radius: 999; -fx-padding: 1 9 1 9; -fx-font-size: 10px; -fx-font-weight: bold; }
        .status.idle { -fx-background-color: rgba(255, 255, 255, 0.07); -fx-text-fill: #93a1b8; }
        .status.running { -fx-background-color: rgba(99, 102, 241, 0.35); -fx-text-fill: #c7d2fe; }
        .status.pass { -fx-background-color: rgba(34, 197, 94, 0.25); -fx-text-fill: #86efac; }
        .status.fail { -fx-background-color: rgba(244, 63, 94, 0.25); -fx-text-fill: #fda4af; }
        .detail { -fx-font-size: 11px; -fx-text-fill: #93a1b8; }
        .summary { -fx-font-size: 18px; -fx-font-weight: bold; }
        .button {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 9;
            -fx-text-fill: #e8eef7;
            -fx-padding: 6 14 6 14;
        }
        .button:hover { -fx-background-color: rgba(255, 255, 255, 0.15); }
        .button:disabled { -fx-opacity: 0.4; }
        .button.accent {
            -fx-background-color: linear-gradient(to right, #6366f1, #d946ef);
            -fx-border-color: transparent;
            -fx-text-fill: white;
            -fx-font-weight: bold;
        }
        .button.small { -fx-padding: 2 10 2 10; -fx-font-size: 11px; }
        .text-field {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.16);
            -fx-border-radius: 9;
            -fx-text-fill: #e8eef7;
        }
        .slider .track { -fx-background-color: rgba(255, 255, 255, 0.14); -fx-background-radius: 4; }
        .slider .thumb { -fx-background-color: white; }
        .context-menu {
            -fx-background-color: rgba(18, 22, 34, 0.96);
            -fx-background-radius: 10;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 10;
        }
        .list-view { -fx-background-color: transparent; -fx-border-color: transparent; }
        .list-view .list-cell { -fx-background-color: transparent; -fx-text-fill: #e8eef7; }
        .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-bar { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: rgba(255, 255, 255, 0.25); -fx-background-radius: 6; }
        .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 0; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-shape: " "; -fx-padding: 0; }
        """;

    private static final String STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));

    private static final long TIMEOUT_MS = 3000;
    private static final double COLOR_TOLERANCE = 0.04;
    private static final String TYPED_TEXT = "Hello Robot 42";
    private static final int LOG_SIZE = 300;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final Color[] SWATCHES = {
        Color.web("#ff0000"), Color.web("#00ff00"), Color.web("#0000ff"), Color.web("#ffffff"), Color.web("#f59e0b")
    };

    private final boolean testSession = "gnome".equals(System.getProperty("glass.gtk4.testSession"));
    private boolean backdropSupported;
    private Robot robot;
    private Stage stage;

    // Playground state, written by event handlers on the FX thread
    private final Pane pad = new Pane();
    private final Line crossH = new Line();
    private final Line crossV = new Line();
    private final Label padInfo = new Label("Pointer pad");
    private final Button clickButton = new Button("Click me");
    private final TextField textField = new TextField();
    private final Slider slider = new Slider(0, 100, 0);
    private final ScrollPane scrollPane = new ScrollPane();
    private final List<Rectangle> swatches = new ArrayList<>();
    private final ImageView captureView = new ImageView();
    private final Label captureInfo = new Label("No capture yet");
    private final Label pointerPill = new Label("Robot pointer: –");
    private Point2D lastMove;
    private int clicks;
    private int maxClickCount;
    private boolean contextMenuShown;
    private int scrollEvents;
    private final List<KeyCode> pressedKeys = new ArrayList<>();

    // Tests
    private record Test(String name, String description, Step body) {}

    @FunctionalInterface
    private interface Step {
        String run() throws Exception;
    }

    private final List<Test> tests = new ArrayList<>();
    private final List<Label> statusLabels = new ArrayList<>();
    private final List<Label> detailLabels = new ArrayList<>();
    private final List<Button> runButtons = new ArrayList<>();
    private final Label summary = new Label("Not run");
    private final Button runAll = new Button("Run all tests");
    private final AtomicBoolean running = new AtomicBoolean();
    private final ObservableList<String> log = FXCollections.observableArrayList();
    private final ListView<String> logView = new ListView<>(log);

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        backdropSupported = Boolean.getBoolean("glass.gtk4.backdrop.active");
        robot = new Robot();
        defineTests();
        stage.initStyle(StageStyle.EXTENDED);

        BorderPane body = new BorderPane();
        body.setTop(header());
        VBox left = column(playgroundCard(), captureCard());
        VBox right = column(testsCard(), logCard());
        HBox columns = new HBox(14, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setPrefWidth(1);
        right.setPrefWidth(1);
        columns.setPadding(new Insets(4, 18, 18, 18));
        VBox center = new VBox(columns);
        VBox.setVgrow(columns, Priority.ALWAYS);
        if (!testSession) {
            Label warning = new Label("Robot input is generated only in the GNOME test session: run "
                    + "./launcher.sh --apps=robot --compositor=gnome (it passes -Dglass.gtk4.testSession=gnome). "
                    + "Here the input tests fail with UnsupportedOperationException; the capture tests still run.");
            warning.setWrapText(true);
            warning.getStyleClass().add("warning");
            VBox.setMargin(warning, new Insets(0, 18, 8, 18));
            center.getChildren().addFirst(warning);
        }
        body.setCenter(center);

        StackPane root = new StackPane(backgroundLayer(), body);
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, Math.min(1200, screen.getWidth() * 0.9), Math.min(880, screen.getHeight() * 0.92),
                Color.TRANSPARENT);
        scene.getStylesheets().add(STYLESHEET);
        scene.getPreferences().setColorScheme(ColorScheme.DARK);
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> pressedKeys.add(e.getCode()));
        stage.setScene(scene);
        stage.setTitle("Robot Lab");
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.show();

        Timeline pointer = new Timeline(new KeyFrame(Duration.millis(200), e -> {
            Point2D p = robot.getMousePosition();
            pointerPill.setText(String.format(Locale.ROOT, "Robot pointer: %.0f, %.0f", p.getX(), p.getY()));
        }));
        pointer.setCycleCount(Timeline.INDEFINITE);
        pointer.play();

        if (Boolean.getBoolean("robot.run")) {
            Platform.runLater(() -> runTests(tests));
        }
    }

    private Region backgroundLayer() {
        Region fill = new Region();
        if (backdropSupported) {
            fill.setBackground(new Background(new BackgroundFill(Color.color(0.02, 0.03, 0.06, 0.22), null, null)));
        } else {
            fill.setBackground(new Background(new BackgroundFill(new RadialGradient(0, 0, 0.2, 0.1, 1.2, true,
                    CycleMethod.NO_CYCLE, new Stop(0, Color.web("#1e1b4b")), new Stop(0.6, Color.web("#0b1120")),
                    new Stop(1, Color.web("#050811"))), null, null)));
        }
        return fill;
    }

    private static VBox column(Node... cards) {
        VBox box = new VBox(14, cards);
        box.setFillWidth(true);
        box.setMinWidth(0);
        return box;
    }

    private Node header() {
        Circle logo = new Circle(10, new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#6366f1")), new Stop(1, Color.web("#d946ef"))));
        logo.setEffect(new Glow(0.6));
        Label brand = new Label("Robot Lab");
        brand.getStyleClass().add("brand");
        Label sub = muted("pointer · buttons · wheel · keyboard · pixels · capture");
        HBox left = new HBox(10, logo, brand, sub);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 18));

        HBox right = new HBox(10, pill(pointerPill),
                pill(new Label(testSession ? "Test session: GNOME" : "No test session")),
                pill(new Label(System.getProperty("glass.platform", "default") + " glass")));
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setPadding(new Insets(0, 10, 0, 0));

        HeaderBar.setDragType(left, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar header = new HeaderBar(left, null, right);
        header.setBackground(null);
        header.setMinHeight(52);
        HeaderBar.setAlignment(left, Pos.CENTER_LEFT);
        return header;
    }

    private static HBox pill(Label label) {
        HBox box = new HBox(label);
        box.getStyleClass().add("pill");
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
    // Playground: the targets of the robot

    private Node playgroundCard() {
        pad.getStyleClass().add("surface");
        pad.setPrefHeight(150);
        pad.setMinHeight(120);
        for (Line l : new Line[] {crossH, crossV}) {
            l.setStroke(Color.web("#818cf8"));
            l.setStrokeWidth(1.5);
            l.setVisible(false);
            l.setMouseTransparent(true);
        }
        padInfo.getStyleClass().add("mono");
        padInfo.relocate(10, 8);
        padInfo.setMouseTransparent(true);
        pad.getChildren().addAll(crossH, crossV, padInfo);
        pad.addEventHandler(MouseEvent.MOUSE_MOVED, e -> pointerAt(e, "moved"));
        pad.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> pointerAt(e, "dragged"));
        pad.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            maxClickCount = Math.max(maxClickCount, e.getClickCount());
            log("Pad clicked: " + e.getButton() + " count " + e.getClickCount());
        });
        pad.setOnMouseExited(e -> {
            crossH.setVisible(false);
            crossV.setVisible(false);
        });
        ContextMenu menu = new ContextMenu(new MenuItem("Context menu opened by the robot"));
        menu.setOnShown(e -> contextMenuShown = true);
        pad.setOnContextMenuRequested(e -> {
            log("Context menu requested at " + fmt(e.getX(), e.getY()));
            menu.show(pad, e.getScreenX(), e.getScreenY());
        });

        clickButton.getStyleClass().add("accent");
        clickButton.setOnAction(e -> {
            clicks++;
            clickButton.setText("Clicked " + clicks + "×");
        });
        textField.setPromptText("The robot types here");
        HBox.setHgrow(textField, Priority.ALWAYS);
        HBox row1 = new HBox(10, clickButton, textField);
        row1.setAlignment(Pos.CENTER_LEFT);

        slider.setShowTickMarks(true);
        slider.setMajorTickUnit(25);
        Label sliderValue = new Label();
        sliderValue.getStyleClass().add("mono");
        sliderValue.textProperty().bind(slider.valueProperty().asString(Locale.ROOT, "%.0f"));
        sliderValue.setMinWidth(30);
        HBox.setHgrow(slider, Priority.ALWAYS);
        HBox row2 = new HBox(10, new Label("Slider"), slider, sliderValue);
        row2.setAlignment(Pos.CENTER_LEFT);

        VBox lines = new VBox(4);
        for (int i = 1; i <= 60; i++) {
            lines.getChildren().add(new Label("Line " + i + " of the wheel target"));
        }
        scrollPane.setContent(lines);
        scrollPane.setPrefHeight(90);
        scrollPane.getStyleClass().add("surface");
        scrollPane.setPadding(new Insets(4, 10, 4, 10));
        scrollPane.addEventFilter(ScrollEvent.SCROLL, e -> scrollEvents++);

        HBox swatchRow = new HBox(8);
        for (Color c : SWATCHES) {
            Rectangle r = new Rectangle(46, 30, c);
            r.setArcWidth(8);
            r.setArcHeight(8);
            swatches.add(r);
            swatchRow.getChildren().add(r);
        }
        HBox row3 = new HBox(10, new Label("Swatches"), swatchRow);
        row3.setAlignment(Pos.CENTER_LEFT);

        return card("Playground", pad, row1, row2, scrollPane, row3);
    }

    private void pointerAt(MouseEvent e, String how) {
        lastMove = new Point2D(e.getScreenX(), e.getScreenY());
        double w = pad.getWidth(), h = pad.getHeight();
        crossH.setStartX(0);
        crossH.setEndX(w);
        crossH.setStartY(e.getY());
        crossH.setEndY(e.getY());
        crossV.setStartX(e.getX());
        crossV.setEndX(e.getX());
        crossV.setStartY(0);
        crossV.setEndY(h);
        crossH.setVisible(true);
        crossV.setVisible(true);
        padInfo.setText(String.format(Locale.ROOT, "%s at %.1f, %.1f (screen %.1f, %.1f)", how, e.getX(), e.getY(),
                e.getScreenX(), e.getScreenY()));
    }

    private Node captureCard() {
        captureView.setPreserveRatio(true);
        captureView.setFitHeight(90);
        captureView.setSmooth(false);
        captureInfo.getStyleClass().add("muted");
        captureInfo.setWrapText(true);
        Button capture = new Button("Capture the playground");
        capture.setOnAction(e -> {
            try {
                Bounds b = screenBounds(pad.getParent());
                WritableImage image = robot.getScreenCapture(null, new Rectangle2D(b.getMinX(), b.getMinY(),
                        b.getWidth(), b.getHeight()));
                captureView.setImage(image);
                captureInfo.setText(String.format(Locale.ROOT, "%.0f × %.0f pixels from %s", image.getWidth(),
                        image.getHeight(), fmt(b.getMinX(), b.getMinY())));
            } catch (RuntimeException ex) {
                captureInfo.setText("Capture failed: " + ex);
            }
        });
        return card("Screen capture", captureView, captureInfo, capture);
    }

    // ---------------------------------------------------------------------
    // Tests

    private void defineTests() {
        tests.add(new Test("Pointer move", "Moves to the center of the pad and checks the MOUSE_MOVED position",
                () -> {
                    Point2D target = fx(() -> center(pad));
                    fx(() -> lastMove = null);
                    fx(() -> robot.mouseMove(target));
                    await("a MOUSE_MOVED on the pad", () -> lastMove != null && lastMove.distance(target) < 2);
                    Point2D reported = fx(() -> robot.getMousePosition());
                    check(reported.distance(target) < 1, "getMousePosition() is " + fmt(reported));
                    return "event at " + fmt(fx(() -> lastMove)) + ", target " + fmt(target);
                }));
        tests.add(new Test("Pointer path", "Moves along a diagonal of the pad in 20 steps", () -> {
            Bounds b = fx(() -> screenBounds(pad));
            for (int i = 0; i <= 20; i++) {
                double t = i / 20.0;
                Point2D p = new Point2D(b.getMinX() + 10 + t * (b.getWidth() - 20),
                        b.getMinY() + 10 + t * (b.getHeight() - 20));
                fx(() -> robot.mouseMove(p));
                Thread.sleep(15);
            }
            Point2D end = new Point2D(b.getMaxX() - 10, b.getMaxY() - 10);
            await("the pointer at the end of the path", () -> lastMove != null && lastMove.distance(end) < 2);
            return "ended at " + fmt(fx(() -> lastMove));
        }));
        tests.add(new Test("Left click", "Clicks the button", () -> {
            int before = fx(() -> clicks);
            Point2D target = fx(() -> center(clickButton));
            fx(() -> robot.mouseMove(target));
            fx(() -> robot.mouseClick(MouseButton.PRIMARY));
            await("the button action", () -> clicks == before + 1);
            return "clicks " + before + " → " + fx(() -> clicks);
        }));
        tests.add(new Test("Double click", "Clicks the pad twice and checks the click count", () -> {
            fx(() -> maxClickCount = 0);
            fx(() -> robot.mouseMove(center(pad)));
            fx(() -> robot.mouseClick(MouseButton.PRIMARY));
            fx(() -> robot.mouseClick(MouseButton.PRIMARY));
            await("a MOUSE_CLICKED with click count 2", () -> maxClickCount >= 2);
            return "click count " + fx(() -> maxClickCount);
        }));
        tests.add(new Test("Right click", "Opens the context menu of the pad", () -> {
            fx(() -> contextMenuShown = false);
            fx(() -> robot.mouseMove(center(pad).add(40, 0)));
            fx(() -> robot.mouseClick(MouseButton.SECONDARY));
            await("the context menu", () -> contextMenuShown);
            fx(() -> robot.keyType(KeyCode.ESCAPE));
            return "context menu shown";
        }));
        tests.add(new Test("Wheel", "Scrolls the list down 3 notches", () -> {
            fx(() -> {
                scrollPane.setVvalue(0);
                scrollEvents = 0;
            });
            fx(() -> robot.mouseMove(center(scrollPane)));
            fx(() -> robot.mouseWheel(3));
            await("a scroll down", () -> scrollEvents > 0 && scrollPane.getVvalue() > 0);
            return String.format(Locale.ROOT, "%d events, vvalue %.2f", fx(() -> scrollEvents),
                    fx(() -> scrollPane.getVvalue()));
        }));
        tests.add(new Test("Drag", "Drags the slider thumb to the right end", () -> {
            fx(() -> slider.setValue(0));
            Point2D thumb = fx(() -> center(slider.lookup(".thumb")));
            Bounds track = fx(() -> screenBounds(slider));
            fx(() -> robot.mouseMove(thumb));
            fx(() -> robot.mousePress(MouseButton.PRIMARY));
            for (int i = 1; i <= 15; i++) {
                double x = thumb.getX() + (track.getMaxX() + 20 - thumb.getX()) * i / 15;
                fx(() -> robot.mouseMove(x, thumb.getY()));
                Thread.sleep(15);
            }
            fx(() -> robot.mouseRelease(MouseButton.PRIMARY));
            await("the slider at its maximum", () -> slider.getValue() > 95);
            return String.format(Locale.ROOT, "value %.0f", fx(() -> slider.getValue()));
        }));
        tests.add(new Test("Typing", "Types \"" + TYPED_TEXT + "\" with Shift for capitals", () -> {
            fx(() -> textField.clear());
            fx(() -> robot.mouseMove(center(textField)));
            fx(() -> robot.mouseClick(MouseButton.PRIMARY));
            await("the text field focused", textField::isFocused);
            for (char c : TYPED_TEXT.toCharArray()) {
                fx(() -> type(c));
            }
            await("the typed text", () -> TYPED_TEXT.equals(textField.getText()));
            return "\"" + fx(() -> textField.getText()) + "\"";
        }));
        tests.add(new Test("Shortcuts", "Selects all with Ctrl+A and deletes with Backspace", () -> {
            fx(() -> {
                textField.setText("select me");
                textField.requestFocus();
                pressedKeys.clear();
            });
            await("the text field focused", textField::isFocused);
            fx(() -> {
                robot.keyPress(KeyCode.CONTROL);
                robot.keyType(KeyCode.A);
                robot.keyRelease(KeyCode.CONTROL);
            });
            await("the text selected", () -> "select me".equals(textField.getSelectedText()));
            fx(() -> robot.keyType(KeyCode.BACK_SPACE));
            await("an empty text field", () -> textField.getText().isEmpty());
            return "keys " + fx(() -> List.copyOf(pressedKeys));
        }));
        tests.add(new Test("Pixel color", "Reads the center pixel of every swatch", () -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < SWATCHES.length; i++) {
                Rectangle r = swatches.get(i);
                Color got = fx(() -> robot.getPixelColor(center(r)));
                check(close(got, SWATCHES[i]), "swatch " + (i + 1) + " is " + hex(got) + ", expected "
                        + hex(SWATCHES[i]));
                sb.append(hex(got)).append(' ');
            }
            return sb.toString().trim();
        }));
        tests.add(new Test("Screen capture", "Captures the swatches and checks their colors in the image", () -> {
            Bounds b = fx(() -> screenBounds(swatches.getFirst().getParent()));
            WritableImage image = fx(() -> robot.getScreenCapture(null,
                    new Rectangle2D(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight()), false));
            fx(() -> {
                captureView.setImage(image);
                captureInfo.setText(String.format(Locale.ROOT, "Swatches: %.0f × %.0f pixels",
                        image.getWidth(), image.getHeight()));
            });
            double sx = image.getWidth() / b.getWidth(), sy = image.getHeight() / b.getHeight();
            for (int i = 0; i < SWATCHES.length; i++) {
                Rectangle r = swatches.get(i);
                Point2D p = fx(() -> centerIn(r, b));
                Color got = image.getPixelReader().getColor((int) (p.getX() * sx), (int) (p.getY() * sy));
                check(close(got, SWATCHES[i]), "swatch " + (i + 1) + " is " + hex(got) + " in the capture, "
                        + "expected " + hex(SWATCHES[i]));
            }
            return String.format(Locale.ROOT, "%.0f × %.0f pixels, scale %.2f", image.getWidth(),
                    image.getHeight(), sx);
        }));
    }

    private void type(char c) {
        KeyCode code = switch (c) {
            case ' ' -> KeyCode.SPACE;
            case '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> KeyCode.getKeyCode(String.valueOf(c));
            default -> KeyCode.getKeyCode(String.valueOf(Character.toUpperCase(c)));
        };
        if (code == null) {
            throw new IllegalArgumentException("Cannot type '" + c + "'");
        }
        boolean shift = Character.isUpperCase(c);
        if (shift) {
            robot.keyPress(KeyCode.SHIFT);
        }
        robot.keyType(code);
        if (shift) {
            robot.keyRelease(KeyCode.SHIFT);
        }
    }

    private Node testsCard() {
        GridPane grid = new GridPane(10, 6);
        for (int i = 0; i < tests.size(); i++) {
            Test test = tests.get(i);
            Label name = new Label(test.name());
            name.setStyle("-fx-font-weight: bold;");
            Label status = new Label("IDLE");
            status.getStyleClass().addAll("status", "idle");
            status.setMinWidth(Region.USE_PREF_SIZE);
            Label detail = new Label(test.description());
            detail.getStyleClass().add("detail");
            detail.setWrapText(true);
            Button run = new Button("Run");
            run.getStyleClass().add("small");
            run.setOnAction(e -> runTests(List.of(test)));
            statusLabels.add(status);
            detailLabels.add(detail);
            runButtons.add(run);
            GridPane.setHgrow(detail, Priority.ALWAYS);
            grid.addRow(i, status, name, detail, run);
        }
        runAll.getStyleClass().add("accent");
        runAll.setOnAction(e -> runTests(tests));
        summary.getStyleClass().add("summary");
        HBox top = new HBox(12, runAll, summary);
        top.setAlignment(Pos.CENTER_LEFT);
        Label hint = muted("Do not touch the mouse or the keyboard while the tests run. "
                + "Pass -Drobot.run=true to run them at startup.");
        return card("Tests", top, grid, hint);
    }

    private void runTests(List<Test> selected) {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        runAll.setDisable(true);
        runButtons.forEach(b -> b.setDisable(true));
        for (Test t : selected) {
            setStatus(tests.indexOf(t), "idle", "QUEUED", t.description());
        }
        summary.setText("Running…");
        stage.toFront();
        stage.requestFocus();
        Thread.ofPlatform().daemon().name("robot-tests").start(() -> {
            int passed = 0;
            long start = System.nanoTime();
            for (Test t : selected) {
                int index = tests.indexOf(t);
                fx(() -> setStatus(index, "running", "RUNNING", t.description()));
                long t0 = System.nanoTime();
                String result;
                boolean ok;
                try {
                    result = t.body().run();
                    ok = true;
                } catch (Throwable ex) {
                    result = ex instanceof AssertionError ? ex.getMessage() : ex.toString();
                    ok = false;
                }
                long ms = (System.nanoTime() - t0) / 1_000_000;
                String detail = result + " (" + ms + " ms)";
                boolean pass = ok;
                fx(() -> {
                    setStatus(index, pass ? "pass" : "fail", pass ? "PASS" : "FAIL", detail);
                    log((pass ? "PASS " : "FAIL ") + t.name() + ": " + detail);
                });
                System.out.println((pass ? "PASS " : "FAIL ") + t.name() + ": " + detail);
                if (ok) {
                    passed++;
                }
                pause(150);
            }
            long total = (System.nanoTime() - start) / 1_000_000;
            String text = passed + " of " + selected.size() + " passed in " + total + " ms";
            System.out.println(text);
            fx(() -> {
                summary.setText(text);
                summary.setTextFill(Color.web(text.startsWith(selected.size() + " ") ? "#86efac" : "#fda4af"));
                runAll.setDisable(false);
                runButtons.forEach(b -> b.setDisable(false));
                running.set(false);
            });
        });
    }

    private void setStatus(int index, String style, String text, String detail) {
        Label status = statusLabels.get(index);
        status.getStyleClass().removeAll("idle", "running", "pass", "fail");
        status.getStyleClass().add(style);
        status.setText(text);
        detailLabels.get(index).setText(detail);
    }

    // ---------------------------------------------------------------------
    // Helpers for the test thread

    private static void fx(Runnable r) {
        fx(() -> {
            r.run();
            return null;
        });
    }

    private static <T> T fx(Supplier<T> s) {
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
        } catch (java.util.concurrent.CompletionException e) {
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
    private static void await(String what, BooleanSupplier condition) {
        long deadline = System.nanoTime() + TIMEOUT_MS * 1_000_000;
        while (System.nanoTime() < deadline) {
            if (fx(condition::getAsBoolean)) {
                return;
            }
            pause(20);
        }
        throw new AssertionError("Timed out waiting for " + what);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static Bounds screenBounds(Node node) {
        Bounds b = node.localToScreen(node.getLayoutBounds());
        if (b == null) {
            throw new IllegalStateException(node + " is not showing");
        }
        return b;
    }

    private static Point2D center(Node node) {
        Bounds b = screenBounds(node);
        return new Point2D(b.getCenterX(), b.getCenterY());
    }

    /**
     * The center of a node relative to the top left corner of screen bounds.
     */
    private static Point2D centerIn(Node node, Bounds area) {
        return center(node).subtract(area.getMinX(), area.getMinY());
    }

    private static boolean close(Color a, Color b) {
        return Math.abs(a.getRed() - b.getRed()) <= COLOR_TOLERANCE
                && Math.abs(a.getGreen() - b.getGreen()) <= COLOR_TOLERANCE
                && Math.abs(a.getBlue() - b.getBlue()) <= COLOR_TOLERANCE;
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255),
                Math.round(c.getBlue() * 255));
    }

    private static String fmt(Point2D p) {
        return p == null ? "none" : fmt(p.getX(), p.getY());
    }

    private static String fmt(double x, double y) {
        return String.format(Locale.ROOT, "%.1f, %.1f", x, y);
    }

    // ---------------------------------------------------------------------
    // Log

    private Node logCard() {
        logView.setPrefHeight(160);
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
        VBox box = card("Log", logView);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private void log(String text) {
        log.add(LocalTime.now().format(CLOCK) + "  " + text);
        if (log.size() > LOG_SIZE) {
            log.remove(0, log.size() - LOG_SIZE);
        }
        logView.scrollTo(log.size() - 1);
    }
}
