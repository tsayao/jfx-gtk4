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

import javafx.application.Application;
import javafx.application.ColorScheme;
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
 * allows in the GNOME test session of the launcher:
 * <pre>./launcher.sh --apps=windows --compositor=gnome</pre>
 */
public class WindowsDemo extends Application {

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

    final TestKit.Suite suite = new TestKit.Suite("Windows");
    private boolean backdropSupported;
    private Stage stage;
    private final ObservableList<String> log = FXCollections.observableArrayList();
    private final ListView<String> logView = new ListView<>(log);
    private final List<Probe> probes = new ArrayList<>();
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

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.EXTENDED);
        BorderPane body = new BorderPane();
        body.setTop(header());
        body.setCenter(content(stage));
        StackPane root = new StackPane(backgroundLayer(), body);
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, Math.min(1240, screen.getWidth() * 0.9), Math.min(860, screen.getHeight() * 0.9),
                Color.TRANSPARENT);
        scene.getStylesheets().add(STYLESHEET);
        scene.getPreferences().setColorScheme(ColorScheme.DARK);
        stage.setScene(scene);
        stage.setTitle("Windows Lab");
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.show();
        TestKit.autorun(suite);
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
        suite.setAfterEach(this::closeProbes);
        VBox left = column(openCard(), logCard());
        VBox right = column(card("Tests", suite.view()));
        HBox columns = new HBox(14, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setPrefWidth(1);
        right.setPrefWidth(1.4);
        columns.setPadding(new Insets(4, 18, 18, 18));
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(columns);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private Region backgroundLayer() {
        Region fill = new Region();
        if (backdropSupported) {
            fill.setBackground(new Background(new BackgroundFill(Color.color(0.02, 0.03, 0.06, 0.22), null, null)));
        } else {
            fill.setBackground(new Background(new BackgroundFill(new RadialGradient(0, 0, 0.2, 0.1, 1.2, true,
                    CycleMethod.NO_CYCLE, new Stop(0, Color.web("#2a1028")), new Stop(0.6, Color.web("#0b1120")),
                    new Stop(1, Color.web("#050811"))), null, null)));
        }
        return fill;
    }

    private Node header() {
        Circle logo = new Circle(10, new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ec4899")), new Stop(1, Color.web("#6366f1"))));
        logo.setEffect(new Glow(0.6));
        Label brand = new Label("Windows Lab");
        brand.getStyleClass().add("brand");
        Label sub = muted("styles · focus · states · modality · sizes · popups");
        HBox left = new HBox(10, logo, brand, sub);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 18));
        HBox right = new HBox(10, pill(TestKit.robotInput() ? "Robot input" : "No robot input"),
                pill(System.getProperty("glass.platform", "default") + " glass"));
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setFillHeight(false);
        right.setPadding(new Insets(0, 10, 0, 0));
        HeaderBar.setDragType(left, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar header = new HeaderBar(left, null, right);
        header.setBackground(null);
        header.setMinHeight(52);
        HeaderBar.setAlignment(left, Pos.CENTER_LEFT);
        return header;
    }

    private static HBox pill(String text) {
        HBox box = new HBox(new Label(text));
        box.getStyleClass().add("pill");
        return box;
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
        for (StageStyle style : StageStyle.values()) {
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
                + "open their own windows at fixed positions, which Wayland only allows in the GNOME test session.");
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
        for (StageStyle style : StageStyle.values()) {
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
}
