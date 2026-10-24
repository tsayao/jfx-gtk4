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

import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A laboratory for several monitors: a map of the screens with their bounds
 * and scales, and tests that place stages, dialogs and popups on a screen of
 * another scale, move stages between the screens, and maximize them and make
 * them full screen there.
 * <p>
 * The tests need two monitors of different scales and windows at given
 * positions, which the nested GNOME Shell of the launcher gives the tests:
 * <pre>./launcher.sh --apps=tests --compositor=gnome --headless -- -Dtests.labs=screens</pre>
 * (a second monitor of 1280 × 800 at a scale near 2, see --monitors).
 * Elsewhere they are skipped. With the Mutter Development Kit (the nested
 * shell with a window), a monitor added from its menu serves as the second
 * one, and the manual hotplug tests pass when a monitor is added and removed.
 */
public class ScreensLab {

    static final String STYLESHEET = WindowsLab.STYLESHEET;
    private static final double SCALE_TOLERANCE = 0.01;
    private static final double BOUNDS_TOLERANCE = 2;

    final TestKit.Suite suite = new TestKit.Suite("Screens");
    private static final String ADD_MONITOR = "With the Mutter Development Kit (./launcher.sh --apps=tests "
            + "--compositor=gnome), use its menu to add a monitor";
    private final TestKit.Test screenAdded = suite.manual("Screen added", ADD_MONITOR
            + ": the new screen shows up in Screen.getScreens() with its size");
    private final TestKit.Test screenRemoved = suite.manual("Screen removed", "Remove the added monitor with the "
            + "menu of the Mutter Development Kit: its screen leaves Screen.getScreens()");
    // The screens before the last change, all with a size
    private List<Screen> lastScreens;
    private boolean sizeCheckPending;
    private final Pane map = new Pane();
    private final VBox details = new VBox(4);
    private final List<Stage> testStages = new ArrayList<>();
    private Stage stage;

    /**
     * The lab, for its tab in the tests app.
     */
    Region content(Stage stage) {
        this.stage = stage;
        defineTests();
        suite.setBeforeRun(() -> {
            stage.toFront();
            stage.requestFocus();
        });
        suite.setAfterEach(this::closeTestStages);

        map.setMinSize(0, 0);
        map.setPrefHeight(260);
        map.widthProperty().addListener(o -> drawMap());
        map.heightProperty().addListener(o -> drawMap());
        Screen.getScreens().addListener((ListChangeListener<Screen>) c -> drawMap());
        lastScreens = List.copyOf(Screen.getScreens());
        Screen.getScreens().addListener((ListChangeListener<Screen>) c -> screensChanged());
        VBox.setVgrow(map, Priority.ALWAYS);
        Label hint = new Label("The tests place stages on the screens, which needs the nested GNOME Shell of the "
                + "launcher with two monitors: ./launcher.sh --apps=tests --compositor=gnome --headless gives the "
                + "tests two of different scales. " + ADD_MONITOR + ".");
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);
        VBox left = column(TestKit.grow(card("Screens", map, details, hint)));
        VBox right = column(TestKit.grow(card("Tests", suite.view())));
        HBox columns = new HBox(14, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setPrefWidth(1);
        right.setPrefWidth(1.2);
        columns.setPadding(new Insets(4, 18, 18, 18));
        // Short of room, the cards are cut at the bottom, the header stays
        columns.setMinHeight(0);
        drawMap();
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

    /**
     * Draws the screens to scale in the map, with their bounds and scales.
     */
    private void drawMap() {
        map.getChildren().clear();
        details.getChildren().clear();
        List<Screen> screens = Screen.getScreens();
        if (screens.isEmpty() || map.getWidth() <= 0 || map.getHeight() <= 0) {
            return;
        }
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (Screen s : screens) {
            Rectangle2D b = s.getBounds();
            minX = Math.min(minX, b.getMinX());
            minY = Math.min(minY, b.getMinY());
            maxX = Math.max(maxX, b.getMaxX());
            maxY = Math.max(maxY, b.getMaxY());
        }
        double k = Math.min((map.getWidth() - 20) / (maxX - minX), (map.getHeight() - 20) / (maxY - minY));
        Color[] colors = {Color.web("#6366f1"), Color.web("#14b8a6"), Color.web("#f59e0b"), Color.web("#ec4899")};
        for (int i = 0; i < screens.size(); i++) {
            Screen s = screens.get(i);
            Rectangle2D b = s.getBounds();
            Color color = colors[i % colors.length];
            Rectangle r = new Rectangle(b.getWidth() * k - 4, b.getHeight() * k - 4, color.deriveColor(0, 1, 1, 0.35));
            r.setStroke(color);
            r.setArcWidth(10);
            r.setArcHeight(10);
            Text t = new Text(String.format(Locale.ROOT, "%d\n%.0f × %.0f\n× %.2f", i + 1, b.getWidth(),
                    b.getHeight(), s.getOutputScaleX()));
            t.setFill(Color.WHITE);
            StackPane box = new StackPane(r, t);
            box.setLayoutX(10 + (b.getMinX() - minX) * k + 2);
            box.setLayoutY(10 + (b.getMinY() - minY) * k + 2);
            map.getChildren().add(box);
            Label l = new Label(String.format(Locale.ROOT, "Screen %d%s: %.0f, %.0f, %.0f × %.0f, visual %.0f, %.0f, "
                    + "%.0f × %.0f, output scale %.2f, %.0f dpi", i + 1, s.equals(Screen.getPrimary()) ? " (primary)" : "",
                    b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight(), s.getVisualBounds().getMinX(),
                    s.getVisualBounds().getMinY(), s.getVisualBounds().getWidth(), s.getVisualBounds().getHeight(),
                    s.getOutputScaleX(), s.getDpi()));
            l.getStyleClass().add("muted");
            details.getChildren().add(l);
        }
    }

    // ---------------------------------------------------------------------
    // Tests

    private void defineTests() {
        suite.auto("Two screens", "Two screens side by side, the first one primary",
                () -> {
                    List<Screen> screens = requireScreens();
                    Screen a = screens.get(0), b = screens.get(1);
                    TestKit.check(TestKit.fx(() -> a.equals(Screen.getPrimary())), "the first screen is not the primary one");
                    TestKit.check(a.getBounds().getMaxX() <= b.getBounds().getMinX() + BOUNDS_TOLERANCE,
                            "the screens overlap: " + a.getBounds() + " and " + b.getBounds());
                    return String.format(Locale.ROOT, "%s at × %.2f, %s at × %.2f", size(a), a.getOutputScaleX(),
                            size(b), b.getOutputScaleX());
                });
        suite.auto("Screens for a rectangle", "Screen.getScreensForRectangle() finds both screens for a rectangle "
                + "across them, and one for a rectangle inside each", () -> {
            List<Screen> screens = requireScreens();
            Screen a = screens.get(0), b = screens.get(1);
            Rectangle2D across = new Rectangle2D(a.getBounds().getMaxX() - 50, 10, 100, 50);
            int both = TestKit.fx(() -> Screen.getScreensForRectangle(across).size());
            TestKit.check(both == 2, both + " screens across the edge");
            for (Screen s : List.of(a, b)) {
                Rectangle2D inside = new Rectangle2D(s.getBounds().getMinX() + 10, s.getBounds().getMinY() + 10, 50, 50);
                List<Screen> found = TestKit.fx(() -> Screen.getScreensForRectangle(inside));
                TestKit.check(found.size() == 1 && found.getFirst().equals(s), "found " + found + " inside " + size(s));
            }
            return "2 across, 1 inside each";
        });
        suite.auto("Stage on the second screen", "A stage placed on the second screen gets its output and render "
                + "scales", () -> {
            Screen b = requireScreens().get(1);
            Stage s = testStage("Second screen", b);
            awaitScale(s, b);
            TestKit.check(near(TestKit.fx(() -> s.getRenderScaleX()), b.getOutputScaleX()),
                    "the render scale is " + TestKit.fx(() -> s.getRenderScaleX()));
            return String.format(Locale.ROOT, "output and render scale %.2f", TestKit.fx(() -> s.getOutputScaleX()));
        });
        suite.auto("Moving between screens", "A stage moved to the other screen and back changes its scale each time",
                () -> {
                    List<Screen> screens = requireScreens(true);
                    Screen a = screens.get(0), b = screens.get(1);
                    Stage s = testStage("Moving", a);
                    awaitScale(s, a);
                    for (Screen to : List.of(b, a)) {
                        TestKit.fx(() -> place(s, to));
                        awaitScale(s, to);
                    }
                    return String.format(Locale.ROOT, "× %.2f → × %.2f → × %.2f", a.getOutputScaleX(),
                            b.getOutputScaleX(), a.getOutputScaleX());
                });
        suite.auto("Maximized on the second screen", "A stage maximized on the second screen fills its visual bounds",
                () -> {
                    Screen b = requireScreens().get(1);
                    Stage s = testStage("Maximized", b);
                    awaitScale(s, b);
                    TestKit.fx(() -> s.setMaximized(true));
                    Rectangle2D v = b.getVisualBounds();
                    awaitBounds(s, v, "the visual bounds of the second screen");
                    return bounds(s);
                });
        suite.auto("Full screen on the second screen", "A stage in full screen on the second screen covers it",
                () -> {
                    Screen b = requireScreens().get(1);
                    Stage s = testStage("Full screen", b);
                    awaitScale(s, b);
                    TestKit.fx(() -> s.setFullScreen(true));
                    awaitBounds(s, b.getBounds(), "the bounds of the second screen");
                    String shown = bounds(s);
                    TestKit.fx(() -> s.setFullScreen(false));
                    TestKit.await("the stage out of full screen", 5000, () -> !s.isFullScreen()
                            && s.getWidth() < b.getBounds().getWidth() - BOUNDS_TOLERANCE);
                    awaitScale(s, b);
                    return shown;
                });
        suite.auto("Popup on the second screen", "A context menu of a stage on the second screen opens there, at "
                + "its scale", () -> {
            Screen b = requireScreens().get(1);
            Stage s = testStage("Popup", b);
            awaitScale(s, b);
            ContextMenu menu = TestKit.fx(() -> {
                ContextMenu m = new ContextMenu(new MenuItem("One"), new MenuItem("Two"), new MenuItem("Three"));
                Node root = s.getScene().getRoot();
                javafx.geometry.Point2D p = root.localToScreen(40, 40);
                m.show(root, p.getX(), p.getY());
                return m;
            });
            try {
                TestKit.await("the menu shown", 5000, () -> menu.isShowing() && menu.getWidth() > 0);
                awaitScale(menu, b);
                Rectangle2D r = TestKit.fx(() -> new Rectangle2D(menu.getX(), menu.getY(), menu.getWidth(),
                        menu.getHeight()));
                TestKit.check(b.getBounds().contains(r.getMinX() + 1, r.getMinY() + 1), "the menu is at " + r);
                return String.format(Locale.ROOT, "at %.0f, %.0f, × %.2f", r.getMinX(), r.getMinY(),
                        TestKit.fx(() -> menu.getOutputScaleX()));
            } finally {
                TestKit.fx(menu::hide);
            }
        });
        suite.auto("Dialog on the second screen", "An alert owned by a stage on the second screen opens there, at "
                + "its scale", () -> {
            Screen b = requireScreens().get(1);
            Stage s = testStage("Dialog owner", b);
            awaitScale(s, b);
            Alert alert = TestKit.fx(() -> {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "On the second screen");
                a.initOwner(s);
                a.show();
                return a;
            });
            try {
                Window w = TestKit.fx(() -> alert.getDialogPane().getScene().getWindow());
                TestKit.await("the alert shown", 5000, () -> alert.isShowing() && w.getWidth() > 0);
                awaitScale(w, b);
                return String.format(Locale.ROOT, "× %.2f", TestKit.fx(() -> w.getOutputScaleX()));
            } finally {
                TestKit.fx(alert::close);
            }
        });
    }

    private static String size(Screen s) {
        return String.format(Locale.ROOT, "%.0f × %.0f", s.getBounds().getWidth(), s.getBounds().getHeight());
    }

    private static String bounds(Window w) {
        return TestKit.fx(() -> String.format(Locale.ROOT, "%.0f, %.0f, %.0f × %.0f", w.getX(), w.getY(),
                w.getWidth(), w.getHeight()));
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < SCALE_TOLERANCE;
    }

    /**
     * Passes the manual hotplug tests. A monitor may come before its geometry:
     * a new screen without a size must get one soon.
     */
    private void screensChanged() {
        List<Screen> now = List.copyOf(Screen.getScreens());
        boolean sized = now.stream().allMatch(s -> s.getBounds().getWidth() > 0 && s.getBounds().getHeight() > 0);
        if (now.size() > lastScreens.size() && !sized) {
            if (!sizeCheckPending) {
                sizeCheckPending = true;
                javafx.animation.PauseTransition wait = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
                wait.setOnFinished(e -> {
                    sizeCheckPending = false;
                    if (Screen.getScreens().stream().anyMatch(s -> s.getBounds().getWidth() <= 0)) {
                        screenAdded.fail("A new screen has no size after 3 s: " + Screen.getScreens().stream()
                                .map(ScreensLab::size).toList());
                    }
                });
                wait.play();
            }
            return;
        }
        if (now.size() > lastScreens.size()) {
            Screen added = now.stream().filter(s -> !lastScreens.contains(s)).findFirst().orElse(now.getLast());
            screenAdded.pass(String.format(Locale.ROOT, "Added %s at × %.2f, %d screens", size(added),
                    added.getOutputScaleX(), now.size()));
        } else if (now.size() < lastScreens.size()) {
            screenRemoved.pass("Removed, " + now.size() + (now.size() == 1 ? " screen" : " screens") + " left");
        }
        lastScreens = now;
    }

    /**
     * The screens, when the tests can place stages on two of different scales.
     */
    private static List<Screen> requireScreens() {
        return requireScreens(false);
    }

    private static List<Screen> requireScreens(boolean differentScales) {
        if (!"gnome".equals(System.getProperty("glass.gtk4.testSession"))) {
            TestKit.skip("Needs the nested GNOME Shell of the launcher, which places the windows: "
                    + "./launcher.sh --apps=tests --compositor=gnome");
        }
        List<Screen> screens = TestKit.fx(() -> List.copyOf(Screen.getScreens()));
        if (screens.size() < 2) {
            TestKit.skip("Needs two monitors, not " + screens.size() + ": " + ADD_MONITOR + ", or run the "
                    + "launcher with --headless or --monitors=1600x960@1,1280x800@2");
        }
        if (differentScales && near(screens.get(0).getOutputScaleX(), screens.get(1).getOutputScaleX())) {
            TestKit.skip("Needs monitors of different scales, not both at " + screens.get(0).getOutputScaleX());
        }
        return screens;
    }

    private void closeTestStages() {
        for (Stage s : List.copyOf(testStages)) {
            s.hide();
        }
        testStages.clear();
    }

    /** Places a stage in the middle of the visual bounds of a screen. */
    private static void place(Stage s, Screen screen) {
        Rectangle2D v = screen.getVisualBounds();
        s.setX(v.getMinX() + (v.getWidth() - s.getWidth()) / 2);
        s.setY(v.getMinY() + (v.getHeight() - s.getHeight()) / 2);
    }

    /** Shows a stage of 320 × 200 in the middle of a screen. */
    private Stage testStage(String title, Screen screen) {
        Stage s = TestKit.fx(() -> {
            Stage st = new Stage(StageStyle.DECORATED);
            st.setTitle(title);
            Label label = new Label(title);
            label.setStyle("-fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold;");
            StackPane root = new StackPane(label);
            root.setAlignment(Pos.CENTER);
            root.setStyle("-fx-background-color: #4338ca;");
            st.setScene(new Scene(root, 320, 200));
            st.setWidth(320);
            st.setHeight(240);
            place(st, screen);
            testStages.add(st);
            st.show();
            return st;
        });
        TestKit.await(title + " shown", 5000, () -> s.isShowing() && s.getScene().getWidth() > 0);
        return s;
    }

    /**
     * Waits for a window to have the output scale of a screen, which the GTK 4
     * glass takes from the surface the compositor placed, for 300 ms in a row:
     * a window gets the scale of the screen at its requested position first.
     */
    private static void awaitScale(Window w, Screen screen) {
        double expected = screen.getOutputScaleX();
        long[] since = {0};
        try {
            TestKit.await("the scale of the screen", 5000, () -> {
                if (!near(w.getOutputScaleX(), expected) || !near(w.getOutputScaleY(), expected)
                        || !near(w.getRenderScaleX(), expected)) {
                    since[0] = 0;
                    return false;
                }
                if (since[0] == 0) {
                    since[0] = System.nanoTime();
                }
                return System.nanoTime() - since[0] > 300_000_000L;
            });
        } catch (AssertionError e) {
            throw new AssertionError(String.format(Locale.ROOT, "%s: × %.2f, not × %.2f, at %s", e.getMessage(),
                    TestKit.fx(() -> w.getOutputScaleX()), expected, bounds(w)));
        }
    }

    private static void awaitBounds(Window w, Rectangle2D expected, String what) {
        try {
            TestKit.await(what, 5000, () -> Math.abs(w.getX() - expected.getMinX()) <= BOUNDS_TOLERANCE
                    && Math.abs(w.getY() - expected.getMinY()) <= BOUNDS_TOLERANCE
                    && Math.abs(w.getWidth() - expected.getWidth()) <= BOUNDS_TOLERANCE
                    && Math.abs(w.getHeight() - expected.getHeight()) <= BOUNDS_TOLERANCE);
        } catch (AssertionError e) {
            throw new AssertionError(e.getMessage() + ": the window is at " + bounds(w) + ", not "
                    + String.format(Locale.ROOT, "%.0f, %.0f, %.0f × %.0f", expected.getMinX(), expected.getMinY(),
                    expected.getWidth(), expected.getHeight()));
        }
    }
}
