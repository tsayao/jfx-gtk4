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
import javafx.application.ColorScheme;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.geometry.VPos;
import javafx.scene.AmbientLight;
import javafx.scene.CacheHint;
import javafx.scene.DirectionalLight;
import javafx.scene.Group;
import javafx.scene.LightBase;
import javafx.scene.Node;
import javafx.scene.ParallelCamera;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SnapshotParameters;
import javafx.scene.SpotLight;
import javafx.scene.SubScene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.effect.Blend;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.ColorInput;
import javafx.scene.effect.DisplacementMap;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Effect;
import javafx.scene.effect.FloatMap;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.effect.Glow;
import javafx.scene.effect.ImageInput;
import javafx.scene.effect.InnerShadow;
import javafx.scene.effect.Light;
import javafx.scene.effect.Lighting;
import javafx.scene.effect.MotionBlur;
import javafx.scene.effect.PerspectiveTransform;
import javafx.scene.effect.Reflection;
import javafx.scene.effect.SepiaTone;
import javafx.scene.effect.Shadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Box;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.CubicCurveTo;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.Line;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.QuadCurve;
import javafx.scene.shape.QuadCurveTo;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.text.TextFlow;
import javafx.scene.transform.Rotate;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.DoubleConsumer;

/**
 * A laboratory for the Prism renderer: tabs of shapes, paints, text, images,
 * effects, blending and clips, canvases, 3D, CSS and controls, and a stress
 * test, all animated, and tests after the rendering system tests of OpenJFX:
 * colors read back from the screen, PixelBuffer updates, LCD text, snapshots,
 * and every tab rendered.
 */
public class RenderLab {

    private static final String CSS = """
        .root {
            -fx-font-family: "Adwaita Sans", "Cantarell", "System";
            -fx-font-size: 13px;
            -fx-base: #171c2b;
            -fx-background: transparent;
            -fx-control-inner-background: #10141f;
            -fx-accent: #f472b6;
            -fx-focus-color: #38bdf8;
            -fx-faint-focus-color: #38bdf822;
            -fx-text-fill: #eef2ff;
        }
        .label, .check-box, .toggle-button { -fx-text-fill: #eef2ff; }
        .muted { -fx-text-fill: #94a3b8; }
        .mono { -fx-font-family: "Adwaita Mono", "Monospace"; }
        .brand { -fx-font-size: 15px; -fx-font-weight: bold; }
        .pill {
            -fx-background-color: rgba(255, 255, 255, 0.07);
            -fx-background-radius: 999;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 999;
            -fx-padding: 5 12 5 12;
            -fx-spacing: 7;
            -fx-alignment: center;
        }
        .pill .label { -fx-font-size: 12px; }
        .card {
            -fx-background-color: rgba(8, 11, 20, 0.62);
            -fx-background-radius: 14;
            -fx-border-color: rgba(255, 255, 255, 0.12);
            -fx-border-radius: 14;
            -fx-padding: 10;
            -fx-spacing: 8;
        }
        .card-title { -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #cbd5e1; }
        .card-note { -fx-font-size: 10px; -fx-text-fill: #94a3b8; }
        .tab-pane > .tab-header-area > .headers-region > .tab {
            -fx-background-color: transparent;
            -fx-background-radius: 999;
            -fx-padding: 4 12 4 12;
        }
        .tab-pane > .tab-header-area > .headers-region > .tab:selected {
            -fx-background-color: rgba(244, 114, 182, 0.25);
        }
        .tab-pane > .tab-header-area > .tab-header-background { -fx-background-color: transparent; }
        .tab-pane > .tab-header-area { -fx-padding: 4 12 4 12; }
        .tab .tab-label { -fx-text-fill: #eef2ff; }
        .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; }
        .css-a {
            -fx-background-color: linear-gradient(to bottom right, #f472b6, #8b5cf6), rgba(0,0,0,0.25);
            -fx-background-insets: 0, 10;
            -fx-background-radius: 24, 14;
            -fx-border-color: #fde68a;
            -fx-border-width: 3;
            -fx-border-style: segments(10, 6) line-cap round;
            -fx-border-radius: 24;
        }
        .css-b {
            -fx-background-color: radial-gradient(center 30% 30%, radius 80%, #38bdf8, #0f172a);
            -fx-background-radius: 50% 10% 50% 10%;
            -fx-border-color: #22d3ee #a3e635 #f472b6 #fbbf24;
            -fx-border-width: 4 8 4 8;
            -fx-border-radius: 50% 10% 50% 10%;
            -fx-effect: dropshadow(gaussian, #22d3ee, 18, 0.2, 0, 0);
        }
        .css-c {
            -fx-background-color: repeating-image-pattern("@TEXTURE@"), rgba(15,23,42,0.6);
            -fx-background-radius: 16;
            -fx-border-color: white;
            -fx-border-style: dotted;
            -fx-border-width: 2;
            -fx-border-radius: 16;
        }
        """;

    private static final double CARD_W = 250;
    private static final double CARD_H = 176;

    private final Map<Tab, List<DoubleConsumer>> ticks = new HashMap<>();
    private final TabPane tabs = new TabPane();
    private final CheckBox animate = new CheckBox("Animate");
    private final CheckBox cycle = new CheckBox("Cycle tabs");
    private final Label fpsLabel = new Label("— fps");
    private final Region fill = new Region();
    private final Random random = new Random(7);
    private Tab building;
    private boolean backdrop;
    private Stage commandStage;
    private Image texture;
    private Image photo;
    private double time;
    private double lastCycle;

    /**
     * The lab, for its tab in the tests app.
     */
    Region content(Stage stage) {
        commandStage = stage;
        backdrop = Boolean.getBoolean("glass.gtk4.backdrop.active");
        texture = checker(64, Color.web("#f472b6"), Color.web("#1e293b"));
        photo = synthetic(256, 256);

        animate.setSelected(true);
        cycle.setSelected(Boolean.getBoolean("render.cycle"));

        addTab("Shapes", this::shapes);
        addTab("Paints", this::paints);
        addTab("Text", this::text);
        addTab("Images", this::images);
        addTab("Effects", this::effects);
        addTab("Blend and clip", this::blending);
        addTab("Canvas", this::canvas);
        addTab("3D", this::scene3d);
        addTab("CSS and controls", this::controls);
        addTab("Stress", this::stress);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getSelectionModel().select(Integer.getInteger("render.tab", 0));
        defineTests();
        suite.setBeforeRun(() -> {
            stage.toFront();
            stage.requestFocus();
        });
        suite.setAfterEach(this::closeTestStage);

        fpsLabel.getStyleClass().add("mono");
        HBox fps = new HBox(fpsLabel);
        fps.getStyleClass().add("pill");
        HBox pipeline = new HBox(new Label(pipelineName() + " · 3D "
                + (Platform.isSupported(ConditionalFeature.SCENE3D) ? "yes" : "no")));
        pipeline.getStyleClass().add("pill");
        HBox bar = new HBox(10, fps, pipeline, animate, cycle);
        bar.setAlignment(Pos.CENTER_LEFT);
        VBox showcase = new VBox(10, bar, tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        showcase.setMinSize(0, 0);
        VBox tests = new VBox(TestKit.grow(testsCard()));
        tests.setMinSize(0, 0);
        tests.setPrefWidth(520);
        tests.setMinWidth(420);
        HBox columns = new HBox(14, showcase, tests);
        HBox.setHgrow(showcase, Priority.ALWAYS);
        columns.setPadding(new Insets(4, 18, 18, 18));
        // Short of room, the cards are cut at the bottom, the header stays
        columns.setMinHeight(0);
        columns.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.RIGHT && e.isControlDown()) {
                tabs.getSelectionModel().selectNext();
            } else if (e.getCode() == KeyCode.LEFT && e.isControlDown()) {
                tabs.getSelectionModel().selectPrevious();
            }
        });
        loop();
        return columns;
    }

    /**
     * The stylesheet of the lab, with its texture, once the content is made.
     */
    String stylesheet() {
        return "data:text/css;base64," + Base64.getEncoder().encodeToString(
                CSS.replace("@TEXTURE@", dataUrl(texture)).getBytes(StandardCharsets.UTF_8));
    }

    private void addTab(String name, Runnable content) {
        building = new Tab(name);
        ticks.put(building, new ArrayList<>());
        FlowPane flow = new FlowPane(14, 14);
        flow.setPadding(new Insets(14));
        building.setUserData(flow);
        content.run();
        ScrollPane scroll = new ScrollPane(flow);
        scroll.setFitToWidth(true);
        building.setContent(scroll);
        tabs.getTabs().add(building);
    }

    private void card(String title, String note, Node content) {
        Label t = new Label(title);
        t.getStyleClass().add("card-title");
        Label n = new Label(note);
        n.getStyleClass().add("card-note");
        n.setWrapText(true);
        StackPane stage = new StackPane(content);
        stage.setMinSize(CARD_W, CARD_H);
        stage.setPrefSize(CARD_W, CARD_H);
        stage.setMaxSize(CARD_W, CARD_H);
        stage.setClip(new Rectangle(CARD_W, CARD_H));
        VBox box = new VBox(t, stage, n);
        box.getStyleClass().add("card");
        box.setPrefWidth(CARD_W + 20);
        ((FlowPane) building.getUserData()).getChildren().add(box);
    }

    private void wide(String title, String note, Node content, double w, double h) {
        Label t = new Label(title);
        t.getStyleClass().add("card-title");
        Label n = new Label(note);
        n.getStyleClass().add("card-note");
        StackPane stage = new StackPane(content);
        stage.setMinSize(w, h);
        stage.setPrefSize(w, h);
        stage.setMaxSize(w, h);
        stage.setClip(new Rectangle(w, h));
        VBox box = new VBox(t, stage, n);
        box.getStyleClass().add("card");
        ((FlowPane) building.getUserData()).getChildren().add(box);
    }

    private void tick(DoubleConsumer consumer) {
        ticks.get(building).add(consumer);
    }

    private void loop() {
        new AnimationTimer() {
            private long last;
            private long fpsStart;
            private int frames;

            @Override
            public void handle(long now) {
                if (last == 0) {
                    last = now;
                    fpsStart = now;
                }
                double dt = (now - last) / 1e9;
                last = now;
                frames++;
                if (now - fpsStart > 500_000_000L) {
                    fpsLabel.setText(String.format("%.0f fps", frames * 1e9 / (now - fpsStart)));
                    frames = 0;
                    fpsStart = now;
                }
                if (animate.isSelected()) {
                    time += dt;
                }
                if (cycle.isSelected() && time - lastCycle > 5) {
                    lastCycle = time;
                    int next = (tabs.getSelectionModel().getSelectedIndex() + 1) % tabs.getTabs().size();
                    tabs.getSelectionModel().select(next);
                }
                Tab selected = tabs.getSelectionModel().getSelectedItem();
                for (DoubleConsumer c : ticks.getOrDefault(selected, List.of())) {
                    c.accept(time);
                }
            }
        }.start();
    }

    private static String pipelineName() {
        String name = com.sun.prism.GraphicsPipeline.getPipeline().getClass().getSimpleName();
        return switch (name) {
            case "VKPipeline" -> "Vulkan";
            case "ES2Pipeline" -> "OpenGL ES";
            case "SWPipeline" -> "Software";
            default -> name;
        };
    }

    private void shapes() {
        Rectangle rect = new Rectangle(150, 100, Color.web("#38bdf8", 0.35));
        rect.setArcWidth(36);
        rect.setArcHeight(36);
        rect.setStroke(Color.web("#38bdf8"));
        rect.setStrokeWidth(4);
        rect.getStrokeDashArray().addAll(18.0, 8.0, 2.0, 8.0);
        rect.setStrokeLineCap(StrokeLineCap.ROUND);
        tick(t -> rect.setStrokeDashOffset(-t * 40));
        card("Rounded rectangle", "Animated dashes with round caps", rect);

        HBox types = new HBox(18);
        types.setAlignment(Pos.CENTER);
        for (StrokeType type : StrokeType.values()) {
            Circle c = new Circle(30, Color.web("#a78bfa", 0.5));
            c.setStroke(Color.web("#f472b6", 0.8));
            c.setStrokeWidth(12);
            c.setStrokeType(type);
            Circle guide = new Circle(30, Color.TRANSPARENT);
            guide.setStroke(Color.WHITE);
            types.getChildren().add(new StackPane(c, guide));
        }
        card("StrokeType", "INSIDE · CENTERED · OUTSIDE, a white line marks the outline", types);

        Group arcs = new Group();
        ArcType[] arcTypes = ArcType.values();
        for (int i = 0; i < arcTypes.length; i++) {
            Arc arc = new Arc(45 + i * 80, 70, 34, 34, 30, 250);
            arc.setType(arcTypes[i]);
            arc.setFill(Color.hsb(40 + i * 60, 0.7, 0.9, 0.6));
            arc.setStroke(Color.WHITE);
            arc.setStrokeWidth(2);
            int k = i;
            tick(t -> arc.setLength(200 + 100 * Math.sin(t * 1.5 + k)));
            arcs.getChildren().add(arc);
        }
        Ellipse ellipse = new Ellipse(120, 145, 100, 18);
        ellipse.setFill(Color.web("#22d3ee", 0.25));
        ellipse.setStroke(Color.web("#22d3ee"));
        arcs.getChildren().add(ellipse);
        card("Arcs and ellipse", "OPEN · CHORD · ROUND with animated length", arcs);

        Polygon star = new Polygon();
        for (int i = 0; i < 5; i++) {
            double a = -Math.PI / 2 + i * 4 * Math.PI / 5;
            star.getPoints().addAll(Math.cos(a) * 70, Math.sin(a) * 70);
        }
        star.setFill(Color.web("#fbbf24", 0.75));
        star.setStroke(Color.web("#fde68a"));
        star.setStrokeWidth(2);
        Path evenOdd = new Path(new MoveTo(0, 0), new LineTo(120, 0), new LineTo(120, 120), new LineTo(0, 120), new ClosePath(),
                new MoveTo(30, 30), new LineTo(90, 30), new LineTo(90, 90), new LineTo(30, 90), new ClosePath());
        evenOdd.setFillRule(FillRule.EVEN_ODD);
        evenOdd.setFill(Color.web("#34d399", 0.7));
        evenOdd.setScaleX(0.6);
        evenOdd.setScaleY(0.6);
        HBox fillRules = new HBox(10, star, evenOdd);
        fillRules.setAlignment(Pos.CENTER);
        tick(t -> star.setRotate(t * 30));
        card("Fill rules", "Star polygon (NON_ZERO) and EVEN_ODD path", fillRules);

        Group joins = new Group();
        StrokeLineJoin[] js = StrokeLineJoin.values();
        StrokeLineCap[] caps = StrokeLineCap.values();
        for (int i = 0; i < 3; i++) {
            Polyline p = new Polyline(10 + i * 80, 140, 40 + i * 80, 40, 70 + i * 80, 140);
            p.setStroke(Color.hsb(190 + i * 50, 0.7, 1));
            p.setStrokeWidth(16);
            p.setStrokeLineJoin(js[i]);
            p.setStrokeLineCap(caps[i]);
            p.setFill(null);
            joins.getChildren().add(p);
        }
        card("Joins and caps", "MITER/SQUARE · BEVEL/BUTT · ROUND/ROUND", joins);

        Path path = new Path(new MoveTo(10, 120), new CubicCurveTo(40, 0, 120, 200, 160, 60),
                new QuadCurveTo(190, 10, 230, 80), new ArcTo(40, 30, 0, 150, 160, true, false), new ClosePath());
        path.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#f472b6", 0.6)), new Stop(1, Color.web("#38bdf8", 0.6))));
        path.setStroke(Color.WHITE);
        path.setStrokeWidth(1.5);
        card("Path", "Cubic, quadratic and ArcTo", new Group(path));

        SVGPath heart = new SVGPath();
        heart.setContent("M 60 108 C 20 80 0 55 0 32 C 0 12 16 0 32 0 C 44 0 54 6 60 16 C 66 6 76 0 88 0 "
                + "C 104 0 120 12 120 32 C 120 55 100 80 60 108 Z");
        heart.setFill(new RadialGradient(0, 0, 0.35, 0.3, 0.8, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#fecdd3")), new Stop(1, Color.web("#e11d48"))));
        tick(t -> {
            double s = 1 + 0.08 * Math.pow(Math.max(0, Math.sin(t * 6)), 8);
            heart.setScaleX(s);
            heart.setScaleY(s);
        });
        card("SVGPath", "Heart with radial gradient and scale", heart);

        QuadCurve quad = new QuadCurve(10, 140, 120, -20, 230, 140);
        quad.setFill(null);
        quad.setStroke(Color.web("#a3e635"));
        quad.setStrokeWidth(3);
        CubicCurve cubic = new CubicCurve(10, 80, 80, 0, 160, 170, 230, 80);
        cubic.setFill(Color.web("#a3e635", 0.15));
        cubic.setStroke(Color.web("#facc15"));
        cubic.setStrokeWidth(3);
        tick(t -> {
            cubic.setControlY1(80 - 80 * Math.sin(t));
            cubic.setControlY2(80 + 80 * Math.sin(t));
        });
        card("Curves", "QuadCurve and CubicCurve with animated controls", new Group(quad, cubic));

        Shape a = new Circle(55, 80, 50);
        Shape b = new Rectangle(55, 40, 100, 80);
        Shape union = Shape.union(a, b);
        Shape sub = Shape.subtract(new Circle(55, 80, 50), new Rectangle(55, 40, 100, 80));
        Shape inter = Shape.intersect(new Circle(55, 80, 50), new Rectangle(55, 40, 100, 80));
        union.setFill(Color.web("#38bdf8", 0.6));
        sub.setFill(Color.web("#f472b6", 0.8));
        inter.setFill(Color.web("#fbbf24", 0.8));
        union.setScaleX(0.6);
        union.setScaleY(0.6);
        sub.setScaleX(0.6);
        sub.setScaleY(0.6);
        inter.setScaleX(0.6);
        inter.setScaleY(0.6);
        HBox ops = new HBox(-30, new Group(union), new Group(sub), new Group(inter));
        ops.setAlignment(Pos.CENTER);
        card("Shape operations", "union · subtract · intersect", ops);

        Group hair = new Group();
        double[] widths = {0.25, 0.5, 1, 1.5, 2, 3};
        for (int i = 0; i < widths.length; i++) {
            Line l = new Line(10, 15 + i * 26, 230, 25 + i * 26);
            l.setStroke(Color.WHITE);
            l.setStrokeWidth(widths[i]);
            Line raw = new Line(10, 22 + i * 26, 230, 32 + i * 26);
            raw.setStroke(Color.web("#f472b6"));
            raw.setStrokeWidth(widths[i]);
            raw.setSmooth(false);
            hair.getChildren().addAll(l, raw);
        }
        card("Hairlines", "0.25 to 3 px, white with antialiasing, pink without", hair);
    }

    private void paints() {
        CycleMethod[] cycles = CycleMethod.values();
        VBox linear = new VBox(8);
        for (CycleMethod c : cycles) {
            Rectangle r = new Rectangle(230, 46);
            r.setArcWidth(12);
            r.setArcHeight(12);
            r.setFill(new LinearGradient(0.35, 0, 0.65, 0, true, c,
                    new Stop(0, Color.web("#f472b6")), new Stop(0.5, Color.web("#fde68a")), new Stop(1, Color.web("#38bdf8"))));
            linear.getChildren().add(r);
        }
        linear.setAlignment(Pos.CENTER);
        card("LinearGradient", "NO_CYCLE · REFLECT · REPEAT", linear);

        Rectangle radial = new Rectangle(230, 160);
        tick(t -> radial.setFill(new RadialGradient(Math.toDegrees(t) % 360, 0.6, 0.5, 0.5, 0.25, true, CycleMethod.REFLECT,
                new Stop(0, Color.web("#0ea5e9")), new Stop(0.5, Color.web("#f0abfc")), new Stop(1, Color.web("#1e1b4b")))));
        card("RadialGradient", "Animated off-center focus with REFLECT", radial);

        Rectangle pattern = new Rectangle(230, 160);
        tick(t -> pattern.setFill(new ImagePattern(texture, t * 20 % 64, t * 10 % 64, 64, 64, false)));
        card("ImagePattern", "Scrolling 64 px tile", pattern);

        Group alpha = new Group();
        Color[] colors = {Color.web("#ef4444"), Color.web("#22c55e"), Color.web("#3b82f6")};
        for (int i = 0; i < 3; i++) {
            Circle c = new Circle(60, colors[i].deriveColor(0, 1, 1, 0.55));
            int k = i;
            tick(t -> {
                c.setCenterX(120 + 40 * Math.cos(t + k * 2.09));
                c.setCenterY(85 + 40 * Math.sin(t + k * 2.09));
            });
            alpha.getChildren().add(c);
        }
        card("Transparency", "Three overlapping colors at 55%", alpha);

        HBox swatches = new HBox();
        for (int i = 0; i < 24; i++) {
            Rectangle r = new Rectangle(10, 160);
            int k = i;
            tick(t -> r.setFill(Color.hsb((k * 15 + t * 60) % 360, 0.85, 0.95)));
            swatches.getChildren().add(r);
        }
        card("Solid colors", "Rotating HSB bands", swatches);

        Text grad = new Text("GRADIENT");
        grad.setFont(Font.font("System", FontWeight.BLACK, 36));
        grad.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#facc15")), new Stop(1, Color.web("#ec4899"))));
        grad.setStroke(new LinearGradient(0, 0, 1, 0, true, CycleMethod.REPEAT,
                new Stop(0, Color.WHITE), new Stop(0.1, Color.web("#38bdf8"))));
        grad.setStrokeWidth(1.5);
        card("Text paint", "Gradient fill and stroke", grad);
    }

    private void text() {
        VBox sizes = new VBox(0);
        for (int s : new int[] {8, 10, 12, 16, 22, 30, 40}) {
            Text t = new Text(s + "px Café naïve Ångström");
            t.setFont(Font.font(s));
            t.setFill(Color.WHITE);
            sizes.getChildren().add(t);
        }
        card("Sizes", "8 to 40 px with accents", sizes);

        VBox smoothing = new VBox(6);
        for (FontSmoothingType type : FontSmoothingType.values()) {
            Text t = new Text(type + ": The quick brown fox jumps over the lazy dog");
            t.setFontSmoothingType(type);
            t.setFont(Font.font(12));
            t.setFill(Color.WHITE);
            t.setWrappingWidth(230);
            smoothing.getChildren().add(t);
            Text dark = new Text(type + ": dark text on a light background");
            dark.setFontSmoothingType(type);
            dark.setFill(Color.BLACK);
            StackPane light = new StackPane(dark);
            light.setBackground(new Background(new BackgroundFill(Color.web("#f1f5f9"), null, null)));
            smoothing.getChildren().add(light);
        }
        card("Smoothing", "GRAY and LCD on dark and light backgrounds", smoothing);

        VBox styles = new VBox(4);
        Text bold = new Text("Bold");
        bold.setFont(Font.font("System", FontWeight.BOLD, 20));
        Text italic = new Text("Italic");
        italic.setFont(Font.font("System", FontPosture.ITALIC, 20));
        Text under = new Text("Underline");
        under.setUnderline(true);
        under.setFont(Font.font(20));
        Text strike = new Text("Strikethrough");
        strike.setStrikethrough(true);
        strike.setFont(Font.font(20));
        Text mono = new Text("Monospace 0O1lI");
        mono.setFont(Font.font("Monospace", 18));
        for (Text t : List.of(bold, italic, under, strike, mono)) {
            t.setFill(Color.WHITE);
        }
        styles.getChildren().addAll(bold, italic, under, strike, mono);
        card("Styles", "Weight, posture, decorations and monospace", styles);

        Text spin = new Text("Rotation ↻");
        spin.setFont(Font.font("System", FontWeight.BOLD, 28));
        spin.setFill(Color.web("#fde68a"));
        tick(t -> {
            spin.setRotate(t * 45);
            double s = 0.6 + 0.5 * (1 + Math.sin(t * 1.3)) / 2;
            spin.setScaleX(s);
            spin.setScaleY(s);
        });
        card("Transforms", "Text rotating and scaling", spin);

        TextFlow flow = new TextFlow();
        Text t1 = new Text("TextFlow ");
        t1.setFill(Color.web("#38bdf8"));
        t1.setFont(Font.font("System", FontWeight.BOLD, 16));
        Text t2 = new Text("mixes styles, emoji 🎨🚀✨, ");
        t2.setFill(Color.WHITE);
        Text t3 = new Text("עברית ");
        t3.setFill(Color.web("#f472b6"));
        Text t4 = new Text("العربية ");
        t4.setFill(Color.web("#a3e635"));
        Text t5 = new Text("日本語 中文 한국어 ");
        t5.setFill(Color.web("#fbbf24"));
        Text t6 = new Text("and wraps lines automatically within the width of the card.");
        t6.setFill(Color.web("#cbd5e1"));
        flow.getChildren().addAll(t1, t2, t3, t4, t5, t6);
        flow.setPrefWidth(230);
        flow.setTextAlignment(TextAlignment.JUSTIFY);
        card("TextFlow", "Color emoji, bidi, CJK and justified", flow);

        Text many = new Text();
        many.setFill(Color.web("#e2e8f0"));
        many.setFont(Font.font("Monospace", 9));
        StringBuilder sb = new StringBuilder();
        tick(t -> {
            sb.setLength(0);
            for (int i = 0; i < 14; i++) {
                for (int j = 0; j < 38; j++) {
                    sb.append((char) ('!' + (int) ((t * 7 + i * 13 + j * 7) % 90)));
                }
                sb.append('\n');
            }
            many.setText(sb.toString());
        });
        card("Dynamic glyphs", "Text changed every frame (glyph cache)", many);
    }

    private void images() {
        ImageView nearest = new ImageView(checker(8, Color.WHITE, Color.web("#0ea5e9")));
        nearest.setSmooth(false);
        nearest.setFitWidth(110);
        nearest.setFitHeight(110);
        ImageView smooth = new ImageView(checker(8, Color.WHITE, Color.web("#0ea5e9")));
        smooth.setSmooth(true);
        smooth.setFitWidth(110);
        smooth.setFitHeight(110);
        HBox filters = new HBox(10, nearest, smooth);
        filters.setAlignment(Pos.CENTER);
        card("Filtering", "A 16×16 image scaled up: nearest neighbor and linear", filters);

        ImageView viewport = new ImageView(photo);
        viewport.setFitWidth(230);
        viewport.setFitHeight(160);
        tick(t -> viewport.setViewport(new Rectangle2D(64 + 60 * Math.cos(t * 0.7), 64 + 60 * Math.sin(t * 0.9), 128, 89)));
        card("Viewport", "Animated viewport of a 256×256 image", viewport);

        ImageView rotated = new ImageView(photo);
        rotated.setFitWidth(120);
        rotated.setPreserveRatio(true);
        rotated.setRotationAxis(new javafx.geometry.Point3D(1, 1, 0));
        tick(t -> rotated.setRotate(t * 40));
        card("Transformed image", "Rotation around a diagonal axis", rotated);

        int w = 230;
        int h = 160;
        WritableImage plasma = new WritableImage(w, h);
        int[] pixels = new int[w * h];
        tick(t -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    double v = Math.sin(x * 0.05 + t) + Math.sin(y * 0.07 - t * 1.3) + Math.sin((x + y) * 0.04 + t * 0.7);
                    int r = (int) (128 + 127 * Math.sin(v * Math.PI));
                    int g = (int) (128 + 127 * Math.sin(v * Math.PI + 2));
                    int b = (int) (128 + 127 * Math.sin(v * Math.PI + 4));
                    pixels[y * w + x] = 0xff000000 | r << 16 | g << 8 | b;
                }
            }
            plasma.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);
        });
        card("WritableImage", "Texture upload every frame with PixelWriter", new ImageView(plasma));

        IntBuffer buffer = IntBuffer.allocate(w * h);
        PixelBuffer<IntBuffer> pixelBuffer = new PixelBuffer<>(w, h, buffer, PixelFormat.getIntArgbPreInstance());
        WritableImage bufferImage = new WritableImage(pixelBuffer);
        tick(t -> pixelBuffer.updateBuffer(pb -> {
            int[] data = buffer.array();
            int cx = (int) (w / 2 + 70 * Math.cos(t));
            int cy = (int) (h / 2 + 45 * Math.sin(t * 1.4));
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int d = (x - cx) * (x - cx) + (y - cy) * (y - cy);
                    int a = Math.max(0, 255 - d / 12);
                    int c = (a * 0xf4 / 255) << 16 | (a * 0x72 / 255) << 8 | (a * 0xb6 / 255);
                    data[y * w + x] = a << 24 | c;
                }
            }
            return null;
        }));
        card("PixelBuffer", "Premultiplied NIO buffer with alpha", new ImageView(bufferImage));

        Group source = new Group();
        for (int i = 0; i < 6; i++) {
            Rectangle r = new Rectangle(40, 40, Color.hsb(i * 60, 0.8, 1));
            r.setTranslateX(Math.cos(i) * 40);
            r.setTranslateY(Math.sin(i) * 40);
            source.getChildren().add(r);
        }
        WritableImage snap = new WritableImage(120, 120);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        params.setViewport(new Rectangle2D(-60, -60, 120, 120));
        ImageView snapView = new ImageView(snap);
        snapView.setFitWidth(160);
        snapView.setFitHeight(160);
        tick(t -> {
            source.setRotate(t * 50);
            source.snapshot(params, snap);
        });
        card("Snapshot", "Render to texture every frame", snapView);

        WritableImage big = new WritableImage(4096, 4096);
        PixelWriter pw = big.getPixelWriter();
        int[] row = new int[4096];
        for (int y = 0; y < 4096; y++) {
            for (int x = 0; x < 4096; x++) {
                row[x] = ((x / 256 + y / 256) % 2 == 0 ? 0xff1e293b : 0xff334155) | ((x ^ y) & 0xff);
            }
            pw.setPixels(0, y, 4096, 1, PixelFormat.getIntArgbInstance(), row, 0, 4096);
        }
        ImageView bigView = new ImageView(big);
        bigView.setFitWidth(230);
        bigView.setFitHeight(160);
        tick(t -> {
            double z = 256 + 3800 * (1 + Math.sin(t * 0.4)) / 2;
            bigView.setViewport(new Rectangle2D((4096 - z) / 2, (4096 - z) / 2, z, z * 160 / 230));
        });
        card("Large texture", "4096×4096 with continuous zoom", bigView);
    }

    private Node sample() {
        Rectangle back = new Rectangle(150, 90, Color.web("#1e293b"));
        back.setArcWidth(18);
        back.setArcHeight(18);
        Circle c = new Circle(28, Color.web("#f472b6"));
        c.setTranslateX(-35);
        Text t = new Text("FX");
        t.setFont(Font.font("System", FontWeight.BLACK, 40));
        t.setFill(Color.web("#38bdf8"));
        t.setTranslateX(30);
        return new StackPane(back, c, t);
    }

    private void effect(String title, Effect effect, DoubleConsumer animation) {
        Node node = sample();
        node.setEffect(effect);
        if (animation != null) {
            tick(animation);
        }
        card(title, effect.getClass().getSimpleName(), node);
    }

    private void effects() {
        DropShadow drop = new DropShadow(20, Color.web("#38bdf8"));
        effect("Drop shadow", drop, t -> {
            drop.setOffsetX(10 * Math.cos(t));
            drop.setOffsetY(10 * Math.sin(t));
        });
        InnerShadow inner = new InnerShadow(16, Color.BLACK);
        effect("Inner shadow", inner, t -> inner.setRadius(4 + 16 * (1 + Math.sin(t * 2)) / 2));
        GaussianBlur gauss = new GaussianBlur();
        effect("Gaussian blur", gauss, t -> gauss.setRadius(30 * (1 + Math.sin(t)) / 2));
        BoxBlur box = new BoxBlur(10, 10, 3);
        effect("Box blur", box, t -> box.setWidth(1 + 20 * (1 + Math.sin(t * 1.3)) / 2));
        MotionBlur motion = new MotionBlur(0, 25);
        effect("Motion blur", motion, t -> motion.setAngle(t * 60 % 360));
        Bloom bloom = new Bloom(0.3);
        effect("Bloom", bloom, t -> bloom.setThreshold(0.5 + 0.5 * Math.sin(t)));
        Glow glow = new Glow();
        effect("Glow", glow, t -> glow.setLevel((1 + Math.sin(t * 2)) / 2));
        effect("Reflection", new Reflection(4, 0.7, 0.6, 0), null);
        ColorAdjust adjust = new ColorAdjust();
        effect("Color adjust", adjust, t -> adjust.setHue(Math.sin(t * 0.8)));
        effect("Sepia", new SepiaTone(0.9), null);
        effect("Shadow", new Shadow(12, Color.web("#a78bfa")), null);

        Lighting distant = new Lighting(new Light.Distant(45, 45, Color.WHITE));
        distant.setSurfaceScale(4);
        effect("Distant light", distant, t -> ((Light.Distant) distant.getLight()).setAzimuth(t * 60 % 360));
        Light.Point point = new Light.Point(0, 0, 60, Color.web("#fde68a"));
        effect("Point light", new Lighting(point), t -> {
            point.setX(75 + 70 * Math.cos(t));
            point.setY(45 + 40 * Math.sin(t));
        });
        Light.Spot spot = new Light.Spot(75, 45, 120, 2, Color.web("#f472b6"));
        effect("Spot light", new Lighting(spot), t -> {
            spot.setPointsAtX(75 + 60 * Math.cos(t * 1.4));
            spot.setPointsAtY(45 + 40 * Math.sin(t * 1.4));
        });

        int mw = 160;
        int mh = 100;
        FloatMap map = new FloatMap(mw, mh);
        DisplacementMap displacement = new DisplacementMap(map);
        effect("Displacement", displacement, t -> {
            for (int x = 0; x < mw; x++) {
                float v = (float) (Math.sin(x / 8.0 + t * 3) * 0.04);
                for (int y = 0; y < mh; y++) {
                    map.setSamples(x, y, 0, v);
                }
            }
        });
        PerspectiveTransform perspective = new PerspectiveTransform();
        effect("Perspective", perspective, t -> {
            double d = 30 * Math.sin(t);
            perspective.setUlx(0);
            perspective.setUly(d);
            perspective.setUrx(150);
            perspective.setUry(-d);
            perspective.setLrx(150);
            perspective.setLry(90 + d);
            perspective.setLlx(0);
            perspective.setLly(90 - d);
        });
        ColorInput tint = new ColorInput(0, 0, 150, 90, Color.web("#22d3ee"));
        Blend blend = new Blend(BlendMode.MULTIPLY);
        blend.setTopInput(tint);
        effect("Blend with ColorInput", blend, t -> blend.setOpacity((1 + Math.sin(t)) / 2));
        Blend image = new Blend(BlendMode.SCREEN, null, new ImageInput(texture, 0, 0));
        effect("ImageInput", image, null);
        DropShadow chained = new DropShadow(18, Color.web("#f472b6"));
        GaussianBlur chainBlur = new GaussianBlur(3);
        Glow chainGlow = new Glow(0.8);
        chainGlow.setInput(chainBlur);
        chained.setInput(chainGlow);
        effect("Effect chain", chained, null);
    }

    private void blending() {
        FlowPane modes = new FlowPane(4, 4);
        modes.setPrefWrapLength(240);
        for (BlendMode mode : BlendMode.values()) {
            Rectangle base = new Rectangle(52, 34, new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#f472b6")), new Stop(1, Color.web("#38bdf8"))));
            Circle top = new Circle(14, Color.web("#fde68a", 0.9));
            top.setBlendMode(mode);
            StackPane cell = new StackPane(base, top);
            Label l = new Label(mode.name().toLowerCase().replace('_', ' '));
            l.setStyle("-fx-font-size: 7px;");
            modes.getChildren().add(new VBox(l, cell));
        }
        wide("Blend modes", "Every BlendMode over a gradient", modes, 250, 250);

        Group clipped = new Group();
        for (int i = 0; i < 12; i++) {
            Rectangle stripe = new Rectangle(i * 20, 0, 20, 176);
            stripe.setFill(Color.hsb(i * 30, 0.7, 0.95));
            clipped.getChildren().add(stripe);
        }
        Circle clip = new Circle(50);
        clipped.setClip(clip);
        tick(t -> {
            clip.setCenterX(125 + 80 * Math.cos(t));
            clip.setCenterY(88 + 40 * Math.sin(t * 2));
            clip.setRadius(40 + 20 * Math.sin(t * 3));
        });
        card("Clip", "Animated circular clip", clipped);

        Text clipText = new Text("CLIP");
        clipText.setFont(Font.font("System", FontWeight.BLACK, 80));
        ImageView behind = new ImageView(photo);
        behind.setFitWidth(250);
        behind.setFitHeight(176);
        behind.setClip(clipText);
        clipText.setX(20);
        clipText.setY(120);
        tick(t -> clipText.setX(20 + 20 * Math.sin(t)));
        card("Text clip", "An image clipped by glyphs", behind);

        Group groupAlpha = new Group(new Circle(60, 80, 50, Color.web("#ef4444")), new Circle(110, 80, 50, Color.web("#3b82f6")));
        groupAlpha.setOpacity(0.5);
        Group nodeAlpha = new Group(new Circle(60, 80, 50, Color.web("#ef4444", 0.5)), new Circle(110, 80, 50, Color.web("#3b82f6", 0.5)));
        nodeAlpha.setScaleX(0.6);
        nodeAlpha.setScaleY(0.6);
        groupAlpha.setScaleX(0.6);
        groupAlpha.setScaleY(0.6);
        HBox opacity = new HBox(0, groupAlpha, nodeAlpha);
        opacity.setAlignment(Pos.CENTER);
        card("Group opacity", "Group at 50% (no overlap) and nodes at 50%", opacity);

        Group cached = new Group();
        for (int i = 0; i < 40; i++) {
            Circle c = new Circle(random.nextDouble() * 220, random.nextDouble() * 150, 6 + random.nextDouble() * 14,
                    Color.hsb(random.nextDouble() * 360, 0.7, 1, 0.7));
            c.setEffect(new DropShadow(6, Color.BLACK));
            cached.getChildren().add(c);
        }
        cached.setCache(true);
        cached.setCacheHint(CacheHint.SPEED);
        tick(t -> {
            cached.setRotate(t * 20);
            cached.setScaleX(0.8 + 0.2 * Math.sin(t));
            cached.setScaleY(0.8 + 0.2 * Math.sin(t));
        });
        card("Cache", "Group with cached effects and CacheHint.SPEED", cached);

        StackPane cube = new StackPane();
        Rectangle face = new Rectangle(120, 120);
        face.setFill(new ImagePattern(photo));
        face.setStroke(Color.WHITE);
        cube.getChildren().add(face);
        face.setRotationAxis(Rotate.Y_AXIS);
        tick(t -> face.setRotate(t * 50));
        SubScene perspective = new SubScene(cube, CARD_W, CARD_H, true, SceneAntialiasing.DISABLED);
        perspective.setFill(Color.TRANSPARENT);
        perspective.setCamera(new PerspectiveCamera());
        cube.setPrefSize(CARD_W, CARD_H);
        card("2D node in perspective", "Rotation in Y with a perspective camera", perspective);
    }

    private void canvas() {
        Canvas canvas = new Canvas(520, 340);
        GraphicsContext g = canvas.getGraphicsContext2D();
        tick(t -> {
            g.setGlobalBlendMode(BlendMode.SRC_OVER);
            g.setGlobalAlpha(1);
            g.setEffect(null);
            g.clearRect(0, 0, 520, 340);
            g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#0f172a")), new Stop(1, Color.web("#312e81"))));
            g.fillRoundRect(0, 0, 520, 340, 24, 24);

            g.save();
            g.translate(130, 120);
            g.rotate(Math.toDegrees(t * 0.5));
            for (int i = 0; i < 12; i++) {
                g.setFill(Color.hsb(i * 30, 0.8, 1, 0.6));
                g.fillArc(-80, -80, 160, 160, i * 30, 25, ArcType.ROUND);
            }
            g.restore();

            g.setStroke(Color.web("#fde68a"));
            g.setLineWidth(4);
            g.setLineDashes(14, 6);
            g.setLineDashOffset(-t * 30);
            g.beginPath();
            g.moveTo(260, 40);
            g.bezierCurveTo(320, -20 + 60 * Math.sin(t), 420, 140, 500, 40);
            g.quadraticCurveTo(470, 160, 300, 150);
            g.arcTo(250, 160, 270, 60, 30);
            g.closePath();
            g.stroke();
            g.setLineDashes();

            g.setEffect(new DropShadow(14, Color.web("#38bdf8")));
            g.setFill(Color.web("#38bdf8"));
            g.setFont(Font.font("System", FontWeight.BOLD, 30));
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            g.fillText("Canvas", 390, 210);
            g.setEffect(null);

            g.drawImage(photo, 20, 220, 110, 110);
            g.drawImage(photo, 64, 64, 128, 128, 140, 220, 110, 110);

            g.save();
            g.beginPath();
            g.arc(330, 285, 45, 45, 0, 360);
            g.closePath();
            g.clip();
            g.setFill(new ImagePattern(texture, t * 30, 0, 32, 32, false));
            g.fillRect(280, 235, 100, 100);
            g.restore();

            g.setGlobalBlendMode(BlendMode.ADD);
            g.setGlobalAlpha(0.7);
            for (int i = 0; i < 3; i++) {
                g.setFill(Color.hsb(i * 120, 1, 1));
                g.fillOval(400 + 30 * Math.cos(t + i * 2.09), 250 + 20 * Math.sin(t + i * 2.09), 70, 70);
            }
            g.setGlobalBlendMode(BlendMode.SRC_OVER);
            g.setGlobalAlpha(1);

            PixelWriter writer = g.getPixelWriter();
            for (int i = 0; i < 400; i++) {
                int x = 20 + random.nextInt(90);
                int y = 20 + random.nextInt(50);
                writer.setArgb(x, y, 0xff000000 | random.nextInt(0xffffff));
            }
        });
        wide("GraphicsContext", "Transforms, arcs, dashes, curves, shadow, text, images, clip, ADD and PixelWriter",
                canvas, 520, 340);

        Canvas effectCanvas = new Canvas(250, 176);
        GraphicsContext e = effectCanvas.getGraphicsContext2D();
        tick(t -> {
            e.clearRect(0, 0, 250, 176);
            for (int i = 0; i < 20; i++) {
                e.setFill(Color.hsb((i * 18 + t * 40) % 360, 0.8, 1));
                e.fillOval(10 + i * 11, 70 + 50 * Math.sin(t * 2 + i * 0.4), 24, 24);
            }
            e.applyEffect(new BoxBlur(3 + 3 * Math.sin(t), 3, 2));
        });
        card("applyEffect", "Effect applied to the content of the canvas", effectCanvas);
    }

    private void scene3d() {
        PhongMaterial boxMaterial = new PhongMaterial(Color.WHITE);
        boxMaterial.setDiffuseMap(photo);
        boxMaterial.setSpecularColor(Color.WHITE);
        boxMaterial.setSpecularPower(32);
        Box box = new Box(140, 140, 140);
        box.setMaterial(boxMaterial);
        box.setTranslateX(-220);

        PhongMaterial sphereMaterial = new PhongMaterial(Color.web("#38bdf8"));
        sphereMaterial.setSpecularColor(Color.WHITE);
        sphereMaterial.setSpecularPower(64);
        sphereMaterial.setBumpMap(bumps(256));
        Sphere sphere = new Sphere(80, 96);
        sphere.setMaterial(sphereMaterial);

        PhongMaterial glowMaterial = new PhongMaterial(Color.web("#1e1b4b"));
        glowMaterial.setSelfIlluminationMap(checker(16, Color.web("#f472b6"), Color.BLACK));
        Cylinder cylinder = new Cylinder(60, 160);
        cylinder.setMaterial(glowMaterial);
        cylinder.setTranslateX(220);

        MeshView torus = new MeshView(torus(70, 24, 64, 24));
        PhongMaterial torusMaterial = new PhongMaterial(Color.WHITE);
        torusMaterial.setDiffuseMap(texture);
        torusMaterial.setSpecularColor(Color.web("#fde68a"));
        torus.setMaterial(torusMaterial);
        torus.setTranslateY(170);
        torus.setTranslateX(-110);

        Sphere wire = new Sphere(60, 24);
        wire.setDrawMode(DrawMode.LINE);
        wire.setMaterial(new PhongMaterial(Color.web("#a3e635")));
        wire.setTranslateY(170);
        wire.setTranslateX(110);

        Box plane = new Box(700, 2, 500);
        plane.setTranslateY(260);
        plane.setCullFace(CullFace.NONE);
        PhongMaterial planeMaterial = new PhongMaterial(Color.web("#334155"));
        planeMaterial.setDiffuseMap(checker(32, Color.web("#475569"), Color.web("#1e293b")));
        plane.setMaterial(planeMaterial);

        Group objects = new Group(box, sphere, cylinder, torus, wire, plane);
        AmbientLight ambient = new AmbientLight(Color.color(0.18, 0.18, 0.22));
        PointLight point = new PointLight(Color.web("#fde68a"));
        point.setConstantAttenuation(0.6);
        point.setLinearAttenuation(0.0008);
        Sphere bulb = new Sphere(8);
        PhongMaterial bulbMaterial = new PhongMaterial(Color.BLACK);
        bulbMaterial.setSelfIlluminationMap(solid(Color.web("#fde68a")));
        bulb.setMaterial(bulbMaterial);
        SpotLight spot = new SpotLight(Color.web("#f472b6"));
        spot.setTranslateY(-400);
        spot.setDirection(new javafx.geometry.Point3D(0, 1, 0));
        spot.setInnerAngle(10);
        spot.setOuterAngle(35);
        DirectionalLight directional = new DirectionalLight(Color.color(0.25, 0.3, 0.45));
        directional.setDirection(new javafx.geometry.Point3D(-1, 1, 1));
        Group world = new Group(objects, ambient, point, bulb, spot, directional);
        tick(t -> {
            box.setRotationAxis(new javafx.geometry.Point3D(1, 1, 0));
            box.setRotate(t * 40);
            sphere.setRotationAxis(Rotate.Y_AXIS);
            sphere.setRotate(t * 30);
            cylinder.setRotationAxis(Rotate.Z_AXIS);
            cylinder.setRotate(t * 25);
            torus.setRotationAxis(new javafx.geometry.Point3D(1, 0, 0.5));
            torus.setRotate(t * 60);
            wire.setRotationAxis(Rotate.Y_AXIS);
            wire.setRotate(-t * 40);
            point.setTranslateX(300 * Math.cos(t * 0.8));
            point.setTranslateZ(-200 + 250 * Math.sin(t * 0.8));
            point.setTranslateY(-120);
            bulb.setTranslateX(point.getTranslateX());
            bulb.setTranslateY(point.getTranslateY());
            bulb.setTranslateZ(point.getTranslateZ());
            spot.setTranslateX(250 * Math.sin(t * 0.6));
        });

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(1);
        camera.setFarClip(5000);
        camera.setFieldOfView(40);
        camera.setTranslateZ(-1100);
        camera.setTranslateY(-120);
        camera.setRotationAxis(Rotate.X_AXIS);
        camera.setRotate(-8);
        SubScene sub = new SubScene(world, 760, 470, true, SceneAntialiasing.BALANCED);
        sub.setFill(Color.web("#020617"));
        sub.setCamera(camera);

        HBox lights = new HBox(10);
        lights.setAlignment(Pos.CENTER_LEFT);
        for (LightBase light : List.of(ambient, point, spot, directional)) {
            ToggleButton b = new ToggleButton(light.getClass().getSimpleName());
            b.setSelected(true);
            light.lightOnProperty().bind(b.selectedProperty());
            lights.getChildren().add(b);
        }
        CheckBox wireframe = new CheckBox("Wireframe");
        wireframe.selectedProperty().addListener((o, a, b) -> {
            DrawMode mode = b ? DrawMode.LINE : DrawMode.FILL;
            box.setDrawMode(mode);
            sphere.setDrawMode(mode);
            cylinder.setDrawMode(mode);
            torus.setDrawMode(mode);
        });
        lights.getChildren().add(wireframe);
        VBox view = new VBox(8, sub, lights);
        wide("3D scene", "Textured box, bump-mapped sphere, self-illuminated cylinder, TriangleMesh torus, "
                + "wireframe, MSAA and the four lights", view, 760, 510);

        Group ortho = new Group();
        for (int i = 0; i < 6; i++) {
            Box b = new Box(30, 30, 30);
            b.setMaterial(new PhongMaterial(Color.hsb(i * 60, 0.7, 1)));
            b.setTranslateX(30 + (i % 3) * 80);
            b.setTranslateY(40 + (i / 3) * 80);
            int k = i;
            tick(t -> {
                b.setRotationAxis(new javafx.geometry.Point3D(1, 1, 1));
                b.setRotate(t * 50 + k * 30);
            });
            ortho.getChildren().add(b);
        }
        SubScene orthoScene = new SubScene(ortho, CARD_W, CARD_H, true, SceneAntialiasing.DISABLED);
        orthoScene.setCamera(new ParallelCamera());
        orthoScene.setFill(Color.web("#0f172a"));
        card("Parallel camera", "Orthographic projection with a depth buffer", orthoScene);
    }

    private void controls() {
        Region a = new Region();
        a.getStyleClass().add("css-a");
        a.setPrefSize(220, 150);
        card("CSS backgrounds and borders", "Gradient, multiple insets, radii and segmented border", a);
        Region b = new Region();
        b.getStyleClass().add("css-b");
        b.setPrefSize(200, 140);
        card("Percentage radii", "Four-color borders and CSS dropshadow", b);
        Region c = new Region();
        c.getStyleClass().add("css-c");
        c.setPrefSize(220, 150);
        card("CSS image pattern", "repeating-image-pattern with a dotted border", c);

        ProgressIndicator spinner = new ProgressIndicator();
        ProgressIndicator determinate = new ProgressIndicator();
        ProgressBar bar = new ProgressBar();
        bar.setPrefWidth(220);
        tick(t -> {
            determinate.setProgress((t * 0.2) % 1);
            bar.setProgress((t * 0.15) % 1);
        });
        HBox indicators = new HBox(16, spinner, determinate);
        indicators.setAlignment(Pos.CENTER);
        VBox progress = new VBox(14, indicators, bar);
        progress.setAlignment(Pos.CENTER);
        card("Progress", "Indeterminate and determinate indicators and a bar", progress);

        Slider slider = new Slider(0, 100, 40);
        TextField field = new TextField("Editable text");
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll("Vulkan", "OpenGL ES", "Software");
        combo.getSelectionModel().select(0);
        ColorPicker picker = new ColorPicker(Color.web("#f472b6"));
        ChoiceBox<String> choice = new ChoiceBox<>();
        choice.getItems().addAll("One", "Two", "Three");
        choice.getSelectionModel().select(1);
        Button button = new Button("Default button");
        button.setDefaultButton(true);
        VBox form = new VBox(6, slider, field, new HBox(6, combo, choice), new HBox(6, picker, button));
        form.setAlignment(Pos.CENTER_LEFT);
        card("Controls", "Popups open as child windows", form);
    }

    private void stress() {
        Pane arena = new Pane();
        arena.setPrefSize(980, 520);
        Slider count = new Slider(100, 20000, 2000);
        Label countLabel = new Label();
        ChoiceBox<String> kind = new ChoiceBox<>();
        kind.getItems().addAll("Gradient circles", "Images", "Text", "Shadowed rectangles");
        kind.getSelectionModel().select(0);
        List<double[]> state = new ArrayList<>();
        Runnable rebuild = () -> {
            arena.getChildren().clear();
            state.clear();
            int n = (int) count.getValue();
            countLabel.setText(n + " nodes");
            String mode = kind.getValue();
            for (int i = 0; i < n; i++) {
                Node node = switch (mode) {
                    case "Images" -> {
                        ImageView v = new ImageView(photo);
                        v.setFitWidth(18);
                        v.setFitHeight(18);
                        yield v;
                    }
                    case "Text" -> {
                        Text t = new Text("✦" + i);
                        t.setFill(Color.hsb(i % 360, 0.7, 1));
                        yield t;
                    }
                    case "Shadowed rectangles" -> {
                        Rectangle r = new Rectangle(14, 14, Color.hsb(i % 360, 0.7, 1));
                        r.setEffect(new DropShadow(4, Color.BLACK));
                        yield r;
                    }
                    default -> {
                        Circle c = new Circle(7, new RadialGradient(0, 0, 0.3, 0.3, 0.8, true, CycleMethod.NO_CYCLE,
                                new Stop(0, Color.WHITE), new Stop(1, Color.hsb(i % 360, 0.8, 0.9))));
                        yield c;
                    }
                };
                state.add(new double[] {random.nextDouble() * 960, random.nextDouble() * 500,
                        (random.nextDouble() - 0.5) * 160, (random.nextDouble() - 0.5) * 160});
                arena.getChildren().add(node);
            }
        };
        count.valueChangingProperty().addListener((o, was, is) -> {
            if (!is) {
                rebuild.run();
            }
        });
        count.valueProperty().addListener((o, a, b) -> {
            if (!count.isValueChanging()) {
                rebuild.run();
            }
        });
        kind.setOnAction(e -> rebuild.run());
        rebuild.run();
        double[] last = {0};
        tick(t -> {
            double dt = Math.min(0.05, t - last[0]);
            last[0] = t;
            List<Node> children = arena.getChildren();
            for (int i = 0; i < children.size(); i++) {
                double[] s = state.get(i);
                s[0] += s[2] * dt;
                s[1] += s[3] * dt;
                if (s[0] < 0 || s[0] > 960) {
                    s[2] = -s[2];
                }
                if (s[1] < 0 || s[1] > 500) {
                    s[3] = -s[3];
                }
                children.get(i).relocate(s[0], s[1]);
            }
        });
        HBox options = new HBox(12, new Label("Count"), count, countLabel, kind);
        options.setAlignment(Pos.CENTER_LEFT);
        VBox content = new VBox(8, options, arena);
        wide("Load", "Many animated nodes to measure frames per second", content, 980, 560);
    }

    private static TriangleMesh torus(float radius, float tube, int segments, int sides) {
        TriangleMesh mesh = new TriangleMesh();
        for (int i = 0; i <= segments; i++) {
            double u = 2 * Math.PI * i / segments;
            for (int j = 0; j <= sides; j++) {
                double v = 2 * Math.PI * j / sides;
                float x = (float) ((radius + tube * Math.cos(v)) * Math.cos(u));
                float y = (float) (tube * Math.sin(v));
                float z = (float) ((radius + tube * Math.cos(v)) * Math.sin(u));
                mesh.getPoints().addAll(x, y, z);
                mesh.getTexCoords().addAll((float) i / segments * 4, (float) j / sides);
            }
        }
        int stride = sides + 1;
        for (int i = 0; i < segments; i++) {
            for (int j = 0; j < sides; j++) {
                int p0 = i * stride + j;
                int p1 = (i + 1) * stride + j;
                int p2 = (i + 1) * stride + j + 1;
                int p3 = i * stride + j + 1;
                mesh.getFaces().addAll(p0, p0, p2, p2, p1, p1);
                mesh.getFaces().addAll(p0, p0, p3, p3, p2, p2);
            }
        }
        return mesh;
    }

    private static Image checker(int cell, Color a, Color b) {
        int size = cell * 2;
        WritableImage image = new WritableImage(size, size);
        PixelWriter w = image.getPixelWriter();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                w.setColor(x, y, (x / cell + y / cell) % 2 == 0 ? a : b);
            }
        }
        return image;
    }

    private static Image solid(Color color) {
        WritableImage image = new WritableImage(4, 4);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                image.getPixelWriter().setColor(x, y, color);
            }
        }
        return image;
    }

    private static Image bumps(int size) {
        WritableImage image = new WritableImage(size, size);
        PixelWriter w = image.getPixelWriter();
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double nx = Math.sin(x * 0.4) * 0.5;
                double ny = Math.sin(y * 0.4) * 0.5;
                w.setColor(x, y, Color.color(0.5 + nx * 0.5, 0.5 + ny * 0.5, 1));
            }
        }
        return image;
    }

    private static Image synthetic(int w, int h) {
        WritableImage image = new WritableImage(w, h);
        PixelWriter pw = image.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double dx = x - w * 0.5;
                double dy = y - h * 0.55;
                double r = Math.hypot(dx, dy);
                Color sky = Color.web("#0f172a").interpolate(Color.web("#7c3aed"), y / (double) h);
                Color sun = r < 48 ? Color.web("#fde68a").interpolate(Color.web("#f472b6"), r / 48) : null;
                boolean grid = y > h * 0.6 && ((x - w / 2) * (y - h * 0.6) / 40 % 16 < 1.2 || y % 16 < 1);
                Color c = sun != null ? sun : grid ? Color.web("#22d3ee") : sky;
                pw.setColor(x, y, c);
            }
        }
        return image;
    }

    private static String dataUrl(Image image) {
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        java.awt.image.BufferedImage bi = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                bi.setRGB(x, y, image.getPixelReader().getArgb(x, y));
            }
        }
        try (java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(bi, "png", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------------
    // Tests, after the rendering system tests of OpenJFX (tests/system)

    final TestKit.Suite suite = new TestKit.Suite("Render");
    private Stage testStage;
    // One bit of rounding writing a color out and one reading it back
    private static final double COMPONENT_TOLERANCE = 2.0001 / 255.0;
    private static final double[][] SRGB_COLORS = {
            {0.25, 0.5, 0.75}, {0.25, 0.75, 0.5}, {0.5, 0.25, 0.75},
            {0.5, 0.75, 0.25}, {0.75, 0.25, 0.5}, {0.75, 0.5, 0.25}};
    private static final Color PB_INIT = new Color(0.92, 0.56, 0.1, 1);
    private static final Color PB_TEST = new Color(0.2, 0.6, 0.8, 1);
    private static final int PB_SIZE = 24;
    private static final Color LCD_TRANSPARENT = Color.color(0.5, 0.5, 0.5, 0.6);

    private Node testsCard() {
        Label t = new Label("TESTS");
        t.getStyleClass().add("card-title");
        VBox box = new VBox(10, t, suite.view());
        box.getStyleClass().add("card");
        return box;
    }

    private void defineTests() {
        suite.auto("sRGB pixels", "Six colors read back with getPixelColor, within 2/255 (SRGBTest)",
                () -> srgbTest(false));
        suite.auto("sRGB screen capture", "Six colors read back with getScreenCapture, within 2/255 (SRGBTest)",
                () -> srgbTest(true));
        suite.auto("PixelBuffer IntArgbPre, direct", "Four images of one direct IntBuffer redraw after "
                + "updateBuffer (PixelBufferDrawTest)", () -> pixelBufferTest(true, true));
        suite.auto("PixelBuffer IntArgbPre, heap", "Four images of one heap IntBuffer redraw after updateBuffer "
                + "(PixelBufferDrawTest)", () -> pixelBufferTest(true, false));
        suite.auto("PixelBuffer ByteBgraPre, direct", "Four images of one direct ByteBuffer redraw after "
                + "updateBuffer (PixelBufferDrawTest)", () -> pixelBufferTest(false, true));
        suite.auto("PixelBuffer ByteBgraPre, heap", "Four images of one heap ByteBuffer redraw after updateBuffer "
                + "(PixelBufferDrawTest)", () -> pixelBufferTest(false, false));
        suite.auto("PixelBuffer off the FX thread", "updateBuffer on another thread throws IllegalStateException "
                + "(PixelBufferDrawTest)", this::pixelBufferThreadTest);
        suite.auto("LCD text, opaque", "Opaque LCD text keeps its color and has color fringes (TransparentLCDTest)",
                () -> lcdTest(true));
        suite.auto("LCD text, transparent", "Translucent LCD text falls back to gray antialiasing "
                + "(TransparentLCDTest)", () -> lcdTest(false));
        suite.auto("Snapshot matches the screen", "A snapshot of shapes, gradients and text matches the frame on "
                + "the screen (Snapshot1Test)", this::snapshotTest);
        suite.auto("Every tab renders", "Each tab of the showcase draws something, without an exception on the FX "
                + "thread", this::tabsTest);
    }

    private void closeTestStage() {
        if (testStage != null) {
            testStage.hide();
            testStage = null;
        }
    }

    /** Shows a root in an undecorated stage of the tests, and waits for it. */
    private void show(Region root, double width, double height) {
        TestKit.fx(() -> {
            closeTestStage();
            testStage = new Stage(StageStyle.UNDECORATED);
            testStage.setTitle("Render test");
            testStage.setScene(new Scene(root, width, height));
            testStage.show();
        });
        TestKit.await("the test stage shown", 5000, () -> testStage != null && testStage.isShowing()
                && root.getWidth() > 0);
        TestKit.pause(400);
    }

    private static boolean near(Color a, Color b, double tolerance) {
        return Math.abs(a.getRed() - b.getRed()) <= tolerance && Math.abs(a.getGreen() - b.getGreen()) <= tolerance
                && Math.abs(a.getBlue() - b.getBlue()) <= tolerance;
    }

    private static String rgb(Color c) {
        return String.format(Locale.ROOT, "%.3f, %.3f, %.3f", c.getRed(), c.getGreen(), c.getBlue());
    }

    private String srgbTest(boolean capture) {
        Rectangle swatch = TestKit.fx(() -> new Rectangle(200, 200));
        show(TestKit.fx(() -> new StackPane(swatch)), 200, 200);
        for (double[] c : SRGB_COLORS) {
            Color expected = Color.color(c[0], c[1], c[2]);
            TestKit.fx(() -> swatch.setFill(expected));
            Color[] seen = {null};
            try {
                TestKit.await("the color " + rgb(expected), 3000, () -> {
                    Point2D center = TestKit.center(swatch);
                    seen[0] = capture
                            ? TestKit.robot().getScreenCapture(null, center.getX(), center.getY(), 5, 5)
                                    .getPixelReader().getColor(3, 3)
                            : TestKit.robot().getPixelColor(center);
                    return near(expected, seen[0], COMPONENT_TOLERANCE);
                });
            } catch (AssertionError e) {
                throw new AssertionError("expected " + rgb(expected) + ", read " + rgb(seen[0]));
            }
        }
        return SRGB_COLORS.length + " colors within 2/255";
    }

    private String pixelBufferTest(boolean ints, boolean direct) {
        HBox row = TestKit.fx(() -> new HBox(1));
        Runnable[] update = new Runnable[1];
        TestKit.fx(() -> {
            if (ints) {
                IntBuffer shared = intBuffer(PB_SIZE, PB_SIZE, direct, PB_INIT);
                IntBuffer source = intBuffer(PB_SIZE, PB_SIZE / 2, direct, PB_TEST);
                PixelBuffer<IntBuffer> pb = new PixelBuffer<>(PB_SIZE, PB_SIZE, shared,
                        PixelFormat.getIntArgbPreInstance());
                for (int i = 0; i < 4; i++) {
                    row.getChildren().add(new ImageView(new WritableImage(pb)));
                }
                update[0] = () -> pb.updateBuffer(b -> {
                    IntBuffer dst = b.getBuffer();
                    dst.put(source);
                    source.rewind();
                    dst.rewind();
                    return new Rectangle2D(0, 0, PB_SIZE, PB_SIZE / 2);
                });
            } else {
                java.nio.ByteBuffer shared = byteBuffer(PB_SIZE, PB_SIZE, direct, PB_INIT);
                java.nio.ByteBuffer source = byteBuffer(PB_SIZE, PB_SIZE / 2, direct, PB_TEST);
                PixelBuffer<java.nio.ByteBuffer> pb = new PixelBuffer<>(PB_SIZE, PB_SIZE, shared,
                        PixelFormat.getByteBgraPreInstance());
                for (int i = 0; i < 4; i++) {
                    row.getChildren().add(new ImageView(new WritableImage(pb)));
                }
                update[0] = () -> pb.updateBuffer(b -> {
                    java.nio.ByteBuffer dst = b.getBuffer();
                    dst.put(source);
                    source.rewind();
                    dst.rewind();
                    return new Rectangle2D(0, 0, PB_SIZE, PB_SIZE / 2);
                });
            }
        });
        show(TestKit.fx(() -> new StackPane(row)), PB_SIZE * 4 + 3 + 40, PB_SIZE + 40);
        TestKit.await("the initial color in the four images", 3000, () -> imagesShow(row, PB_INIT, PB_INIT));
        TestKit.fx(update[0]);
        TestKit.await("the top halves updated in the four images", 3000, () -> imagesShow(row, PB_TEST, PB_INIT));
        return "4 images updated together";
    }

    /** Whether each image shows the colors above and below its middle. */
    private static boolean imagesShow(HBox row, Color top, Color bottom) {
        for (Node n : row.getChildren()) {
            Bounds b = n.localToScreen(n.getLayoutBounds());
            Color a = TestKit.robot().getPixelColor(b.getCenterX(), b.getMinY() + PB_SIZE * 0.45 - 2);
            Color c = TestKit.robot().getPixelColor(b.getCenterX(), b.getMinY() + PB_SIZE * 0.55 + 2);
            if (!near(a, top, 0.01) || !near(c, bottom, 0.01)) {
                return false;
            }
        }
        return true;
    }

    private static IntBuffer intBuffer(int w, int h, boolean direct, Color c) {
        int color = (int) Math.round(c.getOpacity() * 255) << 24 | (int) Math.round(c.getRed() * 255) << 16
                | (int) Math.round(c.getGreen() * 255) << 8 | (int) Math.round(c.getBlue() * 255);
        IntBuffer b = direct ? java.nio.ByteBuffer.allocateDirect(w * h * 4).order(java.nio.ByteOrder.nativeOrder())
                .asIntBuffer() : IntBuffer.allocate(w * h);
        for (int i = 0; i < w * h; i++) {
            b.put(color);
        }
        return b.rewind();
    }

    private static java.nio.ByteBuffer byteBuffer(int w, int h, boolean direct, Color c) {
        java.nio.ByteBuffer b = direct ? java.nio.ByteBuffer.allocateDirect(w * h * 4)
                : java.nio.ByteBuffer.allocate(w * h * 4);
        for (int i = 0; i < w * h; i++) {
            b.put((byte) Math.round(c.getBlue() * 255)).put((byte) Math.round(c.getGreen() * 255))
                    .put((byte) Math.round(c.getRed() * 255)).put((byte) Math.round(c.getOpacity() * 255));
        }
        return b.rewind();
    }

    private String pixelBufferThreadTest() {
        PixelBuffer<IntBuffer> pb = TestKit.fx(() -> new PixelBuffer<>(PB_SIZE, PB_SIZE,
                intBuffer(PB_SIZE, PB_SIZE, true, PB_INIT), PixelFormat.getIntArgbPreInstance()));
        try {
            pb.updateBuffer(b -> null);
        } catch (IllegalStateException e) {
            return "IllegalStateException: " + e.getMessage();
        }
        throw new AssertionError("updateBuffer ran on the test thread");
    }

    private String lcdTest(boolean opaque) {
        if (!Platform.isSupported(ConditionalFeature.SCENE3D)) {
            TestKit.skip("The software pipeline draws no LCD text");
        }
        double scale = TestKit.fx(() -> commandStage.getOutputScaleX());
        if (scale != 1) {
            TestKit.skip("The samples need an output scale of 1, not " + scale);
        }
        double a = LCD_TRANSPARENT.getOpacity();
        Color opaqueColor = Color.color(LCD_TRANSPARENT.getRed() * a + 1 - a, LCD_TRANSPARENT.getGreen() * a + 1 - a,
                LCD_TRANSPARENT.getBlue() * a + 1 - a);
        Text text = TestKit.fx(() -> {
            Text t = new Text("V");
            t.setFont(Font.font("System", FontWeight.BOLD, 36));
            t.setFill(opaque ? opaqueColor : LCD_TRANSPARENT);
            t.setFontSmoothingType(FontSmoothingType.LCD);
            t.setLayoutX(5);
            t.setLayoutY(30);
            return t;
        });
        javafx.scene.layout.Pane root = TestKit.fx(() -> {
            javafx.scene.layout.Pane p = new javafx.scene.layout.Pane(text);
            p.setStyle("-fx-background-color: white;");
            return p;
        });
        show(root, 400, 300);
        List<Color> colors = TestKit.fx(() -> {
            Point2D origin = root.localToScreen(5, 11);
            Image image = TestKit.robot().getScreenCapture(null, origin.getX(), origin.getY(), 12, 1);
            List<Color> list = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                list.add(image.getPixelReader().getColor(i, 0));
            }
            return list;
        });
        double tolerance = 2.5 / 255;
        TestKit.check(near(colors.get(0), Color.WHITE, tolerance) && near(colors.get(11), Color.WHITE, tolerance),
                "the background around the text is " + rgb(colors.get(0)) + " and " + rgb(colors.get(11)));
        TestKit.check(near(colors.get(6), opaqueColor, tolerance), "the middle of the stroke is " + rgb(colors.get(6))
                + ", not " + rgb(opaqueColor));
        long colored = colors.stream().filter(c -> c.getRed() != c.getGreen() || c.getRed() != c.getBlue()).count();
        if (opaque) {
            TestKit.check(colored > 0, "the opaque text has gray antialiasing, not LCD");
            return colored + " colored edge pixels: LCD";
        }
        TestKit.check(colored == 0, "the translucent text has " + colored + " colored pixels, not gray antialiasing");
        return "gray antialiasing";
    }

    private String snapshotTest() {
        StackPane root = TestKit.fx(() -> {
            Rectangle back = new Rectangle(300, 200, new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web("#1e3a8a")), new Stop(1, Color.web("#db2777"))));
            Circle circle = new Circle(60, Color.web("#facc15"));
            Text label = new Text("Snapshot");
            label.setFont(Font.font("System", FontWeight.BOLD, 28));
            label.setFill(Color.WHITE);
            StackPane p = new StackPane(back, circle, label);
            p.setStyle("-fx-background-color: black;");
            return p;
        });
        show(root, 300, 200);
        double[] diff = TestKit.fx(() -> {
            WritableImage snap = root.snapshot(null, null);
            Bounds b = root.localToScreen(root.getLayoutBounds());
            WritableImage screen = TestKit.robot().getScreenCapture(null, b.getMinX(), b.getMinY(), b.getWidth(),
                    b.getHeight(), false);
            int w = (int) Math.min(snap.getWidth(), screen.getWidth());
            int h = (int) Math.min(snap.getHeight(), screen.getHeight());
            double sum = 0, worst = 0;
            int n = 0;
            for (int y = 2; y < h - 2; y += 2) {
                for (int x = 2; x < w - 2; x += 2) {
                    Color c1 = snap.getPixelReader().getColor(x, y), c2 = screen.getPixelReader().getColor(x, y);
                    double d = (Math.abs(c1.getRed() - c2.getRed()) + Math.abs(c1.getGreen() - c2.getGreen())
                            + Math.abs(c1.getBlue() - c2.getBlue())) / 3;
                    sum += d;
                    worst = Math.max(worst, d);
                    n++;
                }
            }
            return new double[] {sum / n, worst};
        });
        TestKit.check(diff[0] < 0.02, String.format(Locale.ROOT, "the snapshot differs from the screen by %.3f "
                + "on average", diff[0]));
        return String.format(Locale.ROOT, "mean difference %.4f, worst %.3f", diff[0], diff[1]);
    }

    private String tabsTest() {
        Throwable[] failure = {null};
        Thread.UncaughtExceptionHandler old = TestKit.fx(() -> Thread.currentThread().getUncaughtExceptionHandler());
        TestKit.fx(() -> Thread.currentThread().setUncaughtExceptionHandler((t, e) -> failure[0] = e));
        int selected = TestKit.fx(() -> tabs.getSelectionModel().getSelectedIndex());
        List<String> blank = new ArrayList<>();
        try {
            int count = TestKit.fx(() -> tabs.getTabs().size());
            for (int i = 0; i < count; i++) {
                int index = i;
                TestKit.fx(() -> tabs.getSelectionModel().select(index));
                TestKit.pause(700);
                double spread = TestKit.fx(() -> {
                    Node content = tabs.getSelectionModel().getSelectedItem().getContent();
                    Bounds b = content.localToScreen(content.getLayoutBounds());
                    WritableImage image = TestKit.robot().getScreenCapture(null, b.getMinX(), b.getMinY(),
                            Math.min(b.getWidth(), 600), Math.min(b.getHeight(), 400));
                    double min = 1, max = 0;
                    for (int y = 0; y < image.getHeight(); y += 7) {
                        for (int x = 0; x < image.getWidth(); x += 7) {
                            double v = image.getPixelReader().getColor(x, y).getBrightness();
                            min = Math.min(min, v);
                            max = Math.max(max, v);
                        }
                    }
                    return max - min;
                });
                if (spread < 0.1) {
                    blank.add(TestKit.fx(() -> tabs.getTabs().get(index).getText()));
                }
                TestKit.check(failure[0] == null, "the FX thread threw " + failure[0]);
            }
            TestKit.check(blank.isEmpty(), "blank tabs: " + blank);
            return count + " tabs drawn";
        } finally {
            TestKit.fx(() -> {
                Thread.currentThread().setUncaughtExceptionHandler(old);
                tabs.getSelectionModel().select(selected);
            });
        }
    }
}
