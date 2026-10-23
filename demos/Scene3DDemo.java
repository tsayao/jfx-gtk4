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

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
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
import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Box;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

/**
 * A laboratory for 3D rendering: its tests (see TestKit) render small scenes
 * in a SubScene and check their pixels on the screen, as the robot reads
 * them, and in a snapshot: flat colors, the depth buffer, lighting, textures,
 * a TriangleMesh and antialiasing; and measure the frame rate of heavier
 * scenes. The tests are skipped when the renderer has no 3D, as the software
 * pipeline (-Dprism.order=sw).
 */
public class Scene3DDemo extends Application {

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
            -fx-accent: #f97316;
            -fx-focus-color: #fb923c;
            -fx-faint-focus-color: #fb923c22;
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
        .viewport {
            -fx-background-color: black;
            -fx-background-radius: 12;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 12;
        }
        .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-bar { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: rgba(255, 255, 255, 0.25); -fx-background-radius: 6; }
        .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 0; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-shape: " "; -fx-padding: 0; }
        """;

    static final String STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));

    private static final double VIEW_W = 420;
    private static final double VIEW_H = 300;
    private static final double TOLERANCE = 0.06;
    /** The frame rate the performance tests ask for. */
    private static final double MIN_FPS = Double.parseDouble(System.getProperty("scene3d.minFps", "30"));
    private static final long MEASURE_MS = 3000;

    final TestKit.Suite suite = new TestKit.Suite("3D");
    private boolean backdropSupported;
    private final StackPane viewport = new StackPane();
    private final Label viewInfo = new Label("No scene yet");

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
        stage.setTitle("3D Lab");
        stage.show();
        TestKit.autorun(suite);
    }

    /**
     * The lab without its header bar, which the tests launcher shows in a tab.
     */
    Region content(Stage stage) {
        backdropSupported = Boolean.getBoolean("glass.gtk4.backdrop.active");
        defineTests();
        suite.setBeforeRun(() -> {
            stage.toFront();
            stage.requestFocus();
        });
        viewport.getStyleClass().add("viewport");
        viewport.setMinSize(VIEW_W, VIEW_H);
        viewport.setPrefSize(VIEW_W, VIEW_H);
        viewport.setMaxSize(VIEW_W, VIEW_H);
        viewInfo.getStyleClass().add("muted");
        viewInfo.setWrapText(true);
        Label renderer = new Label("Pipeline: " + pipelineName() + (has3D() ? "" : " (no 3D: the tests are skipped)"));
        VBox left = column(card("Scene", viewport, viewInfo, renderer));
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

    private Region backgroundLayer() {
        Region fill = new Region();
        if (backdropSupported) {
            fill.setBackground(new Background(new BackgroundFill(Color.color(0.02, 0.03, 0.06, 0.22), null, null)));
        } else {
            fill.setBackground(new Background(new BackgroundFill(new RadialGradient(0, 0, 0.2, 0.1, 1.2, true,
                    CycleMethod.NO_CYCLE, new Stop(0, Color.web("#2a1a0c")), new Stop(0.6, Color.web("#0b1120")),
                    new Stop(1, Color.web("#050811"))), null, null)));
        }
        return fill;
    }

    private Node header() {
        Circle logo = new Circle(10, new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#f97316")), new Stop(1, Color.web("#ec4899"))));
        logo.setEffect(new Glow(0.6));
        Label brand = new Label("3D Lab");
        brand.getStyleClass().add("brand");
        Label sub = new Label("colors · depth · lights · textures · meshes · antialiasing · frame rate");
        sub.getStyleClass().add("muted");
        HBox left = new HBox(10, logo, brand, sub);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 18));
        HBox pipe = new HBox(new Label(pipelineName() + " pipeline"));
        pipe.getStyleClass().add("pill");
        HBox right = new HBox(10, pipe);
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

    private static boolean has3D() {
        return Platform.isSupported(ConditionalFeature.SCENE3D);
    }

    /**
     * The name of the Prism pipeline, without exporting com.sun.prism.
     */
    private static String pipelineName() {
        try {
            Object pipeline = Class.forName("com.sun.prism.GraphicsPipeline").getMethod("getPipeline").invoke(null);
            String name = pipeline.getClass().getSimpleName();
            return name.endsWith("Pipeline") ? name.substring(0, name.length() - "Pipeline".length()) : name;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return System.getProperty("prism.order", "default");
        }
    }

    // ---------------------------------------------------------------------
    // Scenes

    /**
     * A SubScene with a depth buffer and a perspective camera 600 units in
     * front of the origin, which the tests fill.
     */
    private static SubScene subScene(Group world, SceneAntialiasing aa) {
        SubScene sub = new SubScene(world, VIEW_W, VIEW_H, true, aa);
        sub.setFill(Color.BLACK);
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setFieldOfView(35);
        camera.setNearClip(1);
        camera.setFarClip(5000);
        camera.setTranslateZ(-600);
        sub.setCamera(camera);
        return sub;
    }

    private static PhongMaterial material(Color diffuse) {
        PhongMaterial m = new PhongMaterial(diffuse);
        m.setSpecularColor(Color.BLACK);
        return m;
    }

    /**
     * Shows a scene in the viewport and waits for the frames that show it.
     */
    private SubScene show(Group world, SceneAntialiasing aa, String info) {
        SubScene sub = fx(() -> {
            SubScene s = subScene(world, aa);
            viewport.getChildren().setAll(s);
            viewInfo.setText(info);
            return s;
        });
        TestKit.reveal(viewport);
        TestKit.pause(600);
        return sub;
    }

    private static void require3D() {
        if (!has3D()) {
            TestKit.skip("The " + pipelineName() + " renderer has no 3D");
        }
    }

    /**
     * The color on the screen at a point of the viewport, relative to its
     * center, as the robot reads it.
     */
    private Color screenColor(double dx, double dy) {
        return fx(() -> {
            Bounds b = TestKit.screenBounds(viewport);
            return TestKit.robot().getPixelColor(b.getCenterX() + dx, b.getCenterY() + dy);
        });
    }

    private static boolean close(Color a, Color b) {
        return Math.abs(a.getRed() - b.getRed()) <= TOLERANCE && Math.abs(a.getGreen() - b.getGreen()) <= TOLERANCE
                && Math.abs(a.getBlue() - b.getBlue()) <= TOLERANCE;
    }

    private static String hex(Color c) {
        return String.format("#%02x%02x%02x", Math.round(c.getRed() * 255), Math.round(c.getGreen() * 255),
                Math.round(c.getBlue() * 255));
    }

    private static double luminance(Color c) {
        return 0.2126 * c.getRed() + 0.7152 * c.getGreen() + 0.0722 * c.getBlue();
    }

    private void expect(String what, double dx, double dy, Color want) {
        Color got = screenColor(dx, dy);
        TestKit.check(close(got, want), what + " is " + hex(got) + " on the screen, expected " + hex(want));
    }

    // ---------------------------------------------------------------------
    // Tests

    private void defineTests() {
        suite.auto("Flat color", "A red box under a white ambient light only, on the screen and in a snapshot",
                () -> {
                    require3D();
                    Box box = new Box(160, 160, 160);
                    box.setMaterial(material(Color.RED));
                    box.getTransforms().addAll(new Rotate(25, Rotate.Y_AXIS), new Rotate(20, Rotate.X_AXIS));
                    SubScene sub = show(new Group(box, new AmbientLight(Color.WHITE)), SceneAntialiasing.DISABLED,
                            "A red box lit by a white ambient light: every face is pure red");
                    expect("the center of the box", 0, 0, Color.RED);
                    expect("the background", VIEW_W / 2 - 12, VIEW_H / 2 - 12, Color.BLACK);
                    WritableImage snap = fx(() -> sub.snapshot(null, null));
                    Color center = snap.getPixelReader().getColor((int) snap.getWidth() / 2, (int) snap.getHeight() / 2);
                    TestKit.check(close(center, Color.RED), "the snapshot center is " + hex(center));
                    return "red on the screen and in the snapshot";
                });
        suite.auto("Depth buffer", "A green box in front of a blue one, added before it", () -> {
            require3D();
            Box green = new Box(160, 160, 40);
            green.setMaterial(material(Color.LIME));
            green.setTranslateX(-50);
            green.setTranslateZ(-120);
            Box blue = new Box(160, 160, 40);
            blue.setMaterial(material(Color.BLUE));
            blue.setTranslateX(50);
            blue.setTranslateZ(120);
            show(new Group(green, blue, new AmbientLight(Color.WHITE)), SceneAntialiasing.DISABLED,
                    "The nearer green box must cover the blue one, though the blue one is drawn after it");
            expect("the overlap", 0, 0, Color.LIME);
            // The farther blue box spans about -20 to 86 pixels from the center, the green one up to 30
            expect("the uncovered part of the blue box", 60, 0, Color.BLUE);
            return "the nearer box wins";
        });
        suite.auto("Point light", "A white sphere lit from the left by a point light", () -> {
            require3D();
            Sphere sphere = new Sphere(110, 96);
            sphere.setMaterial(material(Color.WHITE));
            PointLight light = new PointLight(Color.WHITE);
            light.setTranslateX(-600);
            light.setTranslateZ(-300);
            show(new Group(sphere, light), SceneAntialiasing.DISABLED,
                    "Lit from the left: bright on the left, dark on the right");
            double left = luminance(screenColor(-60, 0));
            double right = luminance(screenColor(80, 0));
            TestKit.check(left > 0.5, String.format(Locale.ROOT, "the lit side is too dark: %.2f", left));
            TestKit.check(left > right + 0.3, String.format(Locale.ROOT,
                    "the lit side (%.2f) is not brighter than the other (%.2f)", left, right));
            return String.format(Locale.ROOT, "luminance %.2f on the left, %.2f on the right", left, right);
        });
        suite.auto("Texture", "A box with a 2 × 2 checker texture, red and white", () -> {
            require3D();
            Box box = new Box(180, 180, 10);
            PhongMaterial m = material(Color.WHITE);
            m.setDiffuseMap(checker());
            box.setMaterial(m);
            show(new Group(box, new AmbientLight(Color.WHITE)), SceneAntialiasing.DISABLED,
                    "The front face shows the checker: red top left and bottom right, white elsewhere");
            expect("the top left square", -45, -45, Color.RED);
            expect("the top right square", 45, -45, Color.WHITE);
            expect("the bottom left square", -45, 45, Color.WHITE);
            expect("the bottom right square", 45, 45, Color.RED);
            return "the four squares";
        });
        suite.auto("Triangle mesh", "A TriangleMesh square, textured green, seen from both sides", () -> {
            require3D();
            MeshView mesh = new MeshView(square(160));
            mesh.setMaterial(material(Color.LIME));
            mesh.setCullFace(CullFace.NONE);
            Group world = new Group(mesh, new AmbientLight(Color.WHITE));
            show(world, SceneAntialiasing.DISABLED, "A square mesh of two triangles");
            expect("the front of the mesh", 0, 0, Color.LIME);
            fx(() -> mesh.setRotationAxis(Rotate.Y_AXIS));
            fx(() -> mesh.setRotate(180));
            TestKit.pause(400);
            expect("the back of the mesh, without culling", 0, 0, Color.LIME);
            fx(() -> mesh.setCullFace(CullFace.BACK));
            TestKit.pause(400);
            expect("the back of the mesh, culled", 0, 0, Color.BLACK);
            return "front, back and culled back";
        });
        suite.auto("Antialiasing", "The edges of a tilted box blend with BALANCED, not without it", () -> {
            require3D();
            int plain = edgeBlends(SceneAntialiasing.DISABLED);
            int smooth = edgeBlends(SceneAntialiasing.BALANCED);
            TestKit.check(smooth > plain * 2 + 10, "the BALANCED edges have " + smooth + " blended pixels, "
                    + "without antialiasing " + plain);
            return smooth + " blended edge pixels, " + plain + " without antialiasing";
        });
        suite.auto("Frame rate: 1000 boxes", "Rotates 1000 lit boxes for 3 s", () -> {
            require3D();
            Group boxes = new Group();
            Random random = new Random(42);
            for (int i = 0; i < 1000; i++) {
                Box b = new Box(14, 14, 14);
                b.setMaterial(material(Color.hsb(random.nextDouble() * 360, 0.7, 1)));
                b.setTranslateX(random.nextGaussian() * 110);
                b.setTranslateY(random.nextGaussian() * 80);
                b.setTranslateZ(random.nextGaussian() * 110);
                boxes.getChildren().add(b);
            }
            PointLight light = new PointLight(Color.WHITE);
            light.setTranslateZ(-500);
            return frameRate(new Group(boxes, light, new AmbientLight(Color.gray(0.3))), boxes,
                    "1000 boxes, a point light and an ambient light");
        });
        suite.auto("Frame rate: large mesh", "Rotates a sphere of about 360 000 triangles for 3 s", () -> {
            require3D();
            Sphere sphere = new Sphere(130, 600);
            sphere.setMaterial(material(Color.web("#38bdf8")));
            PointLight light = new PointLight(Color.WHITE);
            light.setTranslateX(-400);
            light.setTranslateZ(-400);
            Group spin = new Group(sphere);
            return frameRate(new Group(spin, light), spin, "A sphere of 600 divisions");
        });
        suite.auto("Frame rate: three lights", "Rotates 300 spheres under three colored point lights for 3 s", () -> {
            require3D();
            Group spheres = new Group();
            Random random = new Random(7);
            for (int i = 0; i < 300; i++) {
                Sphere s = new Sphere(9, 24);
                s.setMaterial(material(Color.WHITE));
                s.setTranslateX(random.nextGaussian() * 110);
                s.setTranslateY(random.nextGaussian() * 80);
                s.setTranslateZ(random.nextGaussian() * 110);
                spheres.getChildren().add(s);
            }
            Group world = new Group(spheres);
            Color[] colors = {Color.RED, Color.LIME, Color.BLUE};
            for (int i = 0; i < 3; i++) {
                PointLight light = new PointLight(colors[i]);
                light.setTranslateX(Math.cos(i * 2.1) * 400);
                light.setTranslateY(Math.sin(i * 2.1) * 300);
                light.setTranslateZ(-400);
                world.getChildren().add(light);
            }
            return frameRate(world, spheres, "300 spheres, red, green and blue point lights");
        });
    }

    /**
     * A 2 × 2 checker of red and white squares.
     */
    private static Image checker() {
        int n = 64;
        WritableImage image = new WritableImage(n, n);
        PixelWriter pw = image.getPixelWriter();
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                boolean red = (x < n / 2) == (y < n / 2);
                pw.setColor(x, y, red ? Color.RED : Color.WHITE);
            }
        }
        return image;
    }

    /**
     * A square of two triangles in the XY plane, facing the camera.
     */
    private static TriangleMesh square(float size) {
        float h = size / 2;
        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(-h, -h, 0, h, -h, 0, h, h, 0, -h, h, 0);
        mesh.getTexCoords().addAll(0, 0, 1, 0, 1, 1, 0, 1);
        mesh.getFaces().addAll(0, 0, 2, 2, 1, 1, 0, 0, 3, 3, 2, 2);
        return mesh;
    }

    /**
     * Counts the pixels along the edges of a tilted red box that are neither
     * the background nor the box.
     */
    private int edgeBlends(SceneAntialiasing aa) {
        Box box = new Box(180, 180, 10);
        box.setMaterial(material(Color.RED));
        box.setRotate(22);
        show(new Group(box, new AmbientLight(Color.WHITE)), aa, "A tilted box, antialiasing " + aa);
        WritableImage image = fx(() -> {
            Bounds b = TestKit.screenBounds(viewport);
            return TestKit.robot().getScreenCapture(null, new Rectangle2D(b.getMinX(), b.getMinY(), b.getWidth(),
                    b.getHeight()), false);
        });
        PixelReader pr = image.getPixelReader();
        int blends = 0;
        for (int y = 2; y < image.getHeight() - 2; y++) {
            for (int x = 2; x < image.getWidth() - 2; x++) {
                Color c = pr.getColor(x, y);
                double r = c.getRed(), g = c.getGreen(), bl = c.getBlue();
                // Between black and red: some red, no green or blue
                if (g < 0.1 && bl < 0.1 && r > 0.15 && r < 0.85) {
                    blends++;
                }
            }
        }
        return blends;
    }

    /**
     * Shows a scene, rotates a node of it for a while and measures the
     * pulses, which wait for the rendering.
     */
    private String frameRate(Group world, Node spin, String info) {
        show(world, SceneAntialiasing.DISABLED, info);
        long[] frames = {0};
        long[] first = {0};
        long[] last = {0};
        AnimationTimer timer = fx(() -> {
            AnimationTimer t = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (first[0] == 0) {
                        first[0] = now;
                    }
                    last[0] = now;
                    frames[0]++;
                    spin.setRotationAxis(new javafx.geometry.Point3D(0.3, 1, 0.1));
                    spin.setRotate(spin.getRotate() + 1.5);
                }
            };
            t.start();
            return t;
        });
        TestKit.pause(MEASURE_MS);
        fx(timer::stop);
        double seconds = fx(() -> (last[0] - first[0]) / 1e9);
        long count = fx(() -> frames[0]);
        double fps = seconds > 0 ? (count - 1) / seconds : 0;
        TestKit.check(fps >= MIN_FPS, String.format(Locale.ROOT, "%.1f frames per second, expected at least %.0f",
                fps, MIN_FPS));
        return String.format(Locale.ROOT, "%.1f frames per second", fps);
    }

    private static <T> T fx(Supplier<T> s) {
        return TestKit.fx(s);
    }

    private static void fx(Runnable r) {
        TestKit.fx(r);
    }

}
