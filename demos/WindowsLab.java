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
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.effect.Glow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
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
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.Duration;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A laboratory for windows: opens windows of every style, owned windows,
 * dialogs and popups, and its tests (see TestKit) check their size, focus,
 * maximized, full screen and minimized states (requested before and after
 * show), modality, minimum and maximum sizes, and the position, auto-hide and
 * screen-edge behavior of popups, with the robot where they need input.
 * <p>
 * The tests place their windows at given positions, which Wayland only
 * allows in the GNOME test session of the launcher; elsewhere the compositor
 * places them, and the robot finds them:
 * <pre>./launcher.sh --apps=tests --compositor=gnome --headless -- -Dtests.labs=windows</pre>
 */
public class WindowsLab {

    private static final String CSS = """
        .root {
            -fx-font-family: "Adwaita Sans", "Cantarell", "System";
            -fx-font-size: 13px;
            -fx-base: #1c2230;
            -fx-background: transparent;
            -fx-control-inner-background: #151a26;
            -fx-accent: #ec4899;
            -fx-focus-color: #f472b6;
            -fx-faint-focus-color: #f472b622;
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

    private static final double W = 360;
    private static final double H = 240;
    private static final double TOLERANCE = 2;
    private static final int LOG_SIZE = 300;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final Color[] COLORS = {
        Color.web("#ec4899"), Color.web("#6366f1"), Color.web("#14b8a6"), Color.web("#f59e0b"),
        Color.web("#22c55e"), Color.web("#0ea5e9"), Color.web("#d946ef")
    };

    // UNIFIED exists on macOS only
    private static final List<StageStyle> STYLES = java.util.Arrays.stream(StageStyle.values())
            .filter(s -> s != StageStyle.UNIFIED).toList();

    final TestKit.Suite suite = new TestKit.Suite("Windows");
    private boolean backdropSupported;
    private Stage stage;
    private final ObservableList<String> log = FXCollections.observableArrayList();
    private final ListView<String> logView = new ListView<>(log);
    private final List<Probe> probes = new ArrayList<>();
    // The stages of the stage tests, which close after each test
    private final List<Stage> testStages = new ArrayList<>();
    private static final double ROOT_SIZE = 360;
    private static final double BOUNDS_DELTA = 50;
    private String focusViolation;

    /**
     * A window that the tests open: a colored scene with a button that counts
     * its clicks.
     */
    private final class Probe {
        final Stage stage;
        final Scene scene;
        final Button target = new Button("Target");
        final StackPane root;
        int clicks;

        Probe(StageStyle style, String name) {
            stage = new Stage(style);
            Color color = COLORS[probes.size() % COLORS.length];
            target.setOnAction(e -> {
                clicks++;
                target.setText("Clicked " + clicks + "×");
            });
            Label title = new Label(name);
            title.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: white;");
            VBox content = new VBox(12, title, target);
            content.setAlignment(Pos.CENTER);
            root = new StackPane(content);
            boolean transparent = style == StageStyle.TRANSPARENT;
            // A transparent window keeps transparent corners, around a rounded body
            root.setBackground(new Background(new BackgroundFill(color.deriveColor(0, 1, 0.6, 1),
                    transparent ? new CornerRadii(28) : null, transparent ? new Insets(10) : null)));
            scene = new Scene(root, W, H, transparent ? Color.TRANSPARENT : Color.BLACK);
            scene.getStylesheets().add(STYLESHEET);
            stage.setScene(scene);
            stage.setTitle(name);
            int n = probes.size();
            stage.setX(140 + 40 * (n % 8));
            stage.setY(120 + 40 * (n % 8));
            stage.focusedProperty().addListener((obs, o, focused) -> {
                if (focused) {
                    checkSingleFocus(this);
                }
                log(name + (focused ? " focused" : " lost the focus"));
            });
            stage.iconifiedProperty().addListener((obs, o, n2) -> log(name + " iconified=" + n2));
            stage.maximizedProperty().addListener((obs, o, n2) -> log(name + " maximized=" + n2));
            stage.fullScreenProperty().addListener((obs, o, n2) -> {
                log(name + " fullScreen=" + n2);
                if (Boolean.getBoolean("windows.trace")) {
                    Thread.dumpStack();
                }
            });
            probes.add(this);
        }

        void show() {
            stage.show();
        }
    }

    /**
     * The lab without its header bar, which the tests launcher shows in a tab.
     */
    Region content(Stage stage) {
        this.stage = stage;
        backdropSupported = Boolean.getBoolean("glass.gtk4.backdrop.active");
        defineTests();
        suite.setBeforeRun(() -> {
            stage.toFront();
            stage.requestFocus();
        });
        suite.setAfterEach(() -> {
            closeProbes();
            closeTestStages();
        });
        VBox left = column(openCard(), logCard());
        VBox right = column(TestKit.grow(card("Tests", suite.view())));
        HBox columns = new HBox(14, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setPrefWidth(1);
        right.setPrefWidth(1.4);
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
    // Windows to open by hand

    private Node openCard() {
        FlowPane styles = new FlowPane(8, 8);
        for (StageStyle style : STYLES) {
            Button b = new Button(style.name());
            b.setOnAction(e -> new Probe(style, style.name()).show());
            styles.getChildren().add(b);
        }
        Button owned = new Button("Owned");
        owned.setOnAction(e -> {
            Probe p = new Probe(StageStyle.DECORATED, "Owned by the lab");
            p.stage.initOwner(stage);
            p.show();
        });
        Button windowModal = new Button("Window modal");
        windowModal.setOnAction(e -> {
            Probe p = new Probe(StageStyle.DECORATED, "Window modal");
            p.stage.initOwner(stage);
            p.stage.initModality(Modality.WINDOW_MODAL);
            p.show();
        });
        Button appModal = new Button("Application modal");
        appModal.setOnAction(e -> {
            Probe p = new Probe(StageStyle.DECORATED, "Application modal");
            p.stage.initModality(Modality.APPLICATION_MODAL);
            p.show();
        });
        Button alert = new Button("Alert");
        alert.setOnAction(e -> {
            Alert a = new Alert(Alert.AlertType.INFORMATION, "An alert owned by the lab");
            a.initOwner(stage);
            a.show();
        });
        Button popup = new Button("Popup");
        popup.setOnAction(e -> {
            Popup p = popupOf(180, 90, "A popup with auto-hide");
            p.setAutoHide(true);
            Bounds b = popup.localToScreen(popup.getLayoutBounds());
            p.show(stage, b.getMinX(), b.getMaxY() + 6);
        });
        FlowPane others = new FlowPane(8, 8, owned, windowModal, appModal, alert, popup);
        Button close = new Button("Close the windows");
        close.setOnAction(e -> closeProbes());
        Label hint = muted("Opens windows of each style, owned and modal windows, an alert and a popup. The tests "
                + "open their own windows at fixed positions, which Wayland only allows in the GNOME test session; "
                + "elsewhere the compositor places them.");
        return card("Open", new Label("Styles"), styles, new Label("Owners, modality and popups"), others, close,
                hint);
    }

    private static Popup popupOf(double w, double h, String text) {
        Rectangle r = new Rectangle(w, h, Color.web("#f472b6"));
        r.setArcWidth(16);
        r.setArcHeight(16);
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #1f0a18; -fx-font-weight: bold;");
        Popup p = new Popup();
        p.getContent().add(new StackPane(r, l));
        return p;
    }

    private void closeProbes() {
        for (Probe p : List.copyOf(probes)) {
            p.stage.hide();
        }
        probes.clear();
        for (Window w : List.copyOf(Window.getWindows())) {
            if (w instanceof PopupWindow pw && pw.getOwnerWindow() != null && pw.isShowing()) {
                pw.hide();
            } else if (w instanceof Stage s && s != stage && s.getOwner() == stage) {
                s.hide();
            }
        }
    }

    private void checkSingleFocus(Probe focused) {
        for (Probe p : probes) {
            if (p != focused && p.stage.isFocused()) {
                focusViolation = focused.stage.getTitle() + " and " + p.stage.getTitle() + " were focused together";
            }
        }
    }

    // ---------------------------------------------------------------------
    // Log

    private Node logCard() {
        logView.setPrefHeight(200);
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
        clear.setOnAction(e -> log.clear());
        VBox box = card("Log", logView, clear);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private void log(String text) {
        log.add(LocalTime.now().format(CLOCK) + "  " + text);
        if (log.size() > LOG_SIZE) {
            log.remove(0, log.size() - LOG_SIZE);
        }
        logView.scrollTo(log.size() - 1);
        System.out.println(text);
    }

    // ---------------------------------------------------------------------
    // Tests

    private void defineTests() {
        for (StageStyle style : STYLES) {
            suite.auto("Style " + style.name(), "Shows a " + style.name() + " window of 360 × 240 that takes the focus",
                    () -> styleTest(style));
        }
        suite.robot("Alert", "Shows an alert owned by the lab and closes it with its OK button", () -> {
            Alert alert = fx(() -> {
                Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Close this alert with OK");
                a.initOwner(stage);
                a.show();
                return a;
            });
            await("the alert", () -> alert.isShowing() && alert.getDialogPane().getScene().getWindow().isFocused());
            TestKit.click(alert.getDialogPane().lookupButton(ButtonType.OK));
            await("the alert closed", () -> !alert.isShowing());
            Optional<ButtonType> result = Optional.ofNullable(fx(() -> alert.getResult()));
            check(result.orElse(null) == ButtonType.OK, "the alert result is " + result.orElse(null));
            return "closed with OK";
        });

        suite.auto("New window takes the focus", "A second window takes the focus from the first", () -> {
            Probe a = open(StageStyle.DECORATED, "Focus A", s -> { });
            await("A focused", () -> a.stage.isFocused());
            Probe b = open(StageStyle.DECORATED, "Focus B", s -> { });
            await("B focused", () -> b.stage.isFocused() && !a.stage.isFocused());
            return "A, then B";
        });
        suite.robot("Click focuses a window", "Clicks the button of a window behind another", () -> {
            Probe a = open(StageStyle.DECORATED, "Click A", s -> s.setX(120));
            Probe b = open(StageStyle.DECORATED, "Click B", s -> s.setX(560));
            await("B focused", () -> b.stage.isFocused());
            TestKit.click(a.target);
            await("A focused", () -> a.stage.isFocused() && !b.stage.isFocused());
            await("the click on A", () -> a.clicks == 1);
            return "A focused by the click";
        });
        suite.auto("requestFocus", "Moves the focus to another window with requestFocus()", () -> {
            Probe a = open(StageStyle.DECORATED, "Request A", s -> s.setX(120));
            Probe b = open(StageStyle.DECORATED, "Request B", s -> s.setX(560));
            await("B focused", () -> b.stage.isFocused());
            fx(() -> a.stage.requestFocus());
            await("A focused", () -> a.stage.isFocused() && !b.stage.isFocused());
            fx(() -> b.stage.toFront());
            await("B focused by toFront()", () -> b.stage.isFocused());
            return "requestFocus() and toFront()";
        });
        suite.auto("Focus back to the owner", "Closing an owned window gives the focus back to its owner", () -> {
            Probe owner = open(StageStyle.DECORATED, "Owner", s -> { });
            Probe child = open(StageStyle.DECORATED, "Owned", s -> s.initOwner(owner.stage));
            await("the owned window focused", () -> child.stage.isFocused());
            fx(() -> child.stage.hide());
            await("the owner focused", () -> owner.stage.isFocused());
            return "owner focused";
        });
        suite.robot("One focused window", "Clicks three windows in turn: only one is focused at a time", () -> {
            fx(() -> focusViolation = null);
            List<Probe> ps = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                int x = 100 + 330 * i;
                ps.add(open(StageStyle.DECORATED, "One of three " + (i + 1), s -> s.setX(x)));
            }
            for (int round = 0; round < 2; round++) {
                for (Probe p : ps) {
                    TestKit.click(p.target);
                    await(p.stage.getTitle() + " focused", () -> p.stage.isFocused());
                }
            }
            String violation = fx(() -> focusViolation);
            check(violation == null, violation);
            return "6 clicks, one focused window each time";
        });

        suite.auto("Maximize after show", "Maximizes and restores a shown window", () ->
                maximizeTest(false));
        suite.auto("Maximize before show", "Shows a window maximized, then restores it", () ->
                maximizeTest(true));
        suite.auto("Full screen after show", "Enters and leaves full screen", () -> fullScreenTest(false));
        suite.auto("Full screen before show", "Shows a window in full screen, then leaves it", () ->
                fullScreenTest(true));
        suite.auto("Minimize after show", "Minimizes and restores a shown window", () -> minimizeTest(false));
        suite.auto("Minimize before show", "Shows a window minimized, then restores it", () ->
                minimizeTest(true));
        suite.auto("Full screen toggled quickly", "Enters and leaves full screen before the compositor answers",
                () -> {
                    Probe p = open(StageStyle.DECORATED, "Quick full screen", s -> { });
                    fx(() -> p.stage.setFullScreen(true));
                    TestKit.pause(30);
                    fx(() -> p.stage.setFullScreen(false));
                    TestKit.pause(1500);
                    await("out of full screen at 360 × 240", 5000, () -> !p.stage.isFullScreen()
                            && near(p.scene.getWidth(), W) && near(p.scene.getHeight(), H));
                    return "left full screen";
                });
        suite.auto("Maximized and full screen", "Full screen over a maximized window, which stays maximized", () -> {
            Probe p = open(StageStyle.DECORATED, "Maximized, full screen", s -> { });
            fx(() -> p.stage.setMaximized(true));
            await("maximized", () -> p.stage.isMaximized() && p.scene.getWidth() > W + 50);
            fx(() -> p.stage.setFullScreen(true));
            await("full screen", 5000, () -> p.stage.isFullScreen() && near(p.scene.getWidth(), screen().getWidth())
                    && near(p.scene.getHeight(), screen().getHeight()));
            fx(() -> p.stage.setFullScreen(false));
            await("out of full screen", 5000, () -> !p.stage.isFullScreen());
            await("maximized again", () -> p.stage.isMaximized() && p.scene.getWidth() > W + 50
                    && p.scene.getWidth() < screen().getWidth() + 1);
            fx(() -> p.stage.setMaximized(false));
            try {
                await("the size restored", 5000, () -> !p.stage.isMaximized() && near(p.scene.getWidth(), W)
                        && near(p.scene.getHeight(), H));
            } catch (AssertionError e) {
                throw new AssertionError(String.format(Locale.ROOT, "%s: maximized=%b, scene %.0f × %.0f",
                        e.getMessage(), fx(() -> p.stage.isMaximized()), fx(() -> p.scene.getWidth()),
                        fx(() -> p.scene.getHeight())));
            }
            return "maximized → full screen → maximized → 360 × 240";
        });

        suite.robot("Window modal", "A WINDOW_MODAL window blocks the clicks on its owner", () -> {
            Probe owner = open(StageStyle.DECORATED, "Modal owner", s -> s.setX(120));
            Probe modal = open(StageStyle.DECORATED, "Window modal", s -> {
                s.initOwner(owner.stage);
                s.initModality(Modality.WINDOW_MODAL);
                s.setX(560);
            });
            await("the modal window focused", () -> modal.stage.isFocused());
            TestKit.click(owner.target);
            TestKit.pause(400);
            check(fx(() -> owner.clicks) == 0, "the owner got the click");
            await("the modal window still focused", () -> modal.stage.isFocused());
            fx(() -> modal.stage.hide());
            await("the owner focused", () -> owner.stage.isFocused());
            TestKit.click(owner.target);
            await("the click on the owner", () -> owner.clicks == 1);
            return "blocked, then the owner works";
        });
        suite.robot("Application modal", "An APPLICATION_MODAL window blocks every other window", () -> {
            Probe a = open(StageStyle.DECORATED, "Other A", s -> s.setX(100));
            Probe b = open(StageStyle.DECORATED, "Other B", s -> s.setX(440));
            Probe modal = open(StageStyle.DECORATED, "Application modal", s -> {
                s.initModality(Modality.APPLICATION_MODAL);
                s.setX(780);
            });
            await("the modal window focused", () -> modal.stage.isFocused());
            TestKit.click(a.target);
            TestKit.click(b.target);
            TestKit.pause(400);
            check(fx(() -> a.clicks + b.clicks) == 0, "a blocked window got a click");
            fx(() -> modal.stage.hide());
            TestKit.click(a.target);
            await("the click on A", () -> a.clicks == 1);
            return "both blocked, then A works";
        });
        suite.robot("Not modal", "An owned window without modality leaves its owner usable", () -> {
            Probe owner = open(StageStyle.DECORATED, "Free owner", s -> s.setX(120));
            open(StageStyle.DECORATED, "Not modal", s -> {
                s.initOwner(owner.stage);
                s.setX(560);
            });
            TestKit.click(owner.target);
            await("the click on the owner", () -> owner.clicks == 1);
            return "the owner got the click";
        });

        suite.auto("Min width and max height", "Asks for sizes beyond the limits, which the window keeps", () ->
                limitsTest(300, -1, -1, 220));
        suite.auto("Max width and min height", "The opposite limits", () -> limitsTest(-1, 280, 300, -1));
        suite.auto("Min and max size", "Min and max on both axes", () -> limitsTest(320, 400, 220, 300));
        suite.auto("Fixed size", "Min and max equal", () -> limitsTest(330, 330, 230, 230));
        suite.robot("Resize by the compositor", "Drags the corner of the window inwards, below its min size",
                this::compositorResizeTest);

        suite.auto("Popup position", "Shows a popup below the target of a window", () -> {
            Probe p = open(StageStyle.DECORATED, "Popup owner", s -> { });
            Popup popup = fx(() -> popupOf(160, 80, "Popup"));
            Point2D at = fx(() -> {
                Bounds b = TestKit.screenBounds(p.target);
                return new Point2D(b.getMinX(), b.getMaxY() + 6);
            });
            fx(() -> popup.show(p.stage, at.getX(), at.getY()));
            await("the popup", popup::isShowing);
            Node content = popup.getContent().getFirst();
            await("the popup at its position", () -> {
                Bounds b = content.localToScreen(content.getLayoutBounds());
                return b != null && near(b.getMinX(), at.getX()) && near(b.getMinY(), at.getY());
            });
            fx(popup::hide);
            return "at " + TestKit.fmt(at);
        });
        suite.robot("Popup auto-hide", "A click outside an auto-hide popup hides it", () -> {
            Probe p = open(StageStyle.DECORATED, "Auto-hide owner", s -> { });
            Popup popup = fx(() -> {
                Popup pp = popupOf(160, 80, "Auto-hide");
                pp.setAutoHide(true);
                Bounds b = TestKit.screenBounds(p.target);
                pp.show(p.stage, b.getMinX(), b.getMaxY() + 6);
                return pp;
            });
            await("the popup", popup::isShowing);
            Point2D corner = fx(() -> {
                Bounds b = TestKit.screenBounds(p.root);
                return new Point2D(b.getMinX() + 20, b.getMinY() + 20);
            });
            TestKit.click(corner);
            await("the popup hidden", () -> !popup.isShowing());
            return "hidden by the click";
        });
        suite.auto("Popup at the screen edge", "A popup asked past the bottom right corner stays on the screen", () -> {
            Probe p = open(StageStyle.DECORATED, "Edge owner", s -> { });
            Popup popup = fx(() -> popupOf(200, 100, "Edge"));
            Rectangle2D screen = fx(this::screen);
            fx(() -> popup.show(p.stage, screen.getMaxX() - 50, screen.getMaxY() - 30));
            await("the popup", popup::isShowing);
            Node content = popup.getContent().getFirst();
            await("the popup inside the screen", () -> {
                Bounds b = content.localToScreen(content.getLayoutBounds());
                return b != null && b.getMaxX() <= screen.getMaxX() + 1 && b.getMaxY() <= screen.getMaxY() + 1;
            });
            Bounds b = fx(() -> content.localToScreen(content.getLayoutBounds()));
            fx(popup::hide);
            return String.format(Locale.ROOT, "moved to %.0f, %.0f", b.getMinX(), b.getMinY());
        });
        suite.robot("Context menu", "Opens a context menu with a right click and picks its item", () -> {
            Probe p = open(StageStyle.DECORATED, "Context menu owner", s -> { });
            String[] picked = {null};
            ContextMenu menu = fx(() -> {
                MenuItem pick = new MenuItem("Pick me");
                pick.setId("pick");
                pick.setOnAction(e -> picked[0] = "Pick me");
                ContextMenu m = new ContextMenu(pick, new MenuItem("Other"));
                p.root.setOnContextMenuRequested(e -> m.show(p.root, e.getScreenX(), e.getScreenY()));
                return m;
            });
            Point2D at = fx(() -> TestKit.center(p.root).add(0, 70));
            TestKit.move(at);
            TestKit.pause(100);
            fx(() -> TestKit.robot().mouseClick(MouseButton.SECONDARY));
            await("the context menu", menu::isShowing);
            Node item = awaitNode(menu, "#pick");
            TestKit.click(item);
            await("the item action", () -> picked[0] != null);
            await("the menu hidden", () -> !menu.isShowing());
            return "picked \"" + picked[0] + "\"";
        });
        suite.robot("Combo box", "Opens the list of a combo box and picks the third item", () -> {
            Probe p = open(StageStyle.DECORATED, "Combo box owner", s -> { });
            ComboBox<String> combo = fx(() -> {
                ComboBox<String> c = new ComboBox<>(FXCollections.observableArrayList("One", "Two", "Three", "Four"));
                c.setValue("One");
                ((VBox) p.target.getParent()).getChildren().add(c);
                return c;
            });
            TestKit.pause(200);
            TestKit.click(combo);
            await("the list", combo::isShowing);
            ListView<?> list = fx(() -> (ListView<?>) ((ComboBoxListViewSkin<?>) combo.getSkin()).getPopupContent());
            Node cell = awaitCell(list, "Three");
            TestKit.click(cell);
            await("the value Three", () -> "Three".equals(combo.getValue()));
            await("the list hidden", () -> !combo.isShowing());
            return "Three";
        });
        suite.robot("Tooltip", "Hovers a button with a tooltip until it shows, then leaves it", () -> {
            Probe p = open(StageStyle.DECORATED, "Tooltip owner", s -> { });
            Tooltip tip = fx(() -> {
                Tooltip t = new Tooltip("A tooltip");
                t.setShowDelay(Duration.millis(300));
                p.target.setTooltip(t);
                return t;
            });
            Point2D c = fx(() -> TestKit.center(p.target));
            TestKit.move(c.add(-3, 0));
            TestKit.pause(50);
            TestKit.move(c);
            await("the tooltip", tip::isShowing);
            Point2D tipAt = fx(() -> new Point2D(tip.getX(), tip.getY()));
            TestKit.move(fx(() -> {
                Bounds b = TestKit.screenBounds(p.root);
                return new Point2D(b.getMinX() + 15, b.getMinY() + 15);
            }));
            await("the tooltip hidden", () -> !tip.isShowing());
            return "shown at " + TestKit.fmt(tipAt);
        });
        suite.robot("Submenu", "Opens a menu of a menu bar, then its submenu, and picks an item", () -> {
            Probe p = open(StageStyle.DECORATED, "Menu owner", s -> { });
            String[] picked = {null};
            Menu file = fx(() -> {
                MenuItem deep = new MenuItem("Deep item");
                deep.setId("deep");
                deep.setOnAction(e -> picked[0] = "Deep item");
                Menu more = new Menu("More", null, deep, new MenuItem("Other"));
                more.setId("more");
                Menu f = new Menu("File", null, new MenuItem("New"), more);
                MenuBar bar = new MenuBar(f);
                ((VBox) p.target.getParent()).getChildren().addFirst(bar);
                return f;
            });
            TestKit.pause(200);
            Node fileNode = fx(() -> p.root.lookupAll(".menu").stream()
                    .filter(n -> n.isVisible()).findFirst().orElseThrow());
            TestKit.click(fileNode);
            await("the menu", file::isShowing);
            Node moreNode = awaitNode(null, "#more");
            Point2D m = fx(() -> TestKit.center(moreNode));
            TestKit.move(m.add(-6, 0));
            TestKit.pause(80);
            TestKit.move(m);
            Menu more = (Menu) file.getItems().get(1);
            await("the submenu", more::isShowing);
            Node deep = awaitNode(null, "#deep");
            Point2D d = fx(() -> TestKit.center(deep));
            // Along the submenu row first, then to the item, so the submenu stays open
            TestKit.move(new Point2D(d.getX(), m.getY()));
            TestKit.pause(80);
            TestKit.click(d);
            await("the item action", () -> picked[0] != null);
            await("the menus hidden", () -> !file.isShowing());
            return "picked \"" + picked[0] + "\"";
        });
        // After the stage system tests of OpenJFX (tests/system)
        suite.auto("Size to scene", "sizeToScene() before show fits the stage to a root of 360 × 360 (SizeToSceneTest)",
                () -> sizeToSceneTest(s -> { }, false));
        suite.auto("Size to scene, maximized", "A maximized stage stays maximized through sizeToScene() before show "
                + "(SizeToSceneTest)", () -> sizeToSceneTest(s -> s.setMaximized(true), true));
        suite.auto("Size to scene, full screen", "A full screen stage stays full screen through sizeToScene() before "
                + "show (SizeToSceneTest)", () -> sizeToSceneTest(s -> s.setFullScreen(true), true));
        suite.auto("New scene sizes", "20 stages, resizable or not, get the size of their new scenes (NewSceneSizeTest)",
                this::newSceneSizeTest);
        for (StageStyle style : List.of(StageStyle.UNDECORATED, StageStyle.TRANSPARENT, StageStyle.EXTENDED)) {
            suite.auto("Iconify " + style.name(), "Iconifies and restores a " + style.name() + " stage (IconifyTest)",
                    () -> iconifyTest(style));
        }
        suite.auto("Draws after deiconify", "A fill changed while the stage is iconified shows once it is restored "
                + "(DrawAfterDeiconifyTest)", this::drawAfterDeiconifyTest);
        suite.auto("Child stays in place", "An owned stage keeps its position when its owner is iconified and "
                + "restored (DeiconifiedWithChildTest)", this::childStaysTest);
        suite.auto("Closing the owner", "Closing a stage closes its owned stages, and theirs (StageOwnershipTest)",
                () -> ownershipTest(0));
        suite.auto("Closing a middle stage", "Closing the middle stage of a chain closes its descendants only "
                + "(StageOwnershipTest)", () -> ownershipTest(1));
        suite.auto("Modal child of a maximized owner", "An application modal child leaves its owner maximized and "
                + "takes the focus (StageOwnershipTest)", () -> modalChildTest(false));
        suite.auto("Modal child of a full screen owner", "An application modal child leaves its owner in full screen "
                + "and takes the focus (StageOwnershipTest)", () -> modalChildTest(true));
        suite.auto("Scene change keeps iconified", "Setting a new scene on an iconified stage does not restore it "
                + "(SceneChangeShouldNotFocusStageTest)", this::sceneChangeIconifiedTest);
        suite.robot("New scene takes input", "A scene set on a showing stage lays out and takes clicks "
                + "(SetSceneScalingTest)", this::newSceneInputTest);
        suite.robot("Key press closes the stage", "Escape closes the stage from its KEY_PRESSED handler, without a "
                + "KEY_TYPED afterwards (KeyEventClosesStageTest)", this::keyClosesStageTest);
        suite.auto("Nested event loops", "Three nested event loops return their values in order (NestedEventLoopTest)",
                this::nestedLoopsTest);
        suite.auto("showAndWait", "Modal stages shown with showAndWait() inside one another return in order "
                + "(ShowAndWaitTest)", this::showAndWaitTest);
    }

    private Rectangle2D screen() {
        return Screen.getPrimary().getBounds();
    }

    private Rectangle2D visualBounds() {
        return Screen.getPrimary().getVisualBounds();
    }

    /**
     * Opens a window and waits until it is showing at its size.
     */
    private Probe open(StageStyle style, String name, Consumer<Stage> setup) {
        Probe p = fx(() -> {
            Probe probe = new Probe(style, name);
            setup.accept(probe.stage);
            probe.show();
            return probe;
        });
        await(name + " shown", 5000, () -> p.stage.isShowing() && p.scene.getWidth() > 0);
        TestKit.pause(300);
        return p;
    }

    private String styleTest(StageStyle style) {
        Probe p = open(style, style.name(), s -> { });
        await("the scene at 360 × 240", () -> near(p.scene.getWidth(), W) && near(p.scene.getHeight(), H));
        await("the focus", () -> p.stage.isFocused());
        double sw = fx(() -> p.stage.getWidth()), sh = fx(() -> p.stage.getHeight());
        check(sw >= W - TOLERANCE && sh >= H - TOLERANCE, String.format(Locale.ROOT,
                "the window is %.0f × %.0f, smaller than its scene", sw, sh));
        String extra = "";
        if (style == StageStyle.TRANSPARENT && "Gtk4".equalsIgnoreCase(System.getProperty("glass.platform"))) {
            // The robot of the GTK 4 glass reads the frames of the windows, with their alpha
            TestKit.pause(300);
            Color corner = fx(() -> {
                Bounds b = TestKit.screenBounds(p.root);
                return TestKit.robot().getPixelColor(b.getMinX() + 2, b.getMinY() + 2);
            });
            Color body = fx(() -> TestKit.robot().getPixelColor(TestKit.center(p.root).add(0, -60)));
            check(corner.getOpacity() < 0.1, "the transparent corner has opacity " + corner.getOpacity());
            check(body.getOpacity() > 0.9, "the body has opacity " + body.getOpacity());
            extra = ", transparent corners";
        }
        fx(() -> p.stage.hide());
        await("the window hidden", () -> !p.stage.isShowing());
        return String.format(Locale.ROOT, "scene %.0f × %.0f, window %.0f × %.0f%s", W, H, sw, sh, extra);
    }

    private String maximizeTest(boolean before) {
        Probe p = open(StageStyle.DECORATED, before ? "Maximized at show" : "Maximize", s -> {
            if (before) {
                s.setMaximized(true);
            }
        });
        if (!before) {
            fx(() -> p.stage.setMaximized(true));
        }
        Rectangle2D area = visualBounds();
        await("maximized to the work area", 5000, () -> p.stage.isMaximized()
                && p.scene.getWidth() > area.getWidth() * 0.9 && p.scene.getHeight() > area.getHeight() * 0.8);
        String max = String.format(Locale.ROOT, "%.0f × %.0f", fx(() -> p.scene.getWidth()),
                fx(() -> p.scene.getHeight()));
        fx(() -> p.stage.setMaximized(false));
        await("restored to 360 × 240", 5000, () -> !p.stage.isMaximized() && near(p.scene.getWidth(), W)
                && near(p.scene.getHeight(), H));
        return "maximized " + max + ", restored 360 × 240";
    }

    private String fullScreenTest(boolean before) {
        Probe p = open(StageStyle.DECORATED, before ? "Full screen at show" : "Full screen", s -> {
            if (before) {
                s.setFullScreen(true);
            }
        });
        if (!before) {
            fx(() -> p.stage.setFullScreen(true));
        }
        Rectangle2D screen = screen();
        await("full screen", 5000, () -> p.stage.isFullScreen() && near(p.scene.getWidth(), screen.getWidth())
                && near(p.scene.getHeight(), screen.getHeight()));
        fx(() -> p.stage.setFullScreen(false));
        await("restored to 360 × 240", 5000, () -> !p.stage.isFullScreen() && near(p.scene.getWidth(), W)
                && near(p.scene.getHeight(), H));
        return String.format(Locale.ROOT, "full screen %.0f × %.0f, restored 360 × 240", screen.getWidth(),
                screen.getHeight());
    }

    private String minimizeTest(boolean before) {
        // Another window, to take the focus while this one is minimized
        open(StageStyle.DECORATED, "Behind", s -> s.setX(560));
        Probe p = open(StageStyle.DECORATED, before ? "Minimized at show" : "Minimize", s -> {
            if (before) {
                s.setIconified(true);
            }
        });
        if (!before) {
            await("the window focused", () -> p.stage.isFocused());
            fx(() -> p.stage.setIconified(true));
        }
        await("minimized", () -> p.stage.isIconified());
        await("the focus on another window", () -> !p.stage.isFocused());
        fx(() -> p.stage.setIconified(false));
        await("restored", 5000, () -> !p.stage.isIconified() && p.stage.isFocused());
        check(fx(() -> near(p.scene.getWidth(), W) && near(p.scene.getHeight(), H)), "the size changed");
        return "minimized, restored and focused";
    }

    /**
     * Sets limits (-1 for none) and asks for sizes beyond them on both axes.
     */
    private String limitsTest(double minW, double maxW, double minH, double maxH) {
        Probe p = open(StageStyle.DECORATED, "Limits", s -> {
            if (minW > 0) {
                s.setMinWidth(minW);
            }
            if (maxW > 0) {
                s.setMaxWidth(maxW);
            }
            if (minH > 0) {
                s.setMinHeight(minH);
            }
            if (maxH > 0) {
                s.setMaxHeight(maxH);
            }
        });
        Supplier<String> size = () -> String.format(Locale.ROOT, "%.0f × %.0f", p.stage.getWidth(),
                p.stage.getHeight());
        await("the window within its limits", () -> within(p.stage, minW, maxW, minH, maxH));
        StringBuilder sb = new StringBuilder("shown " + fx(size));
        double[][] asks = {{100, 100}, {900, 700}, {minW > 0 ? minW - 40 : 150, maxH > 0 ? maxH + 120 : 600},
            {maxW > 0 ? maxW + 150 : 800, minH > 0 ? minH - 60 : 120}};
        for (double[] ask : asks) {
            fx(() -> {
                p.stage.setWidth(ask[0]);
                p.stage.setHeight(ask[1]);
            });
            TestKit.pause(300);
            await(String.format(Locale.ROOT, "the window within its limits after asking %.0f × %.0f", ask[0], ask[1]),
                    () -> within(p.stage, minW, maxW, minH, maxH));
            sb.append(String.format(Locale.ROOT, ", %.0f × %.0f → %s", ask[0], ask[1], fx(size)));
        }
        return sb.toString();
    }

    private static boolean within(Stage s, double minW, double maxW, double minH, double maxH) {
        double w = s.getWidth(), h = s.getHeight();
        return (minW < 0 || w >= minW - TOLERANCE) && (maxW < 0 || w <= maxW + TOLERANCE)
                && (minH < 0 || h >= minH - TOLERANCE) && (maxH < 0 || h <= maxH + TOLERANCE);
    }

    private String compositorResizeTest() {
        double minW = 300, maxW = 460, minH = 200, maxH = 300;
        Probe p = open(StageStyle.DECORATED, "Resize me", s -> {
            s.setMinWidth(minW);
            s.setMaxWidth(maxW);
            s.setMinHeight(minH);
            s.setMaxHeight(maxH);
        });
        await("the window focused", () -> p.stage.isFocused());
        double w0 = fx(() -> p.stage.getWidth()), h0 = fx(() -> p.stage.getHeight());
        // The resize border of the window is just outside its bottom right corner:
        // drag it inwards, past the min size. GTK tells the compositor the min
        // size, but has no way to tell it the max size, which only programmatic
        // resizes keep
        Point2D corner = fx(() -> new Point2D(p.stage.getX() + p.stage.getWidth() + 3,
                p.stage.getY() + p.stage.getHeight() + 3));
        TestKit.drag(corner, corner.add(-250, -200));
        TestKit.pause(500);
        double w1 = fx(() -> p.stage.getWidth()), h1 = fx(() -> p.stage.getHeight());
        if (Math.abs(w1 - w0) < 1 && Math.abs(h1 - h0) < 1) {
            TestKit.skip("The drag of the corner did not resize the window");
        }
        check(fx(() -> within(p.stage, minW, maxW, minH, maxH)), String.format(Locale.ROOT,
                "the drag made the window %.0f × %.0f, out of %.0f-%.0f × %.0f-%.0f", w1, h1, minW, maxW, minH, maxH));
        check(w1 < w0 - 20, String.format(Locale.ROOT, "the width did not shrink: %.0f → %.0f", w0, w1));
        return String.format(Locale.ROOT, "%.0f × %.0f → %.0f × %.0f, within %.0f-%.0f × %.0f-%.0f", w0, h0, w1, h1,
                minW, maxW, minH, maxH);
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) <= TOLERANCE;
    }

    /**
     * Waits for a node of a popup: of a context menu, or of any popup when the
     * menu is null.
     */
    private static Node awaitNode(ContextMenu menu, String selector) {
        Node[] found = {null};
        await("the node " + selector, () -> {
            List<Window> windows = menu != null ? List.of(menu) : List.copyOf(Window.getWindows());
            for (Window w : windows) {
                if (w instanceof PopupWindow && w.isShowing() && w.getScene() != null) {
                    Node n = w.getScene().getRoot().lookup(selector);
                    if (n != null && n.localToScreen(n.getLayoutBounds()) != null) {
                        found[0] = n;
                        return true;
                    }
                }
            }
            return false;
        });
        TestKit.pause(150);
        return found[0];
    }

    private static Node awaitCell(ListView<?> list, String text) {
        Node[] found = {null};
        await("the cell " + text, () -> {
            for (Node n : list.lookupAll(".list-cell")) {
                if (n instanceof ListCell<?> c && text.equals(c.getText())) {
                    found[0] = n;
                    return true;
                }
            }
            return false;
        });
        TestKit.pause(150);
        return found[0];
    }

    private static void fx(Runnable r) {
        TestKit.fx(r);
    }

    private static <T> T fx(Supplier<T> s) {
        return TestKit.fx(s);
    }

    private static void await(String what, BooleanSupplier condition) {
        TestKit.await(what, condition);
    }

    private static void await(String what, long ms, BooleanSupplier condition) {
        TestKit.await(what, ms, condition);
    }

    private static void check(boolean condition, String message) {
        TestKit.check(condition, message);
    }

    // ---------------------------------------------------------------------
    // Stage tests, after the stage system tests of OpenJFX (tests/system)

    private void closeTestStages() {
        for (Stage s : List.copyOf(testStages)) {
            s.hide();
        }
        testStages.clear();
    }

    /** A stage of the tests, on the FX thread: colored, of 300 × 200 unless its scene changes. */
    private Stage testStage(StageStyle style, String title) {
        Stage s = new Stage(style);
        s.setTitle(title);
        StackPane root = new StackPane(new Label(title));
        root.setStyle("-fx-background-color: #1e293b;");
        s.setScene(new Scene(root, 300, 200));
        testStages.add(s);
        return s;
    }

    private static boolean near(double a, double b, double tolerance) {
        return Math.abs(a - b) <= tolerance;
    }

    private String sizeToSceneTest(Consumer<Stage> setup, boolean fills) {
        Stage s = TestKit.fx(() -> {
            Stage stage = testStage(StageStyle.DECORATED, "Size to scene");
            Button root = new Button();
            root.setMinSize(ROOT_SIZE, ROOT_SIZE);
            stage.setScene(new Scene(root));
            setup.accept(stage);
            stage.sizeToScene();
            stage.show();
            return stage;
        });
        TestKit.await("the stage shown", 5000, () -> s.isShowing());
        if (fills) {
            boolean fullScreen = TestKit.fx(() -> s.isFullScreen());
            Rectangle2D area = TestKit.fx(() -> fullScreen ? Screen.getPrimary().getBounds()
                    : Screen.getPrimary().getVisualBounds());
            Rectangle2D screen = TestKit.fx(() -> Screen.getPrimary().getBounds());
            TestKit.await("the stage filling the screen", 5000, () -> s.getWidth() >= area.getWidth() - BOUNDS_DELTA
                    && s.getHeight() >= area.getHeight() - BOUNDS_DELTA
                    && s.getWidth() <= screen.getWidth() + BOUNDS_DELTA
                    && s.getHeight() <= screen.getHeight() + BOUNDS_DELTA);
            TestKit.check(TestKit.fx(() -> s.isMaximized() || s.isFullScreen()), "the stage left its state");
        } else {
            TestKit.await("the stage fitting its root", 5000, () -> s.getWidth() >= ROOT_SIZE
                    && s.getHeight() >= ROOT_SIZE && s.getWidth() <= ROOT_SIZE + BOUNDS_DELTA
                    && s.getHeight() <= ROOT_SIZE + BOUNDS_DELTA);
        }
        return String.format(Locale.ROOT, "%.0f × %.0f", TestKit.fx(() -> s.getWidth()), TestKit.fx(() -> s.getHeight()));
    }

    private String newSceneSizeTest() {
        int count = 20;
        List<Stage> stages = TestKit.fx(() -> {
            List<Stage> list = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Stage s = testStage(StageStyle.DECORATED, "Scene " + i);
                s.setResizable(i % 2 == 0);
                s.setScene(new Scene(new VBox(), 300 - i * 5, 200 - i * 5));
                s.show();
                list.add(s);
            }
            return list;
        });
        for (int i = 0; i < count; i++) {
            Stage s = stages.get(i);
            int n = i;
            TestKit.await("the scene of stage " + i, 5000, () -> {
                double sx = s.getOutputScaleX(), sy = s.getOutputScaleY();
                double w = Math.ceil((300 - n * 5) * sx) / sx, h = Math.ceil((200 - n * 5) * sy) / sy;
                return s.isShowing() && near(s.getScene().getWidth(), w, 0.1) && near(s.getScene().getHeight(), h, 0.1);
            });
        }
        return count + " stages, 300 × 200 down to " + (300 - (count - 1) * 5) + " × " + (200 - (count - 1) * 5);
    }

    // Outside the Basic Multilingual Plane, with a skin tone modifier, a
    // variation selector, a flag and ZWJ sequences
    private String iconifyTest(StageStyle style) {
        Stage s = TestKit.fx(() -> {
            Stage stage = testStage(style, "Iconify " + style.name());
            stage.show();
            return stage;
        });
        TestKit.await("the focus", 5000, () -> s.isFocused());
        TestKit.fx(() -> s.setIconified(true));
        TestKit.await("the stage iconified", 5000, () -> s.isIconified());
        TestKit.pause(300);
        TestKit.fx(() -> s.setIconified(false));
        TestKit.await("the stage restored", 5000, () -> !s.isIconified() && s.isShowing());
        return "iconified and restored";
    }

    private Color centerColor(Stage s) {
        return TestKit.fx(() -> TestKit.robot().getPixelColor(TestKit.center(s.getScene().getRoot())));
    }

    private static boolean sameColor(Color a, Color b) {
        return Math.abs(a.getRed() - b.getRed()) < 0.07 && Math.abs(a.getGreen() - b.getGreen()) < 0.07
                && Math.abs(a.getBlue() - b.getBlue()) < 0.07;
    }

    private void awaitColor(Stage s, Color expected, String name) {
        Color[] seen = {null};
        try {
            TestKit.await("a " + name + " frame", 5000, () -> sameColor(seen[0] = centerColor(s), expected));
        } catch (AssertionError e) {
            throw new AssertionError(e.getMessage() + ", the center is " + seen[0]);
        }
    }

    private String drawAfterDeiconifyTest() {
        Stage s = TestKit.fx(() -> {
            Stage stage = testStage(StageStyle.DECORATED, "Deiconify");
            javafx.scene.layout.Pane root = new javafx.scene.layout.Pane();
            // The fill of the scene, under the background Modena gives the root
            root.setStyle("-fx-background-color: transparent;");
            Scene scene = new Scene(root, 300, 300);
            scene.setFill(Color.LIME);
            stage.setScene(scene);
            stage.show();
            return stage;
        });
        TestKit.await("the stage shown", 5000, () -> s.isShowing() && s.getScene().getWidth() > 0);
        awaitColor(s, Color.LIME, "lime");
        TestKit.fx(() -> s.setIconified(true));
        TestKit.await("the stage iconified", 5000, () -> s.isIconified());
        TestKit.fx(() -> s.getScene().setFill(Color.HOTPINK));
        TestKit.pause(300);
        TestKit.fx(() -> s.setIconified(false));
        TestKit.await("the stage restored", 5000, () -> !s.isIconified());
        awaitColor(s, Color.HOTPINK, "hot pink");
        return "lime, then hot pink after the restore";
    }

    private String childStaysTest() {
        Stage[] pair = TestKit.fx(() -> {
            Stage owner = testStage(StageStyle.DECORATED, "Owner");
            owner.show();
            Stage child = testStage(StageStyle.DECORATED, "Child");
            child.initOwner(owner);
            child.setScene(new Scene(new VBox(), 100, 100));
            child.show();
            return new Stage[] {owner, child};
        });
        TestKit.await("the stages shown", 5000, () -> pair[0].isShowing() && pair[1].isShowing());
        TestKit.pause(300);
        double x = TestKit.fx(() -> pair[1].getX()), y = TestKit.fx(() -> pair[1].getY());
        TestKit.fx(() -> pair[0].setIconified(true));
        TestKit.await("the owner iconified", 5000, () -> pair[0].isIconified());
        TestKit.pause(300);
        TestKit.fx(() -> pair[0].setIconified(false));
        TestKit.await("the owner restored", 5000, () -> !pair[0].isIconified());
        TestKit.pause(300);
        double nx = TestKit.fx(() -> pair[1].getX()), ny = TestKit.fx(() -> pair[1].getY());
        TestKit.check(near(x, nx, 0.1) && near(y, ny, 0.1), String.format(Locale.ROOT,
                "the child moved from %.0f, %.0f to %.0f, %.0f", x, y, nx, ny));
        return String.format(Locale.ROOT, "the child stayed at %.0f, %.0f", x, y);
    }

    private String ownershipTest(int closed) {
        Stage[] chain = TestKit.fx(() -> {
            Stage[] stages = new Stage[3];
            for (int i = 0; i < 3; i++) {
                stages[i] = testStage(StageStyle.DECORATED, "Stage " + (i + 1));
                if (i > 0) {
                    stages[i].initOwner(stages[i - 1]);
                }
                stages[i].show();
            }
            return stages;
        });
        TestKit.await("the chain shown", 5000, () -> chain[0].isShowing() && chain[1].isShowing() && chain[2].isShowing());
        TestKit.fx(() -> chain[closed].hide());
        for (int i = 0; i < 3; i++) {
            Stage s = chain[i];
            boolean showing = i < closed;
            TestKit.await("stage " + (i + 1) + (showing ? " showing" : " closed"), 3000, () -> s.isShowing() == showing);
        }
        return closed == 0 ? "the owned stages closed with the first" : "stage 3 closed with stage 2, stage 1 stayed";
    }

    private String modalChildTest(boolean fullScreen) {
        Stage owner = TestKit.fx(() -> {
            Stage s = testStage(StageStyle.DECORATED, "Owner");
            if (fullScreen) {
                s.setFullScreen(true);
            } else {
                s.setMaximized(true);
            }
            s.show();
            return s;
        });
        TestKit.await("the owner " + (fullScreen ? "in full screen" : "maximized"), 5000,
                () -> owner.isShowing() && (fullScreen ? owner.isFullScreen() : owner.isMaximized()));
        TestKit.pause(500);
        Stage child = TestKit.fx(() -> {
            Stage s = testStage(StageStyle.DECORATED, "Modal child");
            s.initOwner(owner);
            s.initModality(Modality.APPLICATION_MODAL);
            s.show();
            return s;
        });
        TestKit.await("the child focused", 5000, () -> child.isFocused());
        TestKit.pause(300);
        TestKit.check(TestKit.fx(() -> fullScreen ? owner.isFullScreen() : owner.isMaximized()),
                "the owner left its state");
        return "the owner stayed " + (fullScreen ? "in full screen" : "maximized") + ", the child got the focus";
    }

    private String sceneChangeIconifiedTest() {
        Stage s = TestKit.fx(() -> {
            Stage stage = testStage(StageStyle.DECORATED, "Iconified");
            stage.show();
            return stage;
        });
        TestKit.await("the stage shown", 5000, () -> s.isShowing());
        TestKit.fx(() -> s.setIconified(true));
        TestKit.await("the stage iconified", 5000, () -> s.isIconified());
        TestKit.fx(() -> s.setScene(new Scene(new StackPane(new Label("New scene")), 300, 200)));
        TestKit.pause(500);
        TestKit.check(TestKit.fx(() -> s.isIconified()), "the new scene restored the stage");
        return "still iconified";
    }

    private String newSceneInputTest() {
        int[] clicks = {0};
        Button button = TestKit.fx(() -> new Button("Click me"));
        Stage s = TestKit.fx(() -> {
            Stage stage = testStage(StageStyle.DECORATED, "Set scene");
            stage.show();
            return stage;
        });
        TestKit.await("the stage shown", 5000, () -> s.isShowing() && s.isFocused());
        TestKit.fx(() -> {
            button.setOnAction(e -> clicks[0]++);
            StackPane root = new StackPane(button);
            s.setScene(new Scene(root, 300, 200));
        });
        TestKit.await("the new scene laid out", 3000, () -> button.getWidth() > 0
                && near(s.getScene().getRoot().getLayoutBounds().getWidth(), s.getScene().getWidth(), 1));
        TestKit.pause(300);
        TestKit.click(button);
        TestKit.await("the click", 3000, () -> clicks[0] == 1);
        return "the button of the new scene got the click";
    }

    private String keyClosesStageTest() {
        boolean[] typed = {false};
        boolean[] pressed = {false};
        Throwable[] failure = {null};
        TextArea area = TestKit.fx(() -> new TextArea());
        Stage s = TestKit.fx(() -> {
            Stage stage = testStage(StageStyle.DECORATED, "Escape closes");
            BorderPane root = new BorderPane(area);
            root.setOnKeyPressed(e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    pressed[0] = true;
                    stage.close();
                }
            });
            root.setOnKeyTyped(e -> typed[0] = true);
            stage.setScene(new Scene(root, 300, 200));
            Thread.currentThread().setUncaughtExceptionHandler((t, e) -> failure[0] = e);
            stage.show();
            return stage;
        });
        try {
            TestKit.await("the stage focused", 5000, () -> s.isFocused());
            TestKit.click(area);
            TestKit.fx(() -> area.getParent().requestFocus());
            TestKit.shortcut(javafx.scene.input.KeyCode.ESCAPE);
            TestKit.await("the stage closed", 3000, () -> pressed[0] && !s.isShowing());
            TestKit.pause(300);
        } finally {
            TestKit.fx(() -> Thread.currentThread().setUncaughtExceptionHandler(null));
        }
        TestKit.check(failure[0] == null, "the FX thread threw " + failure[0]);
        TestKit.check(!typed[0], "a KEY_TYPED arrived after the close");
        return "closed on KEY_PRESSED, no KEY_TYPED";
    }

    private String nestedLoopsTest() {
        List<String> order = new ArrayList<>();
        TestKit.fx(() -> {
            Object[] keys = {new Object(), new Object(), new Object()};
            Platform.runLater(() -> {
                Platform.runLater(() -> {
                    Platform.runLater(() -> {
                        order.add("depth 3");
                        Platform.exitNestedEventLoop(keys[2], "c");
                    });
                    order.add((String) Platform.enterNestedEventLoop(keys[2]));
                    Platform.exitNestedEventLoop(keys[1], "b");
                });
                order.add((String) Platform.enterNestedEventLoop(keys[1]));
                Platform.exitNestedEventLoop(keys[0], "a");
            });
            order.add((String) Platform.enterNestedEventLoop(keys[0]));
            if (Platform.isNestedLoopRunning()) {
                order.add("still nested");
            }
        });
        TestKit.check(order.equals(List.of("depth 3", "c", "b", "a")), "the loops returned " + order);
        return String.join(" → ", order);
    }

    private String showAndWaitTest() {
        List<String> order = java.util.Collections.synchronizedList(new ArrayList<>());
        Stage[] stages = TestKit.fx(() -> {
            Stage[] s = new Stage[3];
            for (int i = 0; i < 3; i++) {
                s[i] = testStage(StageStyle.DECORATED, "Modal " + (i + 1));
                s[i].initModality(Modality.APPLICATION_MODAL);
            }
            return s;
        });
        TestKit.fx(() -> Platform.runLater(() -> {
            Platform.runLater(() -> {
                Platform.runLater(() -> {
                    stages[2].showAndWait();
                    order.add("3");
                });
                stages[1].showAndWait();
                order.add("2");
            });
            stages[0].showAndWait();
            order.add("1");
        }));
        TestKit.await("the three stages shown", 5000, () -> stages[0].isShowing() && stages[1].isShowing()
                && stages[2].isShowing());
        for (int i = 2; i >= 0; i--) {
            Stage s = stages[i];
            TestKit.fx(() -> s.hide());
            int done = 3 - i;
            TestKit.await("showAndWait of modal " + (i + 1) + " returned", 3000, () -> order.size() == done);
        }
        TestKit.check(order.equals(List.of("3", "2", "1")), "showAndWait returned in the order " + order);
        return "returned 3, 2, 1";
    }
}
