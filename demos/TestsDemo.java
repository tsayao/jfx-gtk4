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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
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
 * The tests of the robot, windows, screens, stage, dialogs, drag and drop,
 * clipboard, render, 3D, Swing, touch and accessibility labs in one window, a
 * tab per lab, with a button that runs them all, one lab after the other, and
 * an overview of their OK, FAIL and SKIP results.
 * The robot tests take over the pointer and the keyboard of the desktop, which
 * asks once for the permission, unless they run in the nested GNOME Shell of
 * the launcher, here without a window:
 * <pre>./launcher.sh --apps=tests --compositor=gnome --headless</pre>
 * With -Dtests.run=true the tests run at startup, and with -Dtests.exit=true
 * the application then exits, with status 1 when a test failed:
 * <pre>./launcher.sh --apps=tests --compositor=gnome --headless -- -Dtests.run=true -Dtests.exit=true</pre>
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
        if (ClipboardLab.isReader(getParameters().getUnnamed())) {
            new ClipboardLab().startReader(stage);
            return;
        }
        this.stage = stage;
        stage.initStyle(StageStyle.EXTENDED);

        RobotLab robot = new RobotLab();
        addLab("Robot", robot.content(stage), RobotLab.STYLESHEET, robot.suite);
        WindowsLab windows = new WindowsLab();
        addLab("Windows", windows.content(stage), WindowsLab.STYLESHEET, windows.suite);
        ScreensLab screens = new ScreensLab();
        addLab("Screens", screens.content(stage), ScreensLab.STYLESHEET, screens.suite);
        StageLab stageLab = new StageLab();
        addLab("Stage", stageLab.content(stage), StageLab.STYLESHEET, null);
        DialogsLab dialogs = new DialogsLab();
        addLab("Dialogs", dialogs.content(stage), DialogsLab.STYLESHEET, dialogs.suite);
        DragDropLab dragDrop = new DragDropLab();
        addLab("Drag and drop", dragDrop.content(stage), DragDropLab.STYLESHEET, dragDrop.suite);
        ClipboardLab clipboard = new ClipboardLab();
        addLab("Clipboard", clipboard.content(stage), ClipboardLab.STYLESHEET, clipboard.suite);
        Scene3DLab scene3d = new Scene3DLab();
        RenderLab render = new RenderLab();
        addLab("Render", render.content(stage), render.stylesheet(), render.suite);
        addLab("3D", scene3d.content(stage), Scene3DLab.STYLESHEET, scene3d.suite);
        SwingLab swing = new SwingLab();
        addLab("Swing", swing.content(stage), SwingLab.STYLESHEET, swing.suite);
        TouchLab touch = new TouchLab();
        addLab("Touch", touch.content(stage), TouchLab.STYLESHEET, touch.suite);
        AccessibilityLab accessibility = new AccessibilityLab();
        addLab("Accessibility", accessibility.content(stage), AccessibilityLab.STYLESHEET, accessibility.suite);

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
        holder.getStylesheets().addAll(stylesheet, TestKit.POPUP_STYLESHEET);
        Tab tab = new Tab(name, holder);
        tabs.getTabs().add(tab);
        // A lab without tests is a playground, left out of the overview and the runs
        if (suite != null) {
            labs.add(new Lab(name, suite, tab));
        }
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
        Label sub = new Label("robot · windows · screens · stages · dialogs · drag and drop · clipboard · rendering · 3D · Swing · touch · accessibility");
        sub.getStyleClass().add("muted");
        HBox left = new HBox(10, logo, brand, sub);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 18));

        Button runAll = new Button("Run all labs");
        runAll.getStyleClass().add("test-run");
        runAll.disableProperty().bind(TestKit.busyProperty());
        runAll.setOnAction(e -> run(labs));
        Button stop = TestKit.stopButton(new Button("Stop"));
        total.getStyleClass().add("test-summary");
        HBox right = new HBox(10, total, runAll, stop,
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
        // The labs in a grid that fills the tab, each list scrolling in its card
        GridPane cards = new GridPane(14, 14);
        cards.setPadding(new Insets(4, 18, 18, 18));
        cards.setMinHeight(0);
        int perRow = Math.max(1, Math.min(3, labs.size()));
        int rows = (labs.size() + perRow - 1) / perRow;
        for (int i = 0; i < perRow; i++) {
            ColumnConstraints c = new ColumnConstraints();
            c.setPercentWidth(100.0 / perRow);
            cards.getColumnConstraints().add(c);
        }
        for (int i = 0; i < rows; i++) {
            RowConstraints r = new RowConstraints();
            r.setPercentHeight(100.0 / rows);
            cards.getRowConstraints().add(r);
        }
        int index = 0;
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
            ScrollPane list = new ScrollPane(lab.suite().compactView());
            list.getStyleClass().add("test-list");
            list.getStylesheets().add(TestKit.STYLESHEET);
            list.setFitToWidth(true);
            list.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            list.setMinHeight(0);
            VBox.setVgrow(list, Priority.ALWAYS);
            VBox card = new VBox(10, top, lab.suite().summaryLabel(), list);
            card.getStyleClass().add("glass");
            card.setMinSize(0, 0);
            cards.add(card, index % perRow, index / perRow);
            index++;
        }
        Label hint = new Label("Run all labs runs the tests of each lab in its tab, one lab after the other. "
                + "Robot tests take over the pointer and the keyboard of this desktop, which asks once for the "
                + "permission (-Dtests.robot=false skips them; ./launcher.sh --apps=tests --compositor=gnome "
                + "--headless runs them apart); manual tests, such as the touch screen ones, pass when the lab sees "
                + "their events and are skipped until then. Do not use the mouse or the keyboard during a run.");
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);
        hint.setMinHeight(Region.USE_PREF_SIZE);
        VBox.setMargin(hint, new Insets(0, 18, 0, 18));
        VBox.setVgrow(cards, Priority.ALWAYS);
        VBox page = new VBox(10, hint, cards);
        page.setMinHeight(0);
        return page;
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
            boolean stopped = false;
            try {
                for (Lab lab : selected) {
                    TestKit.fx(() -> tabs.getSelectionModel().select(lab.tab()));
                    TestKit.pause(800);
                    result = result.plus(lab.suite().run(lab.suite().tests));
                    if (TestKit.isStopping()) {
                        break;
                    }
                }
            } catch (TestKit.Stopped e) {
                // Stopped before a lab started
            } finally {
                stopped = TestKit.isStopping();
                TestKit.unlock();
            }
            String text = (stopped ? "Stopped: " : "") + result + " in "
                    + (System.nanoTime() - start) / 1_000_000 / 1000 + " s";
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
