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

import javafx.animation.*;
import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.*;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.DoubleConsumer;
import java.util.stream.IntStream;

/**
 * A stress test of the Prism renderer (Vulkan or OpenGL ES) in an extended
 * stage over a translucent window backdrop: a procedurally textured planet
 * with every Phong map, thousands of asteroids, all light types, a sprite
 * canvas galaxy and a gallery of animated effects.
 */
@SuppressWarnings("deprecation")
public class NebulaDemo extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private static final String CSS = """
        .root {
            -fx-font-family: "Adwaita Sans", "Cantarell", "System";
            -fx-font-size: 13px;
            -fx-base: #1d2233;
            -fx-background: transparent;
            -fx-control-inner-background: #161a28;
            -fx-accent: #8b5cf6;
            -fx-focus-color: #a78bfa;
            -fx-faint-focus-color: #a78bfa22;
            -fx-text-fill: #e8eaf6;
        }
        .label, .check-box { -fx-text-fill: #e8eaf6; }
        .muted { -fx-text-fill: #8f96b3; }
        .mono { -fx-font-family: "Adwaita Mono", "Monospace"; }
        .glass {
            -fx-background-color: rgba(8, 10, 22, 0.5);
            -fx-background-radius: 18;
            -fx-border-color: rgba(255, 255, 255, 0.16) rgba(255, 255, 255, 0.07)
                rgba(255, 255, 255, 0.04) rgba(255, 255, 255, 0.07);
            -fx-border-radius: 18;
            -fx-padding: 16;
        }
        .section { -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #8f96b3; -fx-padding: 8 0 0 0; }
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
        .brand { -fx-font-size: 15px; -fx-font-weight: bold; }
        .stat { -fx-font-size: 20px; -fx-font-weight: bold; }
        .stat-key { -fx-font-size: 10px; -fx-text-fill: #8f96b3; }
        .tile-label { -fx-font-size: 11px; -fx-text-fill: #c4c9e0; }
        .check-box > .box {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 5;
            -fx-border-color: rgba(255, 255, 255, 0.22);
            -fx-border-radius: 5;
        }
        .check-box:selected > .box {
            -fx-background-color: linear-gradient(to bottom right, #38bdf8, #a855f7);
            -fx-border-color: transparent;
        }
        .check-box:selected > .box > .mark { -fx-background-color: white; }
        .slider .track { -fx-background-color: rgba(255, 255, 255, 0.14); -fx-background-radius: 4; }
        .slider .thumb {
            -fx-background-color: white;
            -fx-effect: dropshadow(gaussian, #a855f7, 10, 0.3, 0, 0);
        }
        .button {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 9;
            -fx-text-fill: #e8eaf6;
            -fx-padding: 6 14 6 14;
            -fx-cursor: hand;
        }
        .button:hover { -fx-background-color: rgba(255, 255, 255, 0.15); }
        .button.accent {
            -fx-background-color: linear-gradient(to right, #38bdf8, #a855f7, #f472b6);
            -fx-border-color: transparent;
            -fx-text-fill: white;
            -fx-font-weight: bold;
        }
        .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-pane { -fx-padding: 0; }
        .scroll-bar { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: rgba(255, 255, 255, 0.25); -fx-background-radius: 6; }
        .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 0; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-shape: " "; -fx-padding: 0; }
        """;

    private static final String STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));

    private static final int MAX_PARTICLES = 40000;
    private static final int STARS = 700;
    private static final int FREE_CLOUDS = 14;
    private static final int ARM_CLOUDS = 70;
    private static final int GRAPH = 160;
    private static final double GRAPH_W = 268;
    private static final double GRAPH_H = 56;
    private static final double TILE_W = 118;
    private static final double TILE_H = 78;
    private static final double PLANET = 200;
    private static final String[] SPRITE_COLORS = {
        "#fff4e0", "#ffcf87", "#8fc7ff", "#67e8f9", "#e879f9", "#ff6fae"
    };

    private Stage stage;
    private Scene scene;
    private boolean backdrop;
    private double time;
    private boolean paused;

    private final Canvas canvas = new Canvas();
    private Image[] stars;
    private Image[] clouds;
    private final float[] pRadius = new float[MAX_PARTICLES];
    private final float[] pAngle = new float[MAX_PARTICLES];
    private final float[] pHeight = new float[MAX_PARTICLES];
    private final float[] pSize = new float[MAX_PARTICLES];
    private final float[] pAlpha = new float[MAX_PARTICLES];
    private final float[] pPhase = new float[MAX_PARTICLES];
    private final byte[] pTint = new byte[MAX_PARTICLES];
    private final double[][] starField = new double[STARS][5];
    private final double[][] freeClouds = new double[FREE_CLOUDS][5];
    private final double[][] armClouds = new double[ARM_CLOUDS][4];
    private int sprites;

    private SubScene space;
    private final Group globe = new Group();
    private final Sphere planet = new Sphere(PLANET, 160);
    private final Sphere cloudShell = new Sphere(PLANET * 1.016, 128);
    private final Sphere atmosphere = new Sphere(PLANET * 1.06, 96);
    private final PhongMaterial planetMaterial = new PhongMaterial(Color.web("#2a5d7c"));
    private final Rotate planetSpin = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate cloudSpin = new Rotate(0, Rotate.Y_AXIS);
    private Image[] planetMaps;
    private final Group moonOrbit = new Group();
    private final Rotate moonAngle = new Rotate(0, Rotate.Y_AXIS);
    private MeshView moon;
    private final Group asteroidField = new Group();
    private final Rotate fieldSpin = new Rotate(0, Rotate.Y_AXIS);
    private final List<Rotate> tumbles = new ArrayList<>();
    private double[] tumbleSpeed = new double[0];
    private TriangleMesh[] rocks;
    private PhongMaterial[] rockMaterials;
    private final DirectionalLight sun = new DirectionalLight(Color.web("#cdc2b2"));
    private final SpotLight searchlight = new SpotLight(Color.web("#22b8d6"));
    private final PointLight moonLight = new PointLight(Color.web("#e879f9"));
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Rotate yaw = new Rotate(-20, Rotate.Y_AXIS);
    private final Rotate pitch = new Rotate(-14, Rotate.X_AXIS);
    private final Circle halo = new Circle();

    private final List<Node> spinners = new ArrayList<>();
    private final List<Node> orbs = new ArrayList<>();
    private final List<DoubleConsumer> effectUpdates = new ArrayList<>();

    private final CheckBox showGalaxy = new CheckBox("Galaxy");
    private final CheckBox showNebula = new CheckBox("Nebula clouds");
    private final CheckBox showHalo = new CheckBox("Halo (additive blend)");
    private final CheckBox additive = new CheckBox("Additive sprites (Decora per draw)");
    private final CheckBox showMaps = new CheckBox("Surface maps");
    private final CheckBox showClouds = new CheckBox("Cloud layer (alpha texture)");
    private final CheckBox showAtmosphere = new CheckBox("Atmosphere (translucent)");
    private final CheckBox showAsteroids = new CheckBox("Asteroids");
    private final CheckBox showMoon = new CheckBox("Crystal moon");
    private final CheckBox showGallery = new CheckBox("Effects gallery");
    private final CheckBox sunOn = new CheckBox("Sun (directional)");
    private final CheckBox spotOn = new CheckBox("Searchlight (spot)");
    private final CheckBox moonOn = new CheckBox("Moon glow (point)");
    private final CheckBox wireframe = new CheckBox("Wireframe");
    private final CheckBox autoOrbit = new CheckBox("Auto orbit");
    private final Slider particles = new Slider(0, MAX_PARTICLES, 9000);
    private final Slider asteroids = new Slider(0, 5000, 1500);
    private final Slider timeScale = new Slider(0, 3, 1);
    private final Slider tint = new Slider(0, 0.9, 0.22);

    private final Label fpsValue = new Label("–");
    private final Label frameValue = new Label("–");
    private final Label worstValue = new Label("–");
    private final Label trianglesValue = new Label("–");
    private final Label nodesValue = new Label("–");
    private final Label spritesValue = new Label("–");
    private final Label sizeValue = new Label("–");
    private final Label mapsValue = new Label("Generating maps…");
    private final Polyline frameLine = new Polyline();
    private final Polygon frameArea = new Polygon();
    private final double[] frameTimes = new double[GRAPH];
    private int frameHead;
    private final Region fill = new Region();
    private Node panels;
    private Node gallery;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        backdrop = Boolean.getBoolean("glass.gtk4.backdrop.active");
        stage.initStyle(StageStyle.EXTENDED);

        for (CheckBox c : List.of(showGalaxy, showNebula, showHalo, showMaps, showClouds, showAtmosphere,
                showAsteroids, showMoon, showGallery, sunOn, spotOn, moonOn, autoOrbit)) {
            c.setSelected(true);
        }

        updateFill();
        tint.valueProperty().addListener(o -> updateFill());
        tint.setDisable(!backdrop);

        Pane canvasLayer = new Pane(canvas);
        canvas.setManaged(false);
        canvas.widthProperty().bind(canvasLayer.widthProperty());
        canvas.heightProperty().bind(canvasLayer.heightProperty());
        canvasLayer.setMinSize(0, 0);
        seedGalaxy();

        Pane haloLayer = new Pane(halo);
        haloLayer.setMinSize(0, 0);
        haloLayer.setMouseTransparent(true);
        halo.setManaged(false);
        halo.setBlendMode(BlendMode.ADD);
        halo.visibleProperty().bind(showHalo.selectedProperty());

        Pane spaceLayer = new Pane(space());
        space.setManaged(false);
        space.widthProperty().bind(spaceLayer.widthProperty());
        space.heightProperty().bind(spaceLayer.heightProperty());
        spaceLayer.setMinSize(0, 0);

        BorderPane overlay = new BorderPane();
        overlay.setTop(header());
        overlay.setCenter(panels());
        overlay.setPickOnBounds(false);

        StackPane root = new StackPane(fill, canvasLayer, haloLayer, spaceLayer, overlay);
        root.setMinSize(0, 0);
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width = Math.max(1200, Math.min(1760, screen.getWidth() * 0.88));
        double height = Math.max(760, Math.min(1100, screen.getHeight() * 0.9));
        scene = new Scene(root, width, height, Color.TRANSPARENT);
        scene.getStylesheets().add(STYLESHEET);
        scene.getPreferences().setColorScheme(ColorScheme.DARK);
        orbitControls(root);
        keys();

        stage.setScene(scene);
        stage.setTitle("Nebula — Prism renderer showcase");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();

        Thread.ofVirtual().start(() -> {
            long start = System.nanoTime();
            Image[] maps = planetMaps(2048, 1024);
            double seconds = (System.nanoTime() - start) / 1e9;
            Platform.runLater(() -> {
                planetMaps = maps;
                mapsValue.setText(String.format(Locale.ROOT, "4 maps · 2048×1024 · %.1fs", seconds));
                applyMaps();
                cloudShell.setMaterial(cloudMaterial(maps[4]));
            });
        });

        startLoop();
        String snapshot = System.getProperty("nebula.snapshot");
        if (snapshot != null) {
            PauseTransition wait = new PauseTransition(Duration.seconds(8));
            wait.setOnFinished(e -> {
                try {
                    javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(scene.snapshot(null), null),
                            "png", new java.io.File(snapshot));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                Platform.exit();
            });
            wait.play();
        }
    }

    private void updateFill() {
        if (backdrop) {
            double a = tint.getValue();
            fill.setBackground(new Background(
                    new BackgroundFill(Color.color(0.02, 0.03, 0.07, a), null, null),
                    new BackgroundFill(new RadialGradient(0, 0, 0.5, 0.5, 0.75, true, CycleMethod.NO_CYCLE,
                            new Stop(0.4, Color.TRANSPARENT), new Stop(1, Color.color(0, 0, 0.02, 0.2 + a * 0.4))),
                            null, null)));
        } else {
            fill.setBackground(new Background(
                    new BackgroundFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.web("#05060f")), new Stop(0.5, Color.web("#0b0a1f")),
                            new Stop(1, Color.web("#020814"))), null, null)));
        }
    }

    // ---------------------------------------------------------------------
    // Shell

    private static String pipelineName() {
        String name = com.sun.prism.GraphicsPipeline.getPipeline().getClass().getSimpleName();
        return switch (name) {
            case "VKPipeline" -> "Vulkan";
            case "ES2Pipeline" -> "OpenGL ES";
            case "SWPipeline" -> "Software";
            default -> name;
        };
    }

    private Node header() {
        Circle logo = new Circle(11, new RadialGradient(0, 0, 0.35, 0.35, 0.8, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(0.35, Color.web("#67e8f9")), new Stop(1, Color.web("#7c3aed"))));
        logo.setEffect(new Glow(0.8));
        Label brand = new Label("Nebula");
        brand.getStyleClass().add("brand");
        Label sub = new Label("Prism renderer showcase");
        sub.getStyleClass().add("muted");
        HBox left = new HBox(10, logo, brand, sub);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 18));

        Circle dot = new Circle(4, Color.web("#22c55e"));
        dot.setEffect(new DropShadow(8, Color.web("#22c55e")));
        FadeTransition pulse = new FadeTransition(Duration.millis(900), dot);
        pulse.setFromValue(1);
        pulse.setToValue(0.35);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
        Label fps = new Label();
        fps.getStyleClass().add("mono");
        fps.textProperty().bind(fpsValue.textProperty().concat(" fps"));
        HBox live = new HBox(dot, fps);
        live.getStyleClass().add("pill");
        HBox pipeline = new HBox(new Label(pipelineName() + " · GTK 4"));
        pipeline.getStyleClass().add("pill");
        HBox glass = new HBox(new Label(backdrop ? "Backdrop blur" : "Opaque"));
        glass.getStyleClass().add("pill");

        Button hide = new Button("Panels  H");
        hide.setOnAction(e -> togglePanels());

        HBox right = new HBox(10, live, pipeline, glass, hide);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setPadding(new Insets(0, 10, 0, 0));

        HeaderBar.setDragType(left, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar.setDragType(live, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar.setDragType(pipeline, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar.setDragType(glass, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar.setDragType(right, HeaderDragType.DRAGGABLE);

        HeaderBar header = new HeaderBar(left, null, right);
        header.setBackground(null);
        header.setMinHeight(54);
        header.getStyleClass().add("header");
        HeaderBar.setAlignment(left, Pos.CENTER_LEFT);
        return header;
    }

    private void togglePanels() {
        panels.setVisible(!panels.isVisible());
    }

    private void keys() {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.H) {
                togglePanels();
            } else if (e.getCode() == KeyCode.F || e.getCode() == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
            } else if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.P) {
                paused = !paused;
            } else {
                return;
            }
            e.consume();
        });
    }

    private static boolean onChrome(Object target) {
        for (Node n = target instanceof Node node ? node : null; n != null; n = n.getParent()) {
            if (n instanceof HeaderBar || n.getStyleClass().contains("glass")) {
                return true;
            }
        }
        return false;
    }

    private void orbitControls(Node root) {
        double[] drag = new double[3];
        root.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            drag[0] = e.getSceneX();
            drag[1] = e.getSceneY();
            drag[2] = onChrome(e.getTarget()) ? 0 : 1;
        });
        root.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (drag[2] == 0) {
                return;
            }
            yaw.setAngle(yaw.getAngle() + (e.getSceneX() - drag[0]) * 0.3);
            pitch.setAngle(Math.max(-80, Math.min(60, pitch.getAngle() - (e.getSceneY() - drag[1]) * 0.3)));
            drag[0] = e.getSceneX();
            drag[1] = e.getSceneY();
        });
        root.addEventHandler(ScrollEvent.SCROLL, e -> {
            if (!onChrome(e.getTarget())) {
                camera.setTranslateZ(Math.max(-4200, Math.min(-520, camera.getTranslateZ() + e.getDeltaY() * 2)));
            }
        });
    }

    // ---------------------------------------------------------------------
    // Panels

    private Node panels() {
        AnchorPane area = new AnchorPane();
        area.setPickOnBounds(false);

        Node hud = hud();
        AnchorPane.setTopAnchor(hud, 4.0);
        AnchorPane.setLeftAnchor(hud, 18.0);

        ScrollPane controls = new ScrollPane(controls());
        controls.setFitToWidth(true);
        controls.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        controls.getStyleClass().add("glass");
        controls.setPadding(new Insets(14, 6, 14, 16));
        controls.setPrefWidth(300);
        AnchorPane.setTopAnchor(controls, 4.0);
        AnchorPane.setRightAnchor(controls, 18.0);
        AnchorPane.setBottomAnchor(controls, 18.0);

        gallery = gallery();
        gallery.visibleProperty().bind(showGallery.selectedProperty());
        AnchorPane.setLeftAnchor(gallery, 18.0);
        AnchorPane.setRightAnchor(gallery, 336.0);
        AnchorPane.setBottomAnchor(gallery, 18.0);

        area.getChildren().addAll(hud, controls, gallery);
        panels = area;
        return area;
    }

    private Node hud() {
        Text title = new Text("NEBULA");
        title.setFont(Font.font("Adwaita Sans", FontWeight.BLACK, 54));
        title.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#67e8f9")), new Stop(0.5, Color.web("#a78bfa")), new Stop(1, Color.web("#f472b6"))));
        Reflection reflection = new Reflection(0, 0.45, 0.4, 0);
        reflection.setInput(new DropShadow(BlurType.GAUSSIAN, Color.web("#a855f7cc"), 28, 0.25, 0, 0));
        title.setEffect(reflection);
        Label subtitle = new Label("Prism · " + pipelineName() + " · " + System.getProperty("glass.platform", "gtk")
                + " glass · extended stage");
        subtitle.getStyleClass().add("muted");

        GridPane stats = new GridPane(22, 10);
        stats.add(stat("FPS", fpsValue), 0, 0);
        stats.add(stat("Frame ms", frameValue), 1, 0);
        stats.add(stat("Worst ms", worstValue), 2, 0);
        stats.add(stat("Triangles", trianglesValue), 0, 1);
        stats.add(stat("3D nodes", nodesValue), 1, 1);
        stats.add(stat("Sprites", spritesValue), 2, 1);

        Pane graph = new Pane();
        graph.setPrefSize(GRAPH_W, GRAPH_H);
        graph.setMinSize(GRAPH_W, GRAPH_H);
        graph.setMaxSize(GRAPH_W, GRAPH_H);
        double target = GRAPH_H - GRAPH_H * 16.7 / 40;
        Line budget = new Line(0, target, GRAPH_W, target);
        budget.setStroke(Color.color(1, 1, 1, 0.3));
        budget.getStrokeDashArray().addAll(4.0, 4.0);
        frameArea.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#a855f766")), new Stop(1, Color.web("#38bdf800"))));
        frameLine.setStroke(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#67e8f9")), new Stop(1, Color.web("#f472b6"))));
        frameLine.setStrokeWidth(1.6);
        frameLine.setStrokeLineJoin(StrokeLineJoin.ROUND);
        Text budgetLabel = new Text(GRAPH_W - 52, target - 4, "16.7 ms");
        budgetLabel.setFill(Color.color(1, 1, 1, 0.45));
        budgetLabel.setFont(Font.font("Adwaita Mono", 10));
        graph.getChildren().addAll(frameArea, frameLine, budget, budgetLabel);

        sizeValue.getStyleClass().addAll("muted", "mono");
        mapsValue.getStyleClass().addAll("muted", "mono");
        Label msaa = new Label("4× MSAA · depth buffer · 3 lights + ambient");
        msaa.getStyleClass().addAll("muted", "mono");

        VBox card = new VBox(12, stats, graph, new VBox(3, sizeValue, mapsValue, msaa));
        card.getStyleClass().add("glass");
        card.setMaxWidth(Region.USE_PREF_SIZE);

        VBox hud = new VBox(4, title, subtitle, spacer(10), card);
        hud.setPickOnBounds(false);
        return hud;
    }

    private static Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        return r;
    }

    private static VBox stat(String key, Label value) {
        value.getStyleClass().addAll("stat", "mono");
        value.setMinWidth(80);
        Label k = new Label(key.toUpperCase(Locale.ROOT));
        k.getStyleClass().add("stat-key");
        return new VBox(0, k, value);
    }

    private static Label section(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("section");
        return l;
    }

    private static Node sliderRow(String name, Slider slider, String format) {
        Label value = new Label();
        value.getStyleClass().addAll("muted", "mono");
        value.textProperty().bind(Bindings.format(Locale.ROOT, format, slider.valueProperty()));
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        return new VBox(4, new HBox(new Label(name), gap, value), slider);
    }

    private Node controls() {
        showMaps.selectedProperty().addListener(o -> applyMaps());
        cloudShell.visibleProperty().bind(showClouds.selectedProperty());
        atmosphere.visibleProperty().bind(showAtmosphere.selectedProperty());
        asteroidField.visibleProperty().bind(showAsteroids.selectedProperty());
        moonOrbit.visibleProperty().bind(showMoon.selectedProperty());
        sun.lightOnProperty().bind(sunOn.selectedProperty());
        searchlight.lightOnProperty().bind(spotOn.selectedProperty());
        moonLight.lightOnProperty().bind(moonOn.selectedProperty());
        wireframe.selectedProperty().addListener(o -> applyDrawMode());
        asteroids.valueChangingProperty().addListener((o, was, now) -> {
            if (!now) {
                buildAsteroids();
            }
        });
        asteroids.valueProperty().addListener(o -> {
            if (!asteroids.isValueChanging()) {
                buildAsteroids();
            }
        });

        Button calm = new Button("Calm");
        calm.setOnAction(e -> preset(6000, 600, false));
        Button stress = new Button("Stress");
        stress.getStyleClass().add("accent");
        stress.setOnAction(e -> preset(MAX_PARTICLES, 5000, true));
        Button full = new Button("Full screen");
        full.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));
        HBox buttons = new HBox(8, calm, stress, full);

        Label hint = new Label("Drag to orbit · scroll to zoom\nH panels · F full screen · Space pause");
        hint.getStyleClass().add("muted");

        VBox box = new VBox(8,
                section("CANVAS 2D"), showGalaxy, showNebula, showHalo, additive,
                section("PLANET"), showMaps, showClouds, showAtmosphere, showAsteroids, showMoon,
                section("LIGHTS"), sunOn, spotOn, moonOn,
                section("LOAD"),
                sliderRow("Particles", particles, "%,.0f"),
                sliderRow("Asteroids", asteroids, "%,.0f"),
                sliderRow("Time scale", timeScale, "%.2f×"),
                section("WINDOW"),
                sliderRow("Backdrop tint", tint, "%.2f"),
                wireframe, autoOrbit, showGallery, spacer(4), buttons, spacer(4), hint);
        box.setPadding(new Insets(0, 12, 0, 0));
        return box;
    }

    private void preset(int sprites, int rocks, boolean all) {
        particles.setValue(sprites);
        asteroids.setValue(rocks);
        if (all) {
            for (CheckBox c : List.of(showGalaxy, showNebula, showHalo, showMaps, showClouds, showAtmosphere,
                    showAsteroids, showMoon, showGallery, sunOn, spotOn, moonOn)) {
                c.setSelected(true);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Effects gallery

    private Node gallery() {
        HBox tiles = new HBox(10);
        tiles.setPadding(new Insets(2));

        Bloom bloom = new Bloom(0.3);
        tiles.getChildren().add(tile("Bloom", bloom, t -> bloom.setThreshold(0.25 + 0.2 * Math.sin(t * 1.4))));

        GaussianBlur gaussian = new GaussianBlur();
        tiles.getChildren().add(tile("Gaussian blur", gaussian, t -> gaussian.setRadius(7 + 7 * Math.sin(t * 1.6))));

        MotionBlur motion = new MotionBlur(0, 16);
        tiles.getChildren().add(tile("Motion blur", motion, t -> motion.setAngle(t * 70)));

        FloatMap map = new FloatMap((int) TILE_W, (int) TILE_H);
        DisplacementMap displacement = new DisplacementMap(map);
        tiles.getChildren().add(tile("Displacement", displacement, t -> {
            for (int y = 0; y < TILE_H; y++) {
                for (int x = 0; x < TILE_W; x++) {
                    double d = Math.hypot(x - TILE_W / 2, y - TILE_H / 2);
                    double w = Math.sin(d * 0.3 - t * 6) * 0.025;
                    map.setSamples(x, y, (float) w, (float) w);
                }
            }
        }));

        Light.Point point = new Light.Point(0, 0, 50, Color.WHITE);
        Lighting lighting = new Lighting(point);
        lighting.setSurfaceScale(5);
        lighting.setSpecularExponent(24);
        lighting.setSpecularConstant(0.6);
        tiles.getChildren().add(tile("Lighting", lighting, t -> {
            point.setX(TILE_W / 2 + Math.cos(t * 1.5) * 48);
            point.setY(TILE_H / 2 + Math.sin(t * 1.5) * 26);
        }));

        ColorAdjust adjust = new ColorAdjust(0, 0.3, 0, 0.15);
        tiles.getChildren().add(tile("Color adjust", adjust, t -> adjust.setHue(Math.sin(t * 0.8))));

        PerspectiveTransform perspective = new PerspectiveTransform();
        tiles.getChildren().add(tile("Perspective", perspective, t -> {
            double c = Math.cos(t * 1.2);
            double s = Math.sin(t * 1.2) * 12;
            double hw = TILE_W / 2 * Math.abs(c);
            double cx = TILE_W / 2;
            perspective.setUlx(cx - hw);
            perspective.setLlx(cx - hw);
            perspective.setUrx(cx + hw);
            perspective.setLrx(cx + hw);
            perspective.setUly(s);
            perspective.setLly(TILE_H - s);
            perspective.setUry(-s);
            perspective.setLry(TILE_H + s);
        }));

        BlendMode[] modes = {
            BlendMode.SCREEN, BlendMode.OVERLAY, BlendMode.MULTIPLY, BlendMode.DIFFERENCE, BlendMode.EXCLUSION,
            BlendMode.COLOR_DODGE, BlendMode.COLOR_BURN, BlendMode.HARD_LIGHT, BlendMode.SOFT_LIGHT,
            BlendMode.LIGHTEN, BlendMode.DARKEN, BlendMode.ADD, BlendMode.RED, BlendMode.GREEN, BlendMode.BLUE
        };
        ColorInput layer = new ColorInput(0, 0, TILE_W, TILE_H, new LinearGradient(0, 0, 1, 1, true,
                CycleMethod.REFLECT, new Stop(0, Color.web("#f97316")), new Stop(0.5, Color.web("#22d3ee")),
                new Stop(1, Color.web("#a3e635"))));
        Blend blend = new Blend(BlendMode.SCREEN, null, layer);
        Label blendName = new Label();
        tiles.getChildren().add(tile(blendName, blend, t -> {
            BlendMode mode = modes[(int) (t / 1.6) % modes.length];
            if (blend.getMode() != mode || blendName.getText().isEmpty()) {
                blend.setMode(mode);
                blendName.setText("Blend · " + mode.name().toLowerCase(Locale.ROOT).replace('_', ' '));
            }
        }));

        InnerShadow inner = new InnerShadow(BlurType.GAUSSIAN, Color.web("#0b1020"), 18, 0.2, 0, 0);
        DropShadow glow = new DropShadow(BlurType.GAUSSIAN, Color.web("#67e8f9"), 22, 0.35, 0, 0);
        glow.setInput(inner);
        tiles.getChildren().add(tile("Shadow chain", glow, t -> {
            glow.setColor(Color.hsb((t * 60) % 360, 0.7, 1));
            inner.setOffsetX(Math.cos(t * 2) * 6);
            inner.setOffsetY(Math.sin(t * 2) * 6);
        }));

        SepiaTone sepia = new SepiaTone(0.9);
        BoxBlur box = new BoxBlur(4, 4, 3);
        sepia.setInput(box);
        tiles.getChildren().add(tile("Box blur + sepia", sepia, t -> {
            box.setWidth(1 + 6 * (1 + Math.sin(t * 1.3)));
            box.setHeight(box.getWidth());
            sepia.setLevel(0.5 + 0.5 * Math.sin(t * 0.7));
        }));

        ScrollPane scroll = new ScrollPane(tiles);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setFitToHeight(true);
        Label title = section("DECORA EFFECTS · LIVE PARAMETERS");
        VBox box2 = new VBox(6, title, scroll);
        box2.getStyleClass().add("glass");
        box2.setPadding(new Insets(10, 14, 12, 14));
        box2.setMaxHeight(Region.USE_PREF_SIZE);
        return box2;
    }

    private Node tile(String name, Effect effect, DoubleConsumer update) {
        return tile(new Label(name), effect, update);
    }

    private Node tile(Label name, Effect effect, DoubleConsumer update) {
        int index = spinners.size();
        double hue = 200 + index * 37;
        Rectangle bg = new Rectangle(TILE_W, TILE_H);
        bg.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.hsb(hue % 360, 0.75, 0.55)), new Stop(1, Color.hsb((hue + 70) % 360, 0.8, 0.2))));

        Polygon star = new Polygon();
        for (int i = 0; i < 10; i++) {
            double a = Math.PI * i / 5 - Math.PI / 2;
            double r = i % 2 == 0 ? 30 : 13;
            star.getPoints().addAll(Math.cos(a) * r, Math.sin(a) * r);
        }
        star.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.6, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(1, Color.hsb((hue + 160) % 360, 0.6, 1))));
        star.setStroke(Color.color(1, 1, 1, 0.8));
        star.setStrokeWidth(1.5);

        Circle orb = new Circle(9, new RadialGradient(0, 0, 0.3, 0.3, 0.7, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(1, Color.hsb((hue + 90) % 360, 0.8, 0.9))));
        Text fx = new Text("FX");
        fx.setFont(Font.font("Adwaita Sans", FontWeight.BLACK, 22));
        fx.setFill(Color.color(1, 1, 1, 0.9));
        StackPane.setAlignment(fx, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(fx, new Insets(0, 8, 4, 0));

        StackPane art = new StackPane(bg, star, orb, fx);
        art.setMinSize(TILE_W, TILE_H);
        art.setPrefSize(TILE_W, TILE_H);
        art.setMaxSize(TILE_W, TILE_H);
        art.setEffect(effect);
        spinners.add(star);
        orbs.add(orb);
        effectUpdates.add(update);

        Rectangle clip = new Rectangle(TILE_W, TILE_H);
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        Pane frame = new Pane(art);
        frame.setMinSize(TILE_W, TILE_H);
        frame.setPrefSize(TILE_W, TILE_H);
        frame.setMaxSize(TILE_W, TILE_H);
        frame.setClip(clip);
        name.getStyleClass().add("tile-label");
        return new VBox(6, frame, name);
    }

    // ---------------------------------------------------------------------
    // Galaxy

    private static Image sprite(Color color, int size, double sharpness) {
        WritableImage image = new WritableImage(size, size);
        PixelWriter pw = image.getPixelWriter();
        double h = (size - 1) / 2.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = (x - h) / h;
                double dy = (y - h) / h;
                double d2 = dx * dx + dy * dy;
                double a = d2 >= 1 ? 0 : Math.exp(-d2 * sharpness) * (1 - d2);
                Color c = color.interpolate(Color.WHITE, Math.exp(-d2 * sharpness * 5));
                pw.setColor(x, y, Color.color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(1, a)));
            }
        }
        return image;
    }

    private void seedGalaxy() {
        stars = new Image[SPRITE_COLORS.length];
        clouds = new Image[SPRITE_COLORS.length];
        for (int i = 0; i < SPRITE_COLORS.length; i++) {
            stars[i] = sprite(Color.web(SPRITE_COLORS[i]), 32, 7);
            clouds[i] = sprite(Color.web(SPRITE_COLORS[i]), 128, 2.2);
        }
        Random r = new Random(11);
        for (int i = 0; i < MAX_PARTICLES; i++) {
            double kind = r.nextDouble();
            double rad;
            double offset;
            if (kind < 0.16) {
                rad = Math.abs(r.nextGaussian()) * 0.11;
                offset = r.nextDouble() * Math.PI * 2;
                pSize[i] = 7 + r.nextFloat() * 9;
                pAlpha[i] = 0.5f;
                pTint[i] = (byte) (r.nextDouble() < 0.7 ? 1 : 0);
            } else {
                rad = 0.06 + Math.pow(r.nextDouble(), 1.4) * 0.95;
                offset = r.nextGaussian() * 0.38 * (1.15 - rad);
                pSize[i] = 4 + r.nextFloat() * 7;
                pAlpha[i] = 0.45f;
                double c = r.nextDouble();
                pTint[i] = (byte) (rad < 0.3 ? (c < 0.5 ? 0 : c < 0.8 ? 1 : 2) : (c < 0.45 ? 2 : c < 0.8 ? 3 : 4));
                if (kind > 0.975) {
                    pSize[i] = 18 + r.nextFloat() * 18;
                    pAlpha[i] = 0.32f;
                    pTint[i] = 5;
                }
            }
            int arm = i % 2;
            pRadius[i] = (float) rad;
            pAngle[i] = (float) (arm * Math.PI + rad * 5.4 + offset);
            pHeight[i] = (float) (r.nextGaussian() * 0.025 * (1.2 - Math.min(rad, 1)));
            pPhase[i] = (float) (r.nextDouble() * Math.PI * 2);
        }
        for (double[] s : starField) {
            s[0] = r.nextDouble();
            s[1] = r.nextDouble();
            s[2] = 3 + r.nextDouble() * 5;
            s[3] = r.nextDouble() * Math.PI * 2;
            s[4] = r.nextInt(4);
        }
        int[] cloudTints = {2, 3, 4, 5};
        for (double[] c : freeClouds) {
            c[0] = r.nextDouble();
            c[1] = r.nextDouble();
            c[2] = 0.3 + r.nextDouble() * 0.45;
            c[3] = 0.05 + r.nextDouble() * 0.07;
            c[4] = cloudTints[r.nextInt(cloudTints.length)];
        }
        for (int i = 0; i < ARM_CLOUDS; i++) {
            double rad = 0.18 + r.nextDouble() * 0.75;
            double[] c = armClouds[i];
            c[0] = rad;
            c[1] = (i % 2) * Math.PI + rad * 5.4 + r.nextGaussian() * 0.25;
            c[2] = 0.1 + r.nextDouble() * 0.16;
            c[3] = rad < 0.35 ? 4 : 2 + r.nextInt(4);
        }
    }

    private void drawGalaxy() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);
        sprites = 0;
        boolean galaxy = showGalaxy.isSelected();
        boolean nebula = showNebula.isSelected();
        if (!galaxy && !nebula) {
            return;
        }
        g.setGlobalBlendMode(additive.isSelected() ? BlendMode.ADD : BlendMode.SRC_OVER);
        double cx = w * 0.6;
        double cy = h * 0.47;
        double scale = Math.max(w, h) * 0.42;
        double unit = scale / 600;
        double rot = -0.42;
        double cr = Math.cos(rot);
        double sr = Math.sin(rot);
        double tilt = 0.36;
        double big = Math.max(w, h);

        if (nebula) {
            for (int i = 0; i < FREE_CLOUDS; i++) {
                double[] c = freeClouds[i];
                double x = (c[0] + 0.05 * Math.sin(time * 0.05 + i)) * w;
                double y = (c[1] + 0.04 * Math.cos(time * 0.04 + i * 1.7)) * h;
                double s = c[2] * big;
                g.setGlobalAlpha(c[3] * (0.8 + 0.2 * Math.sin(time * 0.3 + i)));
                g.drawImage(clouds[(int) c[4]], x - s / 2, y - s / 2, s, s);
            }
            for (int i = 0; i < ARM_CLOUDS; i++) {
                double[] c = armClouds[i];
                double a = c[1] + time * 0.22 / (0.18 + c[0]);
                double x = Math.cos(a) * c[0];
                double y = Math.sin(a) * c[0] * tilt;
                double sx = cx + (x * cr - y * sr) * scale;
                double sy = cy + (x * sr + y * cr) * scale;
                double s = c[2] * scale;
                g.setGlobalAlpha(0.13);
                g.drawImage(clouds[(int) c[3]], sx - s / 2, sy - s / 2, s, s);
            }
            sprites += FREE_CLOUDS + ARM_CLOUDS;
        }

        if (galaxy) {
            for (double[] s : starField) {
                double size = s[2] * unit;
                g.setGlobalAlpha(0.45 + 0.45 * Math.sin(time * 2.2 + s[3]));
                g.drawImage(stars[(int) s[4]], s[0] * w - size / 2, s[1] * h - size / 2, size, size);
            }
            int n = (int) particles.getValue();
            for (int i = 0; i < n; i++) {
                double rad = pRadius[i];
                double a = pAngle[i] + time * 0.22 / (0.18 + rad);
                double x = Math.cos(a) * rad;
                double y = Math.sin(a) * rad * tilt + pHeight[i];
                double sx = cx + (x * cr - y * sr) * scale;
                double sy = cy + (x * sr + y * cr) * scale;
                double size = pSize[i] * unit;
                if (sx < -size || sy < -size || sx > w + size || sy > h + size) {
                    continue;
                }
                g.setGlobalAlpha(pAlpha[i] * (0.65 + 0.35 * Math.sin(time * 2.5 + pPhase[i])));
                g.drawImage(stars[pTint[i]], sx - size / 2, sy - size / 2, size, size);
            }
            double core = scale * 0.5;
            g.setGlobalAlpha(0.55);
            g.drawImage(clouds[1], cx - core / 2, cy - core / 2, core, core);
            double hot = scale * 0.14;
            g.setGlobalAlpha(0.9);
            g.drawImage(stars[0], cx - hot / 2, cy - hot / 2, hot, hot);
            sprites += STARS + n + 2;
        }
        g.setGlobalAlpha(1);
        g.setGlobalBlendMode(BlendMode.SRC_OVER);
    }

    // ---------------------------------------------------------------------
    // Planet

    private SubScene space() {
        globe.getTransforms().add(new Rotate(18, Rotate.Z_AXIS));
        planet.getTransforms().add(planetSpin);
        planet.setMaterial(planetMaterial);
        planetMaterial.setSpecularColor(Color.web("#8fb5e0"));
        planetMaterial.setSpecularPower(40);
        cloudShell.getTransforms().add(cloudSpin);
        cloudShell.setMaterial(cloudMaterial(null));
        PhongMaterial air = new PhongMaterial(Color.color(0.42, 0.7, 1, 0.12));
        air.setSpecularColor(Color.color(0.8, 0.9, 1, 0.5));
        air.setSpecularPower(12);
        atmosphere.setMaterial(air);

        moon = new MeshView(icosphere(1, 0.35, 9, true));
        PhongMaterial crystal = new PhongMaterial(Color.web("#7c3aed"));
        crystal.setSpecularColor(Color.web("#8b7bd8"));
        crystal.setSpecularPower(48);
        crystal.setSelfIlluminationMap(solid(Color.web("#3b0764")));
        moon.setMaterial(crystal);
        moon.setScaleX(26);
        moon.setScaleY(26);
        moon.setScaleZ(26);
        moon.setTranslateX(610);
        Rotate moonSpin = new Rotate(0, new Point3D(0.3, 1, 0.2));
        moon.getTransforms().add(moonSpin);
        moonLight.setTranslateX(610);
        moonLight.setMaxRange(1400);
        moonLight.setLinearAttenuation(0.0012);
        moonOrbit.getChildren().addAll(moon, moonLight);
        moonOrbit.getTransforms().addAll(new Rotate(14, Rotate.Z_AXIS), moonAngle);
        moonOrbit.setUserData(moonSpin);

        rocks = new TriangleMesh[6];
        for (int i = 0; i < rocks.length; i++) {
            rocks[i] = icosphere(1, 0.6, 100 + i, true);
        }
        String[][] rockColors = {
            {"#6b6258", "#3a3632"}, {"#857a6c", "#4a443e"}, {"#5c5f6b", "#3b3f4a"}, {"#d6e4f5", "#ffffff"}
        };
        rockMaterials = new PhongMaterial[rockColors.length];
        for (int i = 0; i < rockColors.length; i++) {
            PhongMaterial m = new PhongMaterial(Color.web(rockColors[i][0]));
            m.setSpecularColor(Color.web(rockColors[i][1]));
            m.setSpecularPower(i == 3 ? 48 : 10);
            rockMaterials[i] = m;
        }
        asteroidField.getTransforms().addAll(new Rotate(8, Rotate.X_AXIS), new Rotate(-6, Rotate.Z_AXIS), fieldSpin);
        buildAsteroids();

        sun.setDirection(new Point3D(0.85, 0.3, 0.55));
        searchlight.setInnerAngle(4);
        searchlight.setOuterAngle(16);
        searchlight.setFalloff(1.2);
        AmbientLight ambient = new AmbientLight(Color.rgb(14, 16, 30));

        globe.getChildren().addAll(planet, cloudShell, atmosphere);

        camera.setNearClip(5);
        camera.setFarClip(12000);
        camera.setFieldOfView(30);
        camera.setTranslateZ(-1650);
        camera.setTranslateX(170);
        Group rig = new Group(camera);
        rig.getTransforms().addAll(yaw, pitch);

        Group world = new Group(asteroidField, moonOrbit, globe, sun, searchlight, ambient, rig);
        space = new SubScene(world, 800, 600, true, SceneAntialiasing.BALANCED);
        space.setFill(Color.TRANSPARENT);
        space.setCamera(camera);
        return space;
    }

    private static PhongMaterial cloudMaterial(Image map) {
        PhongMaterial m = new PhongMaterial(map == null ? Color.color(1, 1, 1, 0) : Color.WHITE);
        m.setDiffuseMap(map);
        m.setSpecularColor(Color.BLACK);
        return m;
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

    private void applyMaps() {
        boolean on = showMaps.isSelected() && planetMaps != null;
        planetMaterial.setDiffuseColor(on ? Color.WHITE : Color.web("#2a5d7c"));
        planetMaterial.setDiffuseMap(on ? planetMaps[0] : null);
        planetMaterial.setBumpMap(on ? planetMaps[1] : null);
        planetMaterial.setSpecularMap(on ? planetMaps[2] : null);
        planetMaterial.setSelfIlluminationMap(on ? planetMaps[3] : null);
    }

    private void applyDrawMode() {
        DrawMode mode = wireframe.isSelected() ? DrawMode.LINE : DrawMode.FILL;
        planet.setDrawMode(mode);
        cloudShell.setDrawMode(mode);
        atmosphere.setDrawMode(mode);
        moon.setDrawMode(mode);
        for (Node n : asteroidField.getChildren()) {
            ((Shape3D) n).setDrawMode(mode);
        }
    }

    private void buildAsteroids() {
        int n = (int) asteroids.getValue();
        if (n == asteroidField.getChildren().size()) {
            return;
        }
        Random r = new Random(5);
        List<Node> nodes = new ArrayList<>(n);
        tumbles.clear();
        tumbleSpeed = new double[n];
        DrawMode mode = wireframe.isSelected() ? DrawMode.LINE : DrawMode.FILL;
        for (int i = 0; i < n; i++) {
            MeshView m = new MeshView(rocks[r.nextInt(rocks.length)]);
            m.setMaterial(rockMaterials[r.nextDouble() < 0.12 ? 3 : r.nextInt(3)]);
            double a = r.nextDouble() * Math.PI * 2;
            double band = r.nextDouble() < 0.7 ? 360 : 455;
            double rad = band + r.nextGaussian() * (band > 400 ? 18 : 34);
            double s = 1.4 + Math.pow(r.nextDouble(), 3) * 7;
            m.setScaleX(s * (0.7 + 0.6 * r.nextDouble()));
            m.setScaleY(s * (0.6 + 0.4 * r.nextDouble()));
            m.setScaleZ(s * (0.7 + 0.6 * r.nextDouble()));
            m.setTranslateX(Math.cos(a) * rad);
            m.setTranslateZ(Math.sin(a) * rad);
            m.setTranslateY(r.nextGaussian() * 5);
            Rotate tumble = new Rotate(r.nextDouble() * 360,
                    new Point3D(r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5));
            m.getTransforms().add(tumble);
            m.setDrawMode(mode);
            tumbles.add(tumble);
            tumbleSpeed[i] = (r.nextDouble() - 0.5) * 140;
            nodes.add(m);
        }
        asteroidField.getChildren().setAll(nodes);
    }

    private void updateSpace(double step) {
        planetSpin.setAngle(planetSpin.getAngle() + step * 4);
        cloudSpin.setAngle(cloudSpin.getAngle() + step * 5.5);
        fieldSpin.setAngle(fieldSpin.getAngle() + step * 3);
        moonAngle.setAngle(moonAngle.getAngle() + step * 9);
        Rotate moonSpin = (Rotate) moonOrbit.getUserData();
        moonSpin.setAngle(moonSpin.getAngle() + step * 40);
        for (int i = 0; i < tumbleSpeed.length; i++) {
            Rotate t = tumbles.get(i);
            t.setAngle(t.getAngle() + tumbleSpeed[i] * step);
        }
        double a = time * 0.35;
        double sx = Math.cos(a) * 1100;
        double sy = -380 + Math.sin(time * 0.6) * 260;
        double sz = Math.sin(a) * 1100;
        searchlight.setTranslateX(sx);
        searchlight.setTranslateY(sy);
        searchlight.setTranslateZ(sz);
        searchlight.setDirection(new Point3D(-sx, -sy + Math.sin(time * 1.3) * 120, -sz));
        if (autoOrbit.isSelected() && !paused) {
            yaw.setAngle(yaw.getAngle() + step * 3);
        }
    }

    private void updateHalo() {
        double h = space.getHeight();
        if (!showHalo.isSelected() || h <= 0) {
            return;
        }
        Point3D center = globe.localToScene(Point3D.ZERO, true);
        if (center == null || Double.isNaN(center.getX())) {
            return;
        }
        double distance = Math.hypot(camera.getTranslateZ(), camera.getTranslateX());
        double r = atmosphere.getRadius() / (distance * Math.tan(Math.toRadians(camera.getFieldOfView() / 2))) * h / 2;
        double outer = r * 1.7;
        double edge = r / outer;
        halo.setCenterX(center.getX());
        halo.setCenterY(center.getY());
        halo.setRadius(outer);
        halo.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(edge * 0.86, Color.TRANSPARENT),
                new Stop(edge * 0.97, Color.color(0.45, 0.78, 1, 0.55)),
                new Stop(edge * 1.12, Color.color(0.42, 0.45, 1, 0.22)),
                new Stop(1, Color.TRANSPARENT)));
    }

    // ---------------------------------------------------------------------
    // Meshes and textures

    private static TriangleMesh icosphere(int subdivisions, double jitter, int seed, boolean faceted) {
        double t = (1 + Math.sqrt(5)) / 2;
        List<double[]> v = new ArrayList<>();
        double[][] base = {
            {-1, t, 0}, {1, t, 0}, {-1, -t, 0}, {1, -t, 0}, {0, -1, t}, {0, 1, t},
            {0, -1, -t}, {0, 1, -t}, {t, 0, -1}, {t, 0, 1}, {-t, 0, -1}, {-t, 0, 1}
        };
        for (double[] p : base) {
            v.add(unit(p));
        }
        List<int[]> faces = new ArrayList<>(List.of(
                new int[] {0, 11, 5}, new int[] {0, 5, 1}, new int[] {0, 1, 7}, new int[] {0, 7, 10},
                new int[] {0, 10, 11}, new int[] {1, 5, 9}, new int[] {5, 11, 4}, new int[] {11, 10, 2},
                new int[] {10, 7, 6}, new int[] {7, 1, 8}, new int[] {3, 9, 4}, new int[] {3, 4, 2},
                new int[] {3, 2, 6}, new int[] {3, 6, 8}, new int[] {3, 8, 9}, new int[] {4, 9, 5},
                new int[] {2, 4, 11}, new int[] {6, 2, 10}, new int[] {8, 6, 7}, new int[] {9, 8, 1}));
        for (int s = 0; s < subdivisions; s++) {
            Map<Long, Integer> mids = new HashMap<>();
            List<int[]> next = new ArrayList<>();
            for (int[] f : faces) {
                int ab = midpoint(v, mids, f[0], f[1]);
                int bc = midpoint(v, mids, f[1], f[2]);
                int ca = midpoint(v, mids, f[2], f[0]);
                next.add(new int[] {f[0], ab, ca});
                next.add(new int[] {f[1], bc, ab});
                next.add(new int[] {f[2], ca, bc});
                next.add(new int[] {ab, bc, ca});
            }
            faces = next;
        }
        for (double[] p : v) {
            double k = 1 + jitter * (fbm(p[0] * 1.6, p[1] * 1.6, p[2] * 1.6, 3, seed) - 0.5) * 2;
            p[0] *= k;
            p[1] *= k;
            p[2] *= k;
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getTexCoords().addAll(0, 0);
        int next = 0;
        for (double[] p : faceted ? List.<double[]>of() : v) {
            mesh.getPoints().addAll((float) p[0], (float) p[1], (float) p[2]);
        }
        for (int[] f : faces) {
            double[] a = v.get(f[0]);
            double[] b = v.get(f[1]);
            double[] c = v.get(f[2]);
            double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
            double wx = c[0] - a[0], wy = c[1] - a[1], wz = c[2] - a[2];
            double nx = uy * wz - uz * wy, ny = uz * wx - ux * wz, nz = ux * wy - uy * wx;
            boolean flip = nx * (a[0] + b[0] + c[0]) + ny * (a[1] + b[1] + c[1]) + nz * (a[2] + b[2] + c[2]) < 0;
            int[] order = flip ? new int[] {f[0], f[2], f[1]} : f;
            if (faceted) {
                for (int i : order) {
                    double[] p = v.get(i);
                    mesh.getPoints().addAll((float) p[0], (float) p[1], (float) p[2]);
                }
                mesh.getFaces().addAll(next, 0, next + 1, 0, next + 2, 0);
                next += 3;
            } else {
                mesh.getFaces().addAll(order[0], 0, order[1], 0, order[2], 0);
            }
        }
        return mesh;
    }

    private static double[] unit(double[] p) {
        double l = Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
        return new double[] {p[0] / l, p[1] / l, p[2] / l};
    }

    private static int midpoint(List<double[]> v, Map<Long, Integer> mids, int a, int b) {
        long key = ((long) Math.min(a, b) << 32) | Math.max(a, b);
        Integer index = mids.get(key);
        if (index == null) {
            double[] p = v.get(a);
            double[] q = v.get(b);
            v.add(unit(new double[] {p[0] + q[0], p[1] + q[1], p[2] + q[2]}));
            index = v.size() - 1;
            mids.put(key, index);
        }
        return index;
    }

    private static double hash(int x, int y, int z, int seed) {
        int h = x * 374761393 + y * 668265263 + z * 1274126177 + seed * 1442695041;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0x7fffffff) / (double) 0x7fffffff;
    }

    private static double noise(double x, double y, double z, int seed) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        int zi = (int) Math.floor(z);
        double xf = x - xi;
        double yf = y - yi;
        double zf = z - zi;
        double u = xf * xf * (3 - 2 * xf);
        double v = yf * yf * (3 - 2 * yf);
        double w = zf * zf * (3 - 2 * zf);
        double x00 = lerp(hash(xi, yi, zi, seed), hash(xi + 1, yi, zi, seed), u);
        double x10 = lerp(hash(xi, yi + 1, zi, seed), hash(xi + 1, yi + 1, zi, seed), u);
        double x01 = lerp(hash(xi, yi, zi + 1, seed), hash(xi + 1, yi, zi + 1, seed), u);
        double x11 = lerp(hash(xi, yi + 1, zi + 1, seed), hash(xi + 1, yi + 1, zi + 1, seed), u);
        return lerp(lerp(x00, x10, v), lerp(x01, x11, v), w);
    }

    private static double fbm(double x, double y, double z, int octaves, int seed) {
        double sum = 0;
        double amp = 0.5;
        double norm = 0;
        double f = 1;
        for (int o = 0; o < octaves; o++) {
            sum += amp * noise(x * f, y * f, z * f, seed + o * 31);
            norm += amp;
            amp *= 0.5;
            f *= 2.03;
        }
        return sum / norm;
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private static double smoothstep(double e0, double e1, double x) {
        double t = Math.max(0, Math.min(1, (x - e0) / (e1 - e0)));
        return t * t * (3 - 2 * t);
    }

    private static int argb(double a, double r, double g, double b) {
        return (int) (Math.max(0, Math.min(1, a)) * 255) << 24
                | (int) (Math.max(0, Math.min(1, r)) * 255) << 16
                | (int) (Math.max(0, Math.min(1, g)) * 255) << 8
                | (int) (Math.max(0, Math.min(1, b)) * 255);
    }

    private static int argb(Color c) {
        return argb(1, c.getRed(), c.getGreen(), c.getBlue());
    }

    private static final Color SHALLOW = Color.web("#2fd3c3");
    private static final Color OCEAN = Color.web("#0b4f78");
    private static final Color DEEP = Color.web("#061433");
    private static final Color SAND = Color.web("#d9c38a");
    private static final Color GRASS = Color.web("#3f9a55");
    private static final Color FOREST = Color.web("#1f5e3a");
    private static final Color DESERT = Color.web("#c08a4a");
    private static final Color ROCK = Color.web("#6e5f52");
    private static final Color SNOW = Color.web("#f1f5fb");
    private static final Color ICE = Color.web("#e6f1ff");

    private static Image[] planetMaps(int w, int h) {
        double sea = 0.52;
        double[] height = new double[w * h];
        int[] diffuse = new int[w * h];
        int[] specular = new int[w * h];
        int[] glow = new int[w * h];
        int[] cloud = new int[w * h];
        int[] normal = new int[w * h];
        IntStream.range(0, h).parallel().forEach(y -> {
            double lat = Math.PI * (y + 0.5) / h - Math.PI / 2;
            double cl = Math.cos(lat);
            double py = Math.sin(lat);
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                double lon = 2 * Math.PI * (x + 0.5) / w;
                double px = cl * Math.cos(lon);
                double pz = cl * Math.sin(lon);
                double warp = fbm(px * 2 + 5.2, py * 2 + 1.3, pz * 2, 3, 7) - 0.5;
                double e = fbm(px * 1.7 + warp * 1.6, py * 1.7 + warp, pz * 1.7 - warp * 1.2, 8, 1);
                height[i] = e;
                double ice = Math.abs(py) + (fbm(px * 6, py * 6, pz * 6, 3, 3) - 0.5) * 0.2;
                Color c;
                double spec;
                double t = (e - sea) / (1 - sea);
                if (e < sea) {
                    double d = Math.min((sea - e) / 0.18, 1);
                    c = d < 0.25 ? SHALLOW.interpolate(OCEAN, d / 0.25) : OCEAN.interpolate(DEEP, (d - 0.25) / 0.75);
                    spec = 0.5;
                } else {
                    double dry = smoothstep(0.52, 0.66, fbm(px * 3 + 9, py * 3, pz * 3, 4, 11));
                    if (t < 0.02) {
                        c = SAND;
                    } else if (t < 0.22) {
                        c = GRASS.interpolate(FOREST, (t - 0.02) / 0.2).interpolate(DESERT, dry);
                    } else if (t < 0.42) {
                        c = FOREST.interpolate(ROCK, (t - 0.22) / 0.2).interpolate(DESERT, dry * 0.6);
                    } else {
                        c = ROCK.interpolate(SNOW, Math.min((t - 0.42) / 0.2, 1));
                    }
                    spec = 0.05;
                }
                if (ice > 0.82) {
                    double k = smoothstep(0.82, 0.9, ice);
                    c = c.interpolate(ICE, k);
                    spec = lerp(spec, 0.45, k);
                }
                diffuse[i] = argb(c);
                specular[i] = argb(1, spec, spec, spec);

                double lights = 0;
                if (e >= sea && ice < 0.8 && t > 0.01 && t < 0.32) {
                    double city = fbm(px * 34, py * 34, pz * 34, 3, 21);
                    lights = smoothstep(0.6, 0.72, city) * 0.85;
                }
                double coast = (1 - Math.min(Math.abs(e - sea) / 0.006, 1)) * (ice < 0.8 ? 0.45 : 0);
                glow[i] = argb(1, lights * 1.0 + coast * 0.2, lights * 0.68 + coast * 0.85, lights * 0.32 + coast);

                double cv = fbm(px * 2.4 + warp * 2.2, py * 5, pz * 2.4 - warp * 1.5, 7, 31);
                double ca = smoothstep(0.52, 0.72, cv) * 0.82;
                cloud[i] = argb(ca, 1, 1, 1);
            }
        });
        IntStream.range(0, h).parallel().forEach(y -> {
            int up = Math.max(y - 1, 0);
            int down = Math.min(y + 1, h - 1);
            for (int x = 0; x < w; x++) {
                int l = (x + w - 1) % w;
                int r = (x + 1) % w;
                double dx = Math.max(height[y * w + r], sea) - Math.max(height[y * w + l], sea);
                double dy = Math.max(height[down * w + x], sea) - Math.max(height[up * w + x], sea);
                double nx = -dx * 60;
                double ny = -dy * 60;
                double len = Math.sqrt(nx * nx + ny * ny + 1);
                normal[y * w + x] = argb(1, nx / len * 0.5 + 0.5, ny / len * 0.5 + 0.5, 1 / len * 0.5 + 0.5);
            }
        });
        return new Image[] {image(w, h, diffuse), image(w, h, normal), image(w, h, specular), image(w, h, glow),
                image(w, h, cloud)};
    }

    private static Image image(int w, int h, int[] pixels) {
        WritableImage image = new WritableImage(w, h);
        image.getPixelWriter().setPixels(0, 0, w, h, PixelFormat.getIntArgbInstance(), pixels, 0, w);
        return image;
    }

    // ---------------------------------------------------------------------
    // Render loop

    private void startLoop() {
        new AnimationTimer() {
            long last;
            long window;
            int count;
            double sum;
            double worst;
            int reports;

            @Override
            public void handle(long now) {
                double dt = last == 0 ? 0 : (now - last) / 1e9;
                last = now;
                if (dt > 0) {
                    double ms = dt * 1000;
                    frameTimes[frameHead] = ms;
                    frameHead = (frameHead + 1) % GRAPH;
                    count++;
                    sum += ms;
                    worst = Math.max(worst, ms);
                    window += (long) (dt * 1e9);
                    if (window >= 500_000_000L) {
                        fpsValue.setText(String.format(Locale.ROOT, "%.0f", count * 1e9 / window));
                        frameValue.setText(String.format(Locale.ROOT, "%.1f", sum / count));
                        worstValue.setText(String.format(Locale.ROOT, "%.1f", worst));
                        updateCounters();
                        if (++reports % 4 == 0) {
                            System.out.printf(Locale.ROOT, "nebula: %s fps, %s ms avg, %s ms worst, %s sprites, %s triangles%n",
                                    fpsValue.getText(), frameValue.getText(), worstValue.getText(),
                                    spritesValue.getText(), trianglesValue.getText());
                        }
                        count = 0;
                        sum = 0;
                        worst = 0;
                        window = 0;
                    }
                    updateGraph();
                }
                double step = paused ? 0 : Math.min(dt, 0.1) * timeScale.getValue();
                time += step;
                updateSpace(step);
                drawGalaxy();
                updateHalo();
                if (showGallery.isSelected() && panels.isVisible()) {
                    for (int i = 0; i < spinners.size(); i++) {
                        spinners.get(i).setRotate(time * 45 + i * 24);
                        orbs.get(i).setTranslateX(Math.cos(time * 1.3 + i) * 36);
                        orbs.get(i).setTranslateY(Math.sin(time * 1.7 + i) * 20);
                        effectUpdates.get(i).accept(time);
                    }
                }
            }
        }.start();
    }

    private void updateGraph() {
        Double[] line = new Double[GRAPH * 2];
        for (int i = 0; i < GRAPH; i++) {
            double ms = frameTimes[(frameHead + i) % GRAPH];
            line[i * 2] = GRAPH_W * i / (GRAPH - 1);
            line[i * 2 + 1] = GRAPH_H - Math.min(ms / 40, 1) * GRAPH_H;
        }
        frameLine.getPoints().setAll(line);
        frameArea.getPoints().setAll(line);
        frameArea.getPoints().addAll(GRAPH_W, GRAPH_H, 0.0, GRAPH_H);
    }

    private void updateCounters() {
        long triangles = 0;
        int nodes = 0;
        triangles += 160L * 160;
        nodes++;
        if (cloudShell.isVisible()) {
            triangles += 128L * 128;
            nodes++;
        }
        if (atmosphere.isVisible()) {
            triangles += 96L * 96;
            nodes++;
        }
        if (moonOrbit.isVisible()) {
            triangles += 80;
            nodes++;
        }
        if (asteroidField.isVisible()) {
            triangles += asteroidField.getChildren().size() * 80L;
            nodes += asteroidField.getChildren().size();
        }
        trianglesValue.setText(String.format(Locale.ROOT, "%,d", triangles));
        nodesValue.setText(String.format(Locale.ROOT, "%,d", nodes));
        spritesValue.setText(String.format(Locale.ROOT, "%,d", sprites));
        double scale = stage.getOutputScaleX();
        sizeValue.setText(String.format(Locale.ROOT, "%.0f×%.0f @%.2f · %.0f×%.0f px", scene.getWidth(),
                scene.getHeight(), scale, scene.getWidth() * scale, scene.getHeight() * scale));
    }
}
