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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.effect.Glow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
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
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/**
 * The tests of the robot, windows, dialogs, drag and drop, clipboard, 3D,
 * Swing, touch and accessibility labs in one window, a tab per lab, with a button that runs them all, one
 * lab after the other, and an overview of their OK, FAIL and SKIP results.
 * The robot tests need the GNOME test session:
 * <pre>./launcher.sh --apps=tests --compositor=gnome</pre>
 * With -Dtests.run=true the tests run at startup, and with -Dtests.exit=true
 * the application then exits, with status 1 when a test failed:
 * <pre>./launcher.sh --apps=tests --compositor=gnome -- -Dtests.run=true -Dtests.exit=true</pre>
 */
public class TestsDemo extends Application {

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
            -fx-accent: #8b5cf6;
            -fx-focus-color: #a78bfa;
            -fx-faint-focus-color: #a78bfa22;
            -fx-text-fill: #e8eef7;
        }
        .label { -fx-text-fill: #e8eef7; }
        .muted { -fx-text-fill: #93a1b8; }
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
        .labs > .tab-header-area { -fx-padding: 0 18 6 18; }
        .labs > .tab-header-area > .tab-header-background { -fx-background-color: transparent; }
        .labs > .tab-header-area > .headers-region > .tab {
            -fx-background-color: rgba(255, 255, 255, 0.06);
            -fx-background-radius: 9;
            -fx-padding: 4 12 4 12;
            -fx-background-insets: 0 3 0 3;
        }
        .labs > .tab-header-area > .headers-region > .tab:selected { -fx-background-color: rgba(139, 92, 246, 0.4); }
        .labs > .tab-header-area > .headers-region > .tab .tab-label { -fx-text-fill: #e8eef7; }
        .labs > .tab-header-area > .headers-region > .tab:selected .focus-indicator { -fx-border-color: transparent; }
        .labs > .tab-content-area { -fx-background-color: transparent; }
        .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-bar { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: rgba(255, 255, 255, 0.25); -fx-background-radius: 6; }
        .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 0; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-shape: " "; -fx-padding: 0; }
        """;

    private static final String STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));

    private record Lab(String name, TestKit.Suite suite, Tab tab) {}

    private final List<Lab> labs = new ArrayList<>();
    private final TabPane tabs = new TabPane();
    private final Label total = new Label("Not run");
    private Stage stage;

    @Override
    public void start(Stage stage) {
        // The reader of the cross-process clipboard check starts this application again
        if (ClipboardDemo.isReader(getParameters().getUnnamed())) {
            new ClipboardDemo().startReader(stage);
            return;
        }
        this.stage = stage;
        stage.initStyle(StageStyle.EXTENDED);

        RobotDemo robot = new RobotDemo();
        addLab("Robot", robot.content(stage), RobotDemo.STYLESHEET, robot.suite);
        WindowsDemo windows = new WindowsDemo();
        addLab("Windows", windows.content(stage), WindowsDemo.STYLESHEET, windows.suite);
        DialogsDemo dialogs = new DialogsDemo();
        addLab("Dialogs", dialogs.content(stage), DialogsDemo.STYLESHEET, dialogs.suite);
        DragDropDemo dragDrop = new DragDropDemo();
        addLab("Drag and drop", dragDrop.content(stage), DragDropDemo.STYLESHEET, dragDrop.suite);
        ClipboardDemo clipboard = new ClipboardDemo();
        addLab("Clipboard", clipboard.content(stage), ClipboardDemo.STYLESHEET, clipboard.suite);
        Scene3DDemo scene3d = new Scene3DDemo();
        addLab("3D", scene3d.content(stage), Scene3DDemo.STYLESHEET, scene3d.suite);
        SwingDemo swing = new SwingDemo();
        addLab("Swing", swing.content(stage), SwingDemo.STYLESHEET, swing.suite);
        TouchDemo touch = new TouchDemo();
        addLab("Touch", touch.content(stage), TouchDemo.STYLESHEET, touch.suite);
        AccessibilityDemo accessibility = new AccessibilityDemo();
        addLab("Accessibility", accessibility.content(stage), AccessibilityDemo.STYLESHEET, accessibility.suite);

        Tab overview = new Tab("Overview", overview());
        tabs.getTabs().addFirst(overview);
        tabs.getSelectionModel().select(overview);
        tabs.getStyleClass().add("labs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        BorderPane body = new BorderPane();
        body.setTop(header());
        body.setCenter(tabs);
        StackPane root = new StackPane(backgroundLayer(), body);
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, Math.min(1440, screen.getWidth() * 0.95), Math.min(940, screen.getHeight() * 0.94),
                Color.TRANSPARENT);
        scene.getStylesheets().addAll(STYLESHEET, TestKit.STYLESHEET);
        scene.getPreferences().setColorScheme(ColorScheme.DARK);
        stage.setScene(scene);
        stage.setTitle("Test Lab");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();
        TestKit.autorun(() -> run(labs));
    }

    private void addLab(String name, Region content, String stylesheet, TestKit.Suite suite) {
        // -Dtests.labs=clipboard,dragAndDrop keeps only those labs, by name without spaces
        String only = System.getProperty("tests.labs");
        if (only != null && List.of(only.split(",")).stream()
                .noneMatch(n -> n.equalsIgnoreCase(name.replace(" ", "")))) {
            return;
        }
        // The lab keeps its own look: its stylesheet, and its .root rules on the holder
        StackPane holder = new StackPane(content);
        holder.getStyleClass().add("root");
        holder.getStylesheets().add(stylesheet);
        Tab tab = new Tab(name, holder);
        tabs.getTabs().add(tab);
        labs.add(new Lab(name, suite, tab));
    }

    private Region backgroundLayer() {
        Region fill = new Region();
        if (Boolean.getBoolean("glass.gtk4.backdrop.active")) {
            fill.setBackground(new Background(new BackgroundFill(Color.color(0.02, 0.03, 0.06, 0.22), null, null)));
        } else {
            fill.setBackground(new Background(new BackgroundFill(new RadialGradient(0, 0, 0.2, 0.1, 1.2, true,
                    CycleMethod.NO_CYCLE, new Stop(0, Color.web("#1e1640")), new Stop(0.6, Color.web("#0b1120")),
                    new Stop(1, Color.web("#050811"))), null, null)));
        }
        return fill;
    }

    private Node header() {
        Circle logo = new Circle(10, new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#8b5cf6")), new Stop(1, Color.web("#14b8a6"))));
        logo.setEffect(new Glow(0.6));
        Label brand = new Label("Test Lab");
        brand.getStyleClass().add("brand");
        Label sub = new Label("robot · windows · dialogs · drag and drop · clipboard · 3D · Swing · touch · accessibility");
        sub.getStyleClass().add("muted");
        HBox left = new HBox(10, logo, brand, sub);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 18));

        Button runAll = new Button("Run all labs");
        runAll.getStyleClass().add("test-run");
        runAll.disableProperty().bind(TestKit.busyProperty());
        runAll.setOnAction(e -> run(labs));
        total.getStyleClass().add("test-summary");
        HBox right = new HBox(10, total, runAll,
                pill(TestKit.robotInput() ? "Robot input" : "No robot input"),
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

    private Node overview() {
        FlowPane cards = new FlowPane(14, 14);
        cards.setPadding(new Insets(4, 18, 18, 18));
        for (Lab lab : labs) {
            Label title = new Label(lab.name().toUpperCase(Locale.ROOT));
            title.getStyleClass().add("section");
            Button run = new Button("Run");
            run.disableProperty().bind(TestKit.busyProperty());
            run.setOnAction(e -> run(List.of(lab)));
            Button open = new Button("Open");
            open.setOnAction(e -> tabs.getSelectionModel().select(lab.tab()));
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox top = new HBox(10, title, spacer, open, run);
            top.setAlignment(Pos.CENTER_LEFT);
            VBox card = new VBox(10, top, lab.suite().summaryLabel(), lab.suite().compactView());
            card.getStyleClass().add("glass");
            card.setPrefWidth(660);
            cards.getChildren().add(card);
        }
        Label hint = new Label("Run all labs runs the tests of each lab in its tab, one lab after the other. "
                + "Robot tests need the GNOME test session (./launcher.sh --apps=tests --compositor=gnome) and "
                + "are skipped elsewhere; manual tests, such as the touch screen ones, pass when the lab sees "
                + "their events and are skipped until then. Do not use the mouse or the keyboard during a run.");
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);
        VBox.setMargin(hint, new Insets(0, 18, 0, 18));
        ScrollPane scroll = new ScrollPane(new VBox(10, hint, cards));
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    /**
     * Runs the suites of the labs one after the other, each in its tab.
     */
    private void run(List<Lab> selected) {
        if (!TestKit.tryLock()) {
            return;
        }
        total.setText("Running…");
        Thread.ofPlatform().daemon().name("tests").start(() -> {
            TestKit.Result result = TestKit.Result.NONE;
            long start = System.nanoTime();
            try {
                for (Lab lab : selected) {
                    TestKit.fx(() -> tabs.getSelectionModel().select(lab.tab()));
                    TestKit.pause(800);
                    result = result.plus(lab.suite().run(lab.suite().tests));
                }
            } finally {
                TestKit.unlock();
            }
            String text = result + " in " + (System.nanoTime() - start) / 1_000_000 / 1000 + " s";
            System.out.println("[All] " + text);
            boolean failed = result.fail() > 0;
            TestKit.fx(() -> {
                total.setText(text);
                total.getStyleClass().removeAll("ok", "fail");
                total.getStyleClass().add(failed ? "fail" : "ok");
                tabs.getSelectionModel().selectFirst();
                stage.toFront();
            });
            TestKit.exitIfAsked(result);
        });
    }
}
