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
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingNode;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * A laboratory for the interoperability with Swing: a SwingNode that shows
 * Swing components in this window, and a JFrame with a JFXPanel that shows
 * JavaFX in a Swing window. Its tests (see TestKit) check the painting of the
 * SwingNode, the robot input to the Swing components and to the JavaFX
 * button of the JFXPanel, the hand-off between the two event threads, and the
 * clipboard between Swing (on X11, through Xwayland) and JavaFX (on Wayland).
 * <p>
 * Swing needs an X display: in the GNOME session of the launcher it is the
 * Xwayland of the nested GNOME Shell. Without one, the tests are skipped.
 */
public class SwingDemo extends Application {

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
            -fx-accent: #3b82f6;
            -fx-focus-color: #60a5fa;
            -fx-faint-focus-color: #60a5fa22;
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
        .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-bar { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: rgba(255, 255, 255, 0.25); -fx-background-radius: 6; }
        .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 0; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-shape: " "; -fx-padding: 0; }
        """;

    static final String STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));

    /** The color of the Swing panel, which the painting test looks for. */
    private static final java.awt.Color PANEL = new java.awt.Color(0x3b, 0x82, 0xf6);

    final TestKit.Suite suite = new TestKit.Suite("Swing");
    private boolean backdropSupported;
    private final SwingNode swingNode = new SwingNode();
    private final StackPane swingHolder = new StackPane();
    private final Label status = new Label();
    private final AtomicInteger swingClicks = new AtomicInteger();
    private final AtomicInteger fxClicks = new AtomicInteger();
    // What the event dispatch thread threw, as JFXPanel paints there
    private volatile Throwable edtFailure;
    private JFXPanel jfxPanel;
    private JPanel colorPanel;
    private JButton swingButton;
    private JTextField swingField;
    private JFrame frame;
    private Button fxButton;

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.EXTENDED);
        BorderPane body = new BorderPane();
        body.setTop(header());
        body.setCenter(content(stage));
        StackPane root = new StackPane(backgroundLayer(), body);
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, Math.min(1200, screen.getWidth() * 0.9), Math.min(820, screen.getHeight() * 0.9),
                Color.TRANSPARENT);
        scene.getStylesheets().add(STYLESHEET);
        scene.getPreferences().setColorScheme(ColorScheme.DARK);
        stage.setScene(scene);
        stage.setTitle("Swing Lab");
        stage.show();
        TestKit.autorun(suite);
    }

    /**
     * The lab without its header bar, which the tests launcher shows in a tab.
     */
    Region content(Stage stage) {
        matchSwingScale();
        backdropSupported = Boolean.getBoolean("glass.gtk4.backdrop.active");
        defineTests();
        suite.setBeforeRun(() -> {
            stage.toFront();
            stage.requestFocus();
        });
        suite.setAfterEach(this::closeFrame);
        status.getStyleClass().add("muted");
        status.setWrapText(true);
        swingHolder.setMinSize(380, 180);
        swingHolder.setPrefSize(420, 200);
        if (hasDisplay()) {
            swingHolder.getChildren().add(swingNode);
            SwingUtilities.invokeLater(this::buildSwing);
            status.setText("Swing on " + System.getenv("DISPLAY"));
        } else {
            swingHolder.getChildren().add(new Label("No X display: Swing cannot start"));
            status.setText("Swing needs an X display (Xwayland), which this session has not: the tests are skipped");
        }
        Button open = new Button("Open a JFrame with a JFXPanel");
        open.setDisable(!hasDisplay());
        open.setOnAction(e -> SwingUtilities.invokeLater(this::openFrame));
        VBox left = column(card("SwingNode", swingHolder, status, open));
        VBox right = column(card("Tests", suite.view()));
        HBox columns = new HBox(14, left, right);
        HBox.setHgrow(right, Priority.ALWAYS);
        columns.setPadding(new Insets(4, 18, 18, 18));
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(columns);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    /**
     * Makes Swing draw at the scale that Xwayland expects, before AWT starts.
     * With a fractional monitor scale, GNOME runs Xwayland at the integer
     * scale above it and shrinks the X11 windows to the monitor scale: a 1.5
     * monitor has X11 clients draw at 2, which AWT, whose X11 scale is an
     * integer, does with sun.java2d.uiScale=2. Its windows then have the size
     * and the coordinates of the JavaFX windows.
     */
    private static void matchSwingScale() {
        if (System.getProperty("sun.java2d.uiScale") != null || System.getenv("GDK_SCALE") != null) {
            return;
        }
        double scale = 1;
        for (Screen screen : Screen.getScreens()) {
            scale = Math.max(scale, screen.getOutputScaleX());
        }
        System.setProperty("sun.java2d.uiScale", String.valueOf((int) Math.ceil(scale - 0.01)));
    }

    private static boolean hasDisplay() {
        return System.getenv("DISPLAY") != null && !GraphicsEnvironment.isHeadless();
    }

    private Region backgroundLayer() {
        Region fill = new Region();
        if (backdropSupported) {
            fill.setBackground(new Background(new BackgroundFill(Color.color(0.02, 0.03, 0.06, 0.22), null, null)));
        } else {
            fill.setBackground(new Background(new BackgroundFill(new RadialGradient(0, 0, 0.2, 0.1, 1.2, true,
                    CycleMethod.NO_CYCLE, new Stop(0, Color.web("#0c1a2e")), new Stop(0.6, Color.web("#0b1120")),
                    new Stop(1, Color.web("#050811"))), null, null)));
        }
        return fill;
    }

    private Node header() {
        Circle logo = new Circle(10, new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#3b82f6")), new Stop(1, Color.web("#f97316"))));
        logo.setEffect(new Glow(0.6));
        Label brand = new Label("Swing Lab");
        brand.getStyleClass().add("brand");
        Label sub = new Label("SwingNode · JFXPanel · threads · clipboard");
        sub.getStyleClass().add("muted");
        HBox left = new HBox(10, logo, brand, sub);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 18));
        HBox display = new HBox(new Label(hasDisplay() ? "X display " + System.getenv("DISPLAY") : "No X display"));
        display.getStyleClass().add("pill");
        HBox right = new HBox(10, display);
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

    // ---------------------------------------------------------------------
    // Swing, on the event dispatch thread

    private void buildSwing() {
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            edtFailure = e;
            e.printStackTrace();
            if (Boolean.getBoolean("swing.trace") && jfxPanel != null) {
                dumpFields(jfxPanel, "pWidth", "pHeight", "scaleFactorX", "scaleFactorY", "pixelsIm", "scenePeer");
            }
        });
        colorPanel = new JPanel();
        colorPanel.setBackground(PANEL);
        colorPanel.setPreferredSize(new java.awt.Dimension(400, 70));
        colorPanel.add(new JLabel("A Swing panel in a SwingNode"));
        swingButton = new JButton("Swing button");
        swingButton.addActionListener(e -> swingButton.setText("Clicked " + swingClicks.incrementAndGet() + "×"));
        swingField = new JTextField(18);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(swingButton);
        controls.add(swingField);
        JPanel root = new JPanel(new BorderLayout());
        root.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        root.add(colorPanel, BorderLayout.NORTH);
        root.add(controls, BorderLayout.CENTER);
        swingNode.setContent(root);
    }

    private void openFrame() {
        closeFrame();
        JFXPanel panel = new JFXPanel();
        jfxPanel = panel;
        frame = new JFrame("JFXPanel");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(new JLabel("JavaFX in a Swing window", JLabel.CENTER), BorderLayout.NORTH);
        frame.add(panel, BorderLayout.CENTER);
        frame.setSize(420, 260);
        frame.setLocation(160, 160);
        frame.setVisible(true);
        Platform.runLater(() -> {
            fxButton = new Button("JavaFX button");
            fxButton.setOnAction(e -> fxButton.setText("Clicked " + fxClicks.incrementAndGet() + "×"));
            StackPane fxRoot = new StackPane(fxButton);
            fxRoot.setStyle("-fx-background-color: #f97316;");
            panel.setScene(new Scene(fxRoot));
        });
    }

    private void closeFrame() {
        JFrame f = frame;
        frame = null;
        if (f != null) {
            SwingUtilities.invokeLater(f::dispose);
        }
    }

    // ---------------------------------------------------------------------
    // Tests

    private void defineTests() {
        suite.auto("SwingNode paints", "The color of the Swing panel shows in this window", () -> {
            requireDisplay();
            awaitSwing();
            Point2D at = onEdt(() -> {
                java.awt.Point p = SwingUtilities.convertPoint(colorPanel, 8, 8, swingNode.getContent());
                return new Point2D(p.x, p.y);
            });
            Color want = Color.rgb(PANEL.getRed(), PANEL.getGreen(), PANEL.getBlue());
            TestKit.await("the Swing panel on the screen", 5000, () -> {
                Point2D s = swingNode.localToScreen(at.getX(), at.getY());
                return s != null && close(TestKit.robot().getPixelColor(s), want);
            });
            return "the panel color is on the screen";
        });
        suite.robot("Swing button", "Clicks a JButton of the SwingNode", () -> {
            requireDisplay();
            awaitSwing();
            int before = swingClicks.get();
            TestKit.click(screenCenter(swingButton));
            TestKit.await("the action of the JButton", () -> swingClicks.get() == before + 1);
            return "clicked";
        });
        suite.robot("Swing text field", "Types in a JTextField of the SwingNode", () -> {
            requireDisplay();
            awaitSwing();
            onEdt(() -> {
                swingField.setText("");
                return null;
            });
            // A click focuses the window first, then the field
            boolean focused = false;
            for (int i = 0; i < 3 && !focused; i++) {
                TestKit.click(screenCenter(swingField));
                TestKit.pause(400);
                focused = Boolean.TRUE.equals(onEdt(() -> swingField.isFocusOwner()));
            }
            TestKit.check(focused, "the text field did not get the focus: window focused "
                    + TestKit.fx(() -> swingNode.getScene().getWindow().isFocused()) + ", SwingNode focused "
                    + TestKit.fx(() -> swingNode.isFocused()));
            TestKit.type("Swing 42");
            TestKit.await("the typed text", () -> "Swing 42".equals(onEdtQuietly(() -> swingField.getText())));
            return "\"Swing 42\"";
        });
        suite.auto("Event threads", "Hands work from the FX thread to the EDT and back", () -> {
            requireDisplay();
            CompletableFuture<String> done = new CompletableFuture<>();
            long t0 = System.nanoTime();
            Platform.runLater(() -> SwingUtilities.invokeLater(() -> {
                boolean edt = SwingUtilities.isEventDispatchThread();
                Platform.runLater(() -> done.complete(edt && Platform.isFxApplicationThread() ? "ok"
                        : "wrong thread"));
            }));
            String result = done.get(5, TimeUnit.SECONDS);
            long ms = (System.nanoTime() - t0) / 1_000_000;
            TestKit.check(result.equals("ok"), result);
            return "round trip in " + ms + " ms";
        });
        suite.auto("JFXPanel paints", "Opens a JFrame with a JFXPanel and paints it in Swing", () -> {
            requireDisplay();
            edtFailure = null;
            openFrameAndWait();
            // The JavaFX scene, as the JFXPanel paints it for Swing, near its corner
            java.awt.Color corner = null;
            long deadline = System.nanoTime() + 5_000_000_000L;
            while (System.nanoTime() < deadline) {
                corner = onEdt(() -> {
                    java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                            Math.max(1, jfxPanel.getWidth()), Math.max(1, jfxPanel.getHeight()),
                            java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    java.awt.Graphics2D g = image.createGraphics();
                    jfxPanel.paint(g);
                    g.dispose();
                    return new java.awt.Color(image.getRGB(6, 6), true);
                });
                if (Math.abs(corner.getRed() - 0xf9) < 12 && Math.abs(corner.getGreen() - 0x73) < 12) {
                    break;
                }
                TestKit.pause(200);
            }
            Throwable failure = edtFailure;
            TestKit.check(failure == null, "the event dispatch thread threw " + failure);
            TestKit.check(Math.abs(corner.getRed() - 0xf9) < 12 && Math.abs(corner.getGreen() - 0x73) < 12,
                    "the JFXPanel paints " + corner + ", expected the orange of its scene");
            return "the JavaFX scene paints in Swing";
        });
        suite.robot("JFXPanel button", "Clicks the JavaFX button of the JFXPanel in the JFrame", () -> {
            requireDisplay();
            openFrameAndWait();
            int before = fxClicks.get();
            // The lab window may cover the JFrame, an X11 window that the compositor keeps below it
            TestKit.fx(() -> ((Stage) swingHolder.getScene().getWindow()).setIconified(true));
            TestKit.pause(500);
            onEdt(() -> {
                frame.toFront();
                frame.requestFocus();
                return null;
            });
            try {
                TestKit.await("the JFrame focused", 3000, () -> Boolean.TRUE.equals(onEdtQuietly(() -> frame.isFocused())));
            } catch (AssertionError e) {
                // Clicked below anyway: the click itself focuses it
            }
            Point2D fxAt = TestKit.fx(() -> {
                Bounds b = fxButton.localToScreen(fxButton.getLayoutBounds());
                return new Point2D(b.getCenterX(), b.getCenterY());
            });
            // The button is at the center of the scene, which fills the JFXPanel
            Point2D at = onEdt(() -> {
                java.awt.Point p = jfxPanel.getLocationOnScreen();
                return new Point2D(p.x + jfxPanel.getWidth() / 2.0, p.y + jfxPanel.getHeight() / 2.0);
            });
            TestKit.click(at);
            boolean focused = Boolean.TRUE.equals(onEdt(() -> frame.isFocused()));
            try {
                TestKit.await("the action of the JavaFX button", () -> fxClicks.get() == before + 1);
            } catch (AssertionError e) {
                restoreLab();
                if (!focused) {
                    throw new AssertionError(e.getMessage() + ": the JFrame did not get the focus");
                }
                throw new AssertionError(e.getMessage() + ": clicked at " + TestKit.fmt(at)
                        + ", where Swing has the panel center; JavaFX has the button at " + TestKit.fmt(fxAt));
            }
            restoreLab();
            return "clicked at " + TestKit.fmt(at);
        });
        suite.auto("Swing copies, JavaFX pastes", "Copies text with AWT on X11 and reads it with JavaFX", () -> {
            requireDisplay();
            String text = "From Swing " + System.nanoTime() % 100000;
            onEdt(() -> {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
                return null;
            });
            TestKit.await("the text in JavaFX", 5000,
                    () -> text.equals(javafx.scene.input.Clipboard.getSystemClipboard().getString()));
            return "\"" + text + "\"";
        });
        suite.auto("JavaFX copies, Swing pastes", "Copies text with JavaFX and reads it with AWT on X11", () -> {
            requireDisplay();
            String text = "From JavaFX " + System.nanoTime() % 100000;
            if (TestKit.robotInput()) {
                // Input to this window, so that the compositor takes its copy over the
                // selection that Swing may own since the last test
                TestKit.click(status);
            }
            TestKit.fx(() -> {
                ClipboardContent c = new ClipboardContent();
                c.putString(text);
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(c);
            });
            // Read from this thread: Xwayland asks this process for the data on the FX
            // thread, which must not wait for the EDT meanwhile
            String read = null;
            long deadline = System.nanoTime() + 6_000_000_000L;
            while (System.nanoTime() < deadline && !text.equals(read)) {
                read = onEdt(() -> {
                    try {
                        return (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                                .getData(DataFlavor.stringFlavor);
                    } catch (Exception e) {
                        return e.toString();
                    }
                });
                if (!text.equals(read)) {
                    TestKit.pause(200);
                }
            }
            try {
                TestKit.check(text.equals(read), "Swing reads \"" + read + "\" instead of \"" + text + "\"");
            } catch (AssertionError e) {
                if (!TestKit.robotInput()) {
                    TestKit.skip("Swing owns the clipboard since the last input to this window, so the compositor "
                            + "ignores the copy: run with the robot");
                }
                throw e;
            }
            return "\"" + text + "\"";
        });
    }

    private void restoreLab() {
        TestKit.fx(() -> ((Stage) swingHolder.getScene().getWindow()).setIconified(false));
        TestKit.pause(300);
    }

    private static void requireDisplay() {
        if (!hasDisplay()) {
            TestKit.skip("Swing needs an X display (Xwayland)");
        }
    }

    private void awaitSwing() {
        TestKit.await("the Swing components", 5000, () -> swingButton != null && swingButton.isShowing());
        TestKit.reveal(swingNode);
        TestKit.pause(300);
    }

    private void openFrameAndWait() throws Exception {
        onEdt(() -> {
            openFrame();
            return null;
        });
        TestKit.await("the JFXPanel scene", 8000, () -> fxButton != null && fxButton.getScene() != null
                && fxButton.localToScreen(fxButton.getLayoutBounds()) != null && fxButton.getWidth() > 0);
        TestKit.pause(800);
    }

    /**
     * The center of a Swing component of the SwingNode, on the screen.
     */
    private Point2D screenCenter(Component c) throws Exception {
        Point2D local = onEdt(() -> {
            java.awt.Point p = SwingUtilities.convertPoint(c, c.getWidth() / 2, c.getHeight() / 2,
                    swingNode.getContent());
            return new Point2D(p.x, p.y);
        });
        return TestKit.fx(() -> swingNode.localToScreen(local.getX(), local.getY()));
    }

    private static <T> T onEdt(Supplier<T> s) throws InterruptedException, InvocationTargetException {
        Object[] result = {null};
        SwingUtilities.invokeAndWait(() -> result[0] = s.get());
        @SuppressWarnings("unchecked")
        T t = (T) result[0];
        return t;
    }

    /**
     * Runs on the EDT from the FX thread, as the conditions of TestKit.await do.
     */
    private static <T> T onEdtQuietly(Supplier<T> s) {
        CompletableFuture<T> f = new CompletableFuture<>();
        SwingUtilities.invokeLater(() -> f.complete(s.get()));
        try {
            return f.get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            return null;
        }
    }

    // -Dswing.trace=true, with the packages opened: the sizes behind a failed paint
    private static void dumpFields(Object o, String... names) {
        for (String name : names) {
            try {
                java.lang.reflect.Field f = o.getClass().getDeclaredField(name);
                f.setAccessible(true);
                Object v = f.get(o);
                System.out.println("TRACE " + name + " = " + v);
                if (name.equals("scenePeer") && v != null) {
                    for (String n : new String[] {"texLineStride", "texBits", "texScaleFactorX", "renderScaleX"}) {
                        try {
                            java.lang.reflect.Field g = v.getClass().getDeclaredField(n);
                            g.setAccessible(true);
                            System.out.println("TRACE   " + n + " = " + g.get(v));
                        } catch (ReflectiveOperationException | RuntimeException ex) {
                            System.out.println("TRACE   " + n + ": " + ex);
                        }
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException ex) {
                System.out.println("TRACE " + name + ": " + ex);
            }
        }
    }

    private static boolean close(Color a, Color b) {
        return Math.abs(a.getRed() - b.getRed()) < 0.05 && Math.abs(a.getGreen() - b.getGreen()) < 0.05
                && Math.abs(a.getBlue() - b.getBlue()) < 0.05;
    }
}
