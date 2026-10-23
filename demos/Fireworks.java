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
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

/**
 * A fireworks show drawn with the Prism renderer (Vulkan or OpenGL ES) in an
 * extended stage over a translucent window backdrop: tens of thousands of
 * canvas sprites in thirteen kinds of shells over a city skyline that is
 * mirrored in the water below.
 */
@SuppressWarnings("deprecation")
public class Fireworks implements EyeCandyDemo.Candy {

    // The selection of the candies, in the middle of the header bar
    private Node switcher;

    private static final String CSS = """
        .root {
            -fx-font-family: "Adwaita Sans", "Cantarell", "System";
            -fx-font-size: 13px;
            -fx-base: #22182f;
            -fx-background: transparent;
            -fx-control-inner-background: #1a1226;
            -fx-accent: #ff4fa3;
            -fx-focus-color: #ffb347;
            -fx-faint-focus-color: #ffb34722;
            -fx-text-fill: #fbeefc;
        }
        .label, .check-box { -fx-text-fill: #fbeefc; }
        .muted { -fx-text-fill: #a996b8; }
        .mono { -fx-font-family: "Adwaita Mono", "Monospace"; }
        .glass {
            -fx-background-color: rgba(14, 6, 24, 0.5);
            -fx-background-radius: 18;
            -fx-border-color: rgba(255, 255, 255, 0.16) rgba(255, 255, 255, 0.07)
                rgba(255, 255, 255, 0.04) rgba(255, 255, 255, 0.07);
            -fx-border-radius: 18;
            -fx-padding: 16;
        }
        .section { -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #a996b8; -fx-padding: 8 0 0 0; }
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
        .stat-key { -fx-font-size: 10px; -fx-text-fill: #a996b8; }
        .check-box > .box {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 5;
            -fx-border-color: rgba(255, 255, 255, 0.22);
            -fx-border-radius: 5;
        }
        .check-box:selected > .box {
            -fx-background-color: linear-gradient(to bottom right, #ffb347, #ff4fa3, #8b5cf6);
            -fx-border-color: transparent;
        }
        .check-box:selected > .box > .mark { -fx-background-color: white; }
        .slider .track { -fx-background-color: rgba(255, 255, 255, 0.14); -fx-background-radius: 4; }
        .slider .thumb {
            -fx-background-color: white;
            -fx-effect: dropshadow(gaussian, #ff4fa3, 10, 0.3, 0, 0);
        }
        .text-field {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.18);
            -fx-border-radius: 9;
            -fx-text-fill: #fbeefc;
            -fx-prompt-text-fill: #a996b8;
        }
        .text-field:focused { -fx-border-color: #ffb347; }
        .button {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 9;
            -fx-text-fill: #fbeefc;
            -fx-padding: 6 14 6 14;
            -fx-cursor: hand;
        }
        .button:hover { -fx-background-color: rgba(255, 255, 255, 0.15); }
        .button.accent {
            -fx-background-color: linear-gradient(to right, #ffb347, #ff4fa3, #8b5cf6, #3ee6ff);
            -fx-border-color: transparent;
            -fx-text-fill: white;
            -fx-font-weight: bold;
        }
        .button.shell { -fx-padding: 6 12 6 10; -fx-graphic-text-gap: 8; }
        .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-pane { -fx-padding: 0; }
        .scroll-bar { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: rgba(255, 255, 255, 0.25); -fx-background-radius: 6; }
        .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 0; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-shape: " "; -fx-padding: 0; }
        """;

    private static final String STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));

    private static final int MAX_PARTICLES = 60000;
    private static final int MAX_FLASHES = 48;
    private static final int MAX_SMOKE = 160;
    private static final int STARS = 420;
    private static final int GRAPH = 160;
    private static final double GRAPH_W = 268;
    private static final double GRAPH_H = 56;
    private static final double HORIZON = 0.8;

    private static final String[] PALETTE = {
        "#ffffff", "#ffc861", "#ff8a3d", "#ff3b5c", "#ff5fd2", "#d65cff", "#8b6bff",
        "#4d8dff", "#3ee6ff", "#2effb4", "#7dff4d", "#e5ff4d", "#cfe3ff", "#ffb020"
    };
    private static final int WHITE = 0;
    private static final int GOLD = 1;
    private static final int RED = 3;
    private static final int PINK = 4;
    private static final int SILVER = 12;
    private static final int AMBER = 13;
    private static final int[] RAINBOW = {3, 2, 13, 11, 10, 9, 8, 7, 6, 5, 4};
    private static final int[] BRIGHT = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 1, 12};

    private static final int ROCKET = 1;
    private static final int STROBE = 2;
    private static final int CRACKLE = 4;
    private static final int CROSSETTE = 8;
    private static final int GLITTER = 16;

    private enum Shell {
        PEONY("Peony", "#ff3b5c"),
        CHRYSANTHEMUM("Chrysanthemum", "#ffc861"),
        DAHLIA("Rainbow dahlia", "#7dff4d"),
        RING("Ring", "#3ee6ff"),
        SATURN("Saturn", "#8b6bff"),
        WILLOW("Willow", "#ffb020"),
        PALM("Palm", "#ff8a3d"),
        CROSSETTE("Crossette", "#2effb4"),
        STROBE("Strobe", "#cfe3ff"),
        CRACKLE("Crackle", "#e5ff4d"),
        HEART("Heart", "#ff5fd2"),
        STAR("Star", "#ffd84d"),
        TEXT("Message", "#d65cff");

        final String label;
        final String color;

        Shell(String label, String color) {
            this.label = label;
            this.color = color;
        }
    }

    private Stage stage;
    private Scene scene;
    private boolean backdrop;
    private double time;
    private boolean paused;
    private double unit = 1;
    private final Random random = new Random(2026);
    private long glitterSeed = 0x9E3779B97F4A7C15L;

    private final Canvas sky = new Canvas();
    private final Canvas canvas = new Canvas();
    private final Canvas front = new Canvas();
    private Image[] sparks;
    private Image[] glows;
    private Image smokeSprite;
    private Image starSprite;
    private Image skyline;
    private Image skylineLit;
    private double skylineW = -1;
    private double skylineH = -1;
    private long skylineBuilt;
    private final List<double[]> beacons = new ArrayList<>();

    private final float[] px = new float[MAX_PARTICLES];
    private final float[] py = new float[MAX_PARTICLES];
    private final float[] vx = new float[MAX_PARTICLES];
    private final float[] vy = new float[MAX_PARTICLES];
    private final float[] age = new float[MAX_PARTICLES];
    private final float[] life = new float[MAX_PARTICLES];
    private final float[] size = new float[MAX_PARTICLES];
    private final float[] drag = new float[MAX_PARTICLES];
    private final float[] grav = new float[MAX_PARTICLES];
    private final float[] rate = new float[MAX_PARTICLES];
    private final float[] accum = new float[MAX_PARTICLES];
    private final float[] phase = new float[MAX_PARTICLES];
    private final byte[] color1 = new byte[MAX_PARTICLES];
    private final byte[] color2 = new byte[MAX_PARTICLES];
    private final byte[] flags = new byte[MAX_PARTICLES];
    private final byte[] shellOf = new byte[MAX_PARTICLES];
    private int count;

    private final double[][] flashes = new double[MAX_FLASHES][4];
    private int flashCount;
    private final double[][] smoke = new double[MAX_SMOKE][7];
    private int smokeCount;
    private final double[][] starField = new double[STARS][4];

    private float[] textPoints = new float[0];
    private double nextLaunch;
    private double finaleUntil = -1;
    private boolean finaleBouquet;
    private long bursts;
    private int rockets;
    private int sprites;

    private final CheckBox autoShow = new CheckBox("Auto show");
    private final CheckBox salvos = new CheckBox("Salvos");
    private final CheckBox trails = new CheckBox("Streaks (3 sprites per spark)");
    private final CheckBox reflections = new CheckBox("Water reflections");
    private final CheckBox city = new CheckBox("City skyline");
    private final CheckBox skyFlash = new CheckBox("Sky flashes");
    private final CheckBox smokeOn = new CheckBox("Smoke");
    private final CheckBox additive = new CheckBox("Additive sprites (Decora per draw)");
    private final CheckBox bloom = new CheckBox("Bloom (Decora)");
    private final Map<Shell, CheckBox> enabled = new EnumMap<>(Shell.class);
    private final Slider intensity = new Slider(0.2, 6, 1.6);
    private final Slider gravity = new Slider(0, 2, 1);
    private final Slider wind = new Slider(-1, 1, 0.12);
    private final Slider timeScale = new Slider(0, 3, 1);
    private final Slider budget = new Slider(1000, MAX_PARTICLES, 20000);
    private final Slider tint = new Slider(0, 0.9, 0.3);
    private final TextField message = new TextField("JavaFX");

    private final Label fpsValue = new Label("–");
    private final Label frameValue = new Label("–");
    private final Label worstValue = new Label("–");
    private final Label particlesValue = new Label("–");
    private final Label rocketsValue = new Label("–");
    private final Label burstsValue = new Label("–");
    private final Label spritesValue = new Label("–");
    private final Label sizeValue = new Label("–");
    private final Polyline frameLine = new Polyline();
    private final Polygon frameArea = new Polygon();
    private final double[] frameTimes = new double[GRAPH];
    private int frameHead;
    private final Region fill = new Region();
    private final Text title = new Text("FIREWORKS");
    private final Group logo = new Group();
    private Node panels;

    @Override
    public Scene scene(Stage stage, Node switcher, double width, double height) {
        this.switcher = switcher;
        this.stage = stage;
        backdrop = Boolean.getBoolean("glass.gtk4.backdrop.active");

        for (CheckBox c : List.of(autoShow, salvos, trails, reflections, city, skyFlash, smokeOn, bloom)) {
            c.setSelected(true);
        }
        for (Shell s : Shell.values()) {
            CheckBox c = new CheckBox(s.label);
            c.setSelected(true);
            enabled.put(s, c);
        }

        updateFill();
        tint.valueProperty().addListener(o -> updateFill());
        tint.setDisable(!backdrop);

        Bloom glow = new Bloom(0.45);
        canvas.effectProperty().bind(Bindings.when(bloom.selectedProperty()).then((Effect) glow).otherwise((Effect) null));
        canvas.setMouseTransparent(true);
        sky.setMouseTransparent(true);
        front.setMouseTransparent(true);
        Pane canvasLayer = new Pane(sky, canvas, front);
        for (Canvas c : List.of(sky, canvas, front)) {
            c.setManaged(false);
            c.widthProperty().bind(canvasLayer.widthProperty());
            c.heightProperty().bind(canvasLayer.heightProperty());
        }
        canvasLayer.setMinSize(0, 0);
        seedSprites();
        buildTextPoints();
        message.setOnAction(e -> {
            buildTextPoints();
            fire(Shell.TEXT, canvas.getWidth() * 0.5, canvas.getHeight() * 0.3);
        });

        BorderPane overlay = new BorderPane();
        overlay.setTop(header());
        overlay.setCenter(panels());
        overlay.setPickOnBounds(false);

        StackPane root = new StackPane(fill, canvasLayer, overlay);
        root.setMinSize(0, 0);
        scene = new Scene(root, width, height, Color.TRANSPARENT);
        scene.getStylesheets().add(STYLESHEET);
        scene.getPreferences().setColorScheme(ColorScheme.DARK);
        mouse(root);
        keys();

        startLoop();
        return scene;
    }

    private void updateFill() {
        if (backdrop) {
            double a = tint.getValue();
            fill.setBackground(new Background(
                    new BackgroundFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.color(0.01, 0.01, 0.06, a)),
                            new Stop(0.7, Color.color(0.07, 0.03, 0.14, a)),
                            new Stop(1, Color.color(0.02, 0.02, 0.06, Math.min(1, a + 0.3)))), null, null)));
        } else {
            fill.setBackground(new Background(
                    new BackgroundFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.web("#03020c")), new Stop(0.45, Color.web("#0b0722")),
                            new Stop(0.78, Color.web("#24113f")), new Stop(1, Color.web("#05040e"))), null, null)));
        }
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

    private Node header() {
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            Line ray = new Line(Math.cos(a) * 4, Math.sin(a) * 4, Math.cos(a) * 11, Math.sin(a) * 11);
            ray.setStroke(Color.web(PALETTE[BRIGHT[i]]));
            ray.setStrokeWidth(2);
            ray.setStrokeLineCap(StrokeLineCap.ROUND);
            logo.getChildren().add(ray);
        }
        logo.getChildren().add(new Circle(2.5, Color.WHITE));
        logo.setEffect(new Glow(0.9));
        Label brand = new Label("Fireworks");
        brand.getStyleClass().add("brand");
        Label sub = new Label("Prism renderer showcase");
        sub.getStyleClass().add("muted");
        HBox left = new HBox(10, logo, brand, sub);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 18));

        Circle dot = new Circle(4, Color.web("#ff4fa3"));
        dot.setEffect(new DropShadow(8, Color.web("#ff4fa3")));
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

        Button finale = new Button("Finale  G");
        finale.getStyleClass().add("accent");
        finale.setOnAction(e -> finale());
        Button hide = new Button("Panels  H");
        hide.setOnAction(e -> togglePanels());

        HBox right = new HBox(10, live, pipeline, glass, finale, hide);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setPadding(new Insets(0, 10, 0, 0));

        HeaderBar.setDragType(left, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar.setDragType(live, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar.setDragType(pipeline, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar.setDragType(glass, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar.setDragType(right, HeaderDragType.DRAGGABLE);

        HeaderBar header = new HeaderBar(left, switcher, right);
        header.setBackground(null);
        header.setMinHeight(54);
        HeaderBar.setAlignment(left, Pos.CENTER_LEFT);
        return header;
    }

    private void togglePanels() {
        panels.setVisible(!panels.isVisible());
    }

    private void keys() {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getTarget() instanceof TextField) {
                return;
            }
            KeyCode code = e.getCode();
            if (code == KeyCode.H) {
                togglePanels();
            } else if (code == KeyCode.F || code == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
            } else if (code == KeyCode.SPACE || code == KeyCode.P) {
                paused = !paused;
            } else if (code == KeyCode.G) {
                finale();
            } else if (code == KeyCode.C) {
                clear();
            } else if (code.isDigitKey()) {
                int index = (code.getCode() - KeyCode.DIGIT0.getCode() + 9) % 10;
                if (code.getCode() >= KeyCode.NUMPAD0.getCode()) {
                    index = (code.getCode() - KeyCode.NUMPAD0.getCode() + 9) % 10;
                }
                launchRandomTarget(Shell.values()[index]);
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

    private void mouse(Node root) {
        root.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (onChrome(e.getTarget())) {
                return;
            }
            Point2D p = canvas.sceneToLocal(e.getSceneX(), e.getSceneY());
            if (p.getY() > canvas.getHeight() * HORIZON - 20 * unit) {
                return;
            }
            Shell shell = randomShell();
            if (e.getButton() == MouseButton.SECONDARY || e.isShiftDown()) {
                burst(shell, p.getX(), p.getY(), BRIGHT[random.nextInt(BRIGHT.length)]);
            } else {
                fire(shell, p.getX(), p.getY());
            }
        });
    }

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

        Node launcher = launcher();
        AnchorPane.setLeftAnchor(launcher, 18.0);
        AnchorPane.setRightAnchor(launcher, 336.0);
        AnchorPane.setBottomAnchor(launcher, 18.0);

        area.getChildren().addAll(hud, controls, launcher);
        panels = area;
        return area;
    }

    private Node hud() {
        title.setFont(Font.font("Adwaita Sans", FontWeight.BLACK, 54));
        Reflection reflection = new Reflection(0, 0.45, 0.4, 0);
        reflection.setInput(new DropShadow(BlurType.GAUSSIAN, Color.web("#ff4fa3cc"), 28, 0.25, 0, 0));
        title.setEffect(reflection);
        Label subtitle = new Label("Prism · " + pipelineName() + " · " + System.getProperty("glass.platform", "gtk")
                + " glass · click the sky to launch");
        subtitle.getStyleClass().add("muted");

        GridPane stats = new GridPane(22, 10);
        stats.add(stat("FPS", fpsValue), 0, 0);
        stats.add(stat("Frame ms", frameValue), 1, 0);
        stats.add(stat("Worst ms", worstValue), 2, 0);
        stats.add(stat("Particles", particlesValue), 0, 1);
        stats.add(stat("Rockets", rocketsValue), 1, 1);
        stats.add(stat("Bursts", burstsValue), 2, 1);

        Pane graph = new Pane();
        graph.setPrefSize(GRAPH_W, GRAPH_H);
        graph.setMinSize(GRAPH_W, GRAPH_H);
        graph.setMaxSize(GRAPH_W, GRAPH_H);
        double target = GRAPH_H - GRAPH_H * 16.7 / 40;
        Line limit = new Line(0, target, GRAPH_W, target);
        limit.setStroke(Color.color(1, 1, 1, 0.3));
        limit.getStrokeDashArray().addAll(4.0, 4.0);
        frameArea.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ff4fa366")), new Stop(1, Color.web("#ffb34700"))));
        frameLine.setStroke(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffb347")), new Stop(0.5, Color.web("#ff4fa3")), new Stop(1, Color.web("#3ee6ff"))));
        frameLine.setStrokeWidth(1.6);
        frameLine.setStrokeLineJoin(StrokeLineJoin.ROUND);
        Text limitLabel = new Text(GRAPH_W - 52, target - 4, "16.7 ms");
        limitLabel.setFill(Color.color(1, 1, 1, 0.45));
        limitLabel.setFont(Font.font("Adwaita Mono", 10));
        graph.getChildren().addAll(frameArea, frameLine, limit, limitLabel);

        sizeValue.getStyleClass().addAll("muted", "mono");
        spritesValue.getStyleClass().addAll("muted", "mono");

        VBox card = new VBox(12, stats, graph, new VBox(3, sizeValue, spritesValue));
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
        GridPane shells = new GridPane(10, 6);
        Shell[] all = Shell.values();
        for (int i = 0; i < all.length; i++) {
            shells.add(enabled.get(all[i]), i % 2, i / 2);
        }

        message.setPromptText("Text shell message");
        Button say = new Button("Launch");
        say.setOnAction(message.getOnAction());
        HBox.setHgrow(message, Priority.ALWAYS);
        HBox text = new HBox(8, message, say);

        Button finale = new Button("Finale");
        finale.getStyleClass().add("accent");
        finale.setOnAction(e -> finale());
        Button clear = new Button("Clear");
        clear.setOnAction(e -> clear());
        Button full = new Button("Full screen");
        full.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));
        HBox buttons = new HBox(8, finale, clear, full);

        Label hint = new Label("Click the sky to launch · right click to burst\n"
                + "1–0 shells · G finale · C clear\nH panels · F full screen · Space pause");
        hint.getStyleClass().add("muted");

        VBox box = new VBox(8,
                section("SHOW"), autoShow, salvos, sliderRow("Rockets per second", intensity, "%.1f"),
                section("SHELLS"), shells,
                section("TEXT SHELL"), text,
                section("PHYSICS"),
                sliderRow("Gravity", gravity, "%.2f×"),
                sliderRow("Wind", wind, "%+.2f"),
                sliderRow("Time scale", timeScale, "%.2f×"),
                sliderRow("Particle budget", budget, "%,.0f"),
                section("LOOK"), trails, reflections, city, skyFlash, smokeOn, additive, bloom,
                section("WINDOW"),
                sliderRow("Backdrop tint", tint, "%.2f"),
                spacer(4), buttons, spacer(4), hint);
        box.setPadding(new Insets(0, 12, 0, 0));
        return box;
    }

    private Node launcher() {
        FlowPane buttons = new FlowPane(8, 8);
        Shell[] all = Shell.values();
        for (int i = 0; i < all.length; i++) {
            Shell s = all[i];
            Circle dot = new Circle(5, new RadialGradient(0, 0, 0.35, 0.35, 0.7, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.WHITE), new Stop(1, Color.web(s.color))));
            dot.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.web(s.color), 8, 0.4, 0, 0));
            Button b = new Button(i < 10 ? s.label + "  " + (i + 1) % 10 : s.label, dot);
            b.getStyleClass().add("shell");
            b.setOnAction(e -> launchRandomTarget(s));
            buttons.getChildren().add(b);
        }
        VBox box = new VBox(8, section("LAUNCH A SHELL"), buttons);
        box.getStyleClass().add("glass");
        box.setPadding(new Insets(10, 14, 14, 14));
        box.setMaxHeight(Region.USE_PREF_SIZE);
        return box;
    }

    private static Image sprite(Color color, int size, double sharpness) {
        return sprite(color, size, sharpness, true);
    }

    private static Image sprite(Color color, int size, double sharpness, boolean core) {
        WritableImage image = new WritableImage(size, size);
        PixelWriter pw = image.getPixelWriter();
        double h = (size - 1) / 2.0;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                double dx = (x - h) / h;
                double dy = (y - h) / h;
                double d2 = dx * dx + dy * dy;
                double a = d2 >= 1 ? 0 : Math.exp(-d2 * sharpness) * (1 - d2);
                Color c = core ? color.interpolate(Color.WHITE, Math.exp(-d2 * sharpness * 5)) : color;
                pw.setColor(x, y, Color.color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(1, a)));
            }
        }
        return image;
    }

    private void seedSprites() {
        sparks = new Image[PALETTE.length];
        glows = new Image[PALETTE.length];
        for (int i = 0; i < PALETTE.length; i++) {
            sparks[i] = sprite(Color.web(PALETTE[i]), 32, 6);
            glows[i] = sprite(Color.web(PALETTE[i]), 128, 3.2, false);
        }
        smokeSprite = sprite(Color.web("#a89cc0"), 96, 2.6, false);
        starSprite = sprite(Color.web("#e8ecff"), 16, 8);
        for (double[] s : starField) {
            s[0] = random.nextDouble();
            s[1] = Math.pow(random.nextDouble(), 1.3) * HORIZON * 0.92;
            s[2] = 2 + random.nextDouble() * 4;
            s[3] = random.nextDouble() * Math.PI * 2;
        }
    }

    private void buildTextPoints() {
        String msg = message.getText() == null || message.getText().isBlank() ? "JavaFX" : message.getText().strip();
        Text t = new Text(msg);
        t.setFont(Font.font("Adwaita Sans", FontWeight.BLACK, 120));
        t.setFill(Color.WHITE);
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        WritableImage image = t.snapshot(params, null);
        int w = (int) image.getWidth();
        int h = (int) image.getHeight();
        PixelReader pr = image.getPixelReader();
        int filled = 0;
        for (int y = 0; y < h; y += 2) {
            for (int x = 0; x < w; x += 2) {
                if (pr.getColor(x, y).getOpacity() > 0.5) {
                    filled++;
                }
            }
        }
        int step = Math.max(3, (int) Math.round(2 * Math.sqrt(filled / 420.0)));
        List<float[]> points = new ArrayList<>();
        double half = Math.max(w, h) / 2.0;
        for (int y = 0; y < h; y += step) {
            for (int x = (y / step % 2) * step / 2; x < w; x += step) {
                if (pr.getColor(x, y).getOpacity() > 0.5) {
                    points.add(new float[] {(float) ((x - w / 2.0) / half), (float) ((y - h / 2.0) / half)});
                }
            }
        }
        textPoints = new float[points.size() * 2];
        for (int i = 0; i < points.size(); i++) {
            textPoints[i * 2] = points.get(i)[0];
            textPoints[i * 2 + 1] = points.get(i)[1];
        }
    }

    private void ensureSkyline(double w, double h) {
        long now = System.nanoTime();
        if ((w == skylineW && h == skylineH) || w < 1 || h < 1 || now - skylineBuilt < 150_000_000L) {
            return;
        }
        skylineW = w;
        skylineH = h;
        skylineBuilt = now;
        double horizon = h * HORIZON;
        double top = h * 0.26;
        Canvas dark = new Canvas(w, top);
        Canvas lit = new Canvas(w, top);
        GraphicsContext gd = dark.getGraphicsContext2D();
        GraphicsContext gl = lit.getGraphicsContext2D();
        Random r = new Random(3);
        beacons.clear();
        Color[] windows = {Color.web("#ffd27a"), Color.web("#ffb35c"), Color.web("#bfe3ff"), Color.web("#ff9ad5")};
        double x = -10;
        while (x < w) {
            double bw = (26 + r.nextDouble() * 70) * unit;
            boolean tower = r.nextDouble() < 0.12;
            double bh = (tower ? 0.55 + r.nextDouble() * 0.4 : 0.15 + Math.pow(r.nextDouble(), 1.6) * 0.5) * top;
            double y = top - bh;
            double shade = 0.03 + r.nextDouble() * 0.04;
            gd.setFill(Color.color(shade, shade * 0.8, shade * 1.6 + 0.02));
            gd.fillRect(x, y, bw, bh);
            gl.setFill(Color.color(0.75, 0.7, 0.9));
            gl.fillRect(x, y, bw, bh);
            if (tower) {
                double ax = x + bw / 2;
                gd.setStroke(Color.color(shade, shade, shade * 1.8));
                gd.setLineWidth(2 * unit);
                gd.strokeLine(ax, y, ax, y - 22 * unit);
                beacons.add(new double[] {ax, horizon - top + y - 22 * unit, r.nextDouble() * 6});
            }
            double cw = 7 * unit;
            double ch = 10 * unit;
            for (double wy = y + 6 * unit; wy < top - ch; wy += ch + 4 * unit) {
                for (double wx = x + 5 * unit; wx < x + bw - cw - 2 * unit; wx += cw + 5 * unit) {
                    if (r.nextDouble() < 0.38) {
                        Color c = windows[r.nextInt(windows.length)];
                        gd.setFill(c.deriveColor(0, 1, 0.6 + r.nextDouble() * 0.4, 0.55 + r.nextDouble() * 0.4));
                        gd.fillRect(wx, wy, cw, ch);
                    }
                }
            }
            x += bw + (r.nextDouble() < 0.2 ? r.nextDouble() * 18 * unit : 0);
        }
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        skyline = dark.snapshot(params, null);
        skylineLit = lit.snapshot(params, null);
    }

    private int index(int color) {
        return color & 0xff;
    }

    private boolean spawn(double x, double y, double dx, double dy, double lifetime, double sz, double k,
                          double g, int c1, int c2, int flag, double emit) {
        if (count >= Math.min(MAX_PARTICLES, (int) budget.getValue())) {
            return false;
        }
        int i = count++;
        px[i] = (float) x;
        py[i] = (float) y;
        vx[i] = (float) dx;
        vy[i] = (float) dy;
        age[i] = 0;
        life[i] = (float) lifetime;
        size[i] = (float) sz;
        drag[i] = (float) k;
        grav[i] = (float) g;
        rate[i] = (float) emit;
        accum[i] = 0;
        phase[i] = random.nextFloat();
        color1[i] = (byte) c1;
        color2[i] = (byte) c2;
        flags[i] = (byte) flag;
        shellOf[i] = 0;
        return true;
    }

    private void kill(int i) {
        int last = --count;
        px[i] = px[last];
        py[i] = py[last];
        vx[i] = vx[last];
        vy[i] = vy[last];
        age[i] = age[last];
        life[i] = life[last];
        size[i] = size[last];
        drag[i] = drag[last];
        grav[i] = grav[last];
        rate[i] = rate[last];
        accum[i] = accum[last];
        phase[i] = phase[last];
        color1[i] = color1[last];
        color2[i] = color2[last];
        flags[i] = flags[last];
        shellOf[i] = shellOf[last];
    }

    private void clear() {
        count = 0;
        flashCount = 0;
        smokeCount = 0;
        finaleUntil = -1;
    }

    private Shell randomShell() {
        List<Shell> on = new ArrayList<>();
        for (Shell s : Shell.values()) {
            if (enabled.get(s).isSelected()) {
                on.add(s);
            }
        }
        if (on.isEmpty()) {
            return Shell.PEONY;
        }
        return on.get(random.nextInt(on.size()));
    }

    private double gravityPx() {
        return 300 * unit * gravity.getValue();
    }

    private void launchRandomTarget(Shell shell) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        fire(shell, w * (0.15 + random.nextDouble() * 0.7), h * (0.14 + random.nextDouble() * 0.28));
    }

    private void fire(Shell shell, double tx, double ty) {
        double h = canvas.getHeight();
        double w = canvas.getWidth();
        double x0 = Math.max(0, Math.min(w, tx + (random.nextDouble() - 0.5) * w * 0.18));
        double y0 = h * HORIZON;
        double g = Math.max(gravityPx(), 120 * unit);
        double rise = Math.max(40 * unit, y0 - ty);
        double v0 = Math.sqrt(2 * g * rise);
        double t = v0 / g;
        if (spawn(x0, y0, (tx - x0) / t, -v0, t + 1, 5.5, 0, g, AMBER, BRIGHT[random.nextInt(BRIGHT.length)], ROCKET, 75)) {
            shellOf[count - 1] = (byte) shell.ordinal();
        }
    }

    private void finale() {
        finaleUntil = time + 7;
        finaleBouquet = true;
    }

    private void addFlash(double x, double y, int color) {
        if (flashCount == MAX_FLASHES) {
            System.arraycopy(flashes, 1, flashes, 0, MAX_FLASHES - 1);
            flashes[MAX_FLASHES - 1] = new double[4];
            flashCount--;
        }
        double[] f = flashes[flashCount++];
        f[0] = x;
        f[1] = y;
        f[2] = color;
        f[3] = 0;
    }

    private void addSmoke(double x, double y) {
        for (int k = 0; k < 3 && smokeCount < MAX_SMOKE; k++) {
            double[] s = smoke[smokeCount++];
            s[0] = x + (random.nextDouble() - 0.5) * 80 * unit;
            s[1] = y + (random.nextDouble() - 0.5) * 60 * unit;
            s[2] = (random.nextDouble() - 0.5) * 12 * unit;
            s[3] = -4 * unit - random.nextDouble() * 6 * unit;
            s[4] = 0;
            s[5] = 6 + random.nextDouble() * 4;
            s[6] = (160 + random.nextDouble() * 180) * unit;
        }
    }

    private double gauss() {
        return random.nextGaussian();
    }

    private void burst(Shell shell, double x, double y, int c) {
        bursts++;
        double s = 380 * unit;
        int alt = BRIGHT[random.nextInt(BRIGHT.length)];
        if (alt == c) {
            alt = WHITE;
        }
        addFlash(x, y, shell == Shell.WILLOW ? GOLD : c);
        addSmoke(x, y);
        switch (shell) {
            case PEONY -> {
                boolean split = random.nextBoolean();
                for (int i = 0; i < 170; i++) {
                    double[] d = sphere();
                    double v = s * (0.92 + random.nextDouble() * 0.1);
                    int cc = split && i % 2 == 0 ? alt : c;
                    spawn(x, y, d[0] * v, d[1] * v, 1.5 + random.nextDouble() * 0.5, 8, 1.5, 1, cc, cc, 0, 0);
                }
            }
            case CHRYSANTHEMUM -> {
                for (int i = 0; i < 140; i++) {
                    double[] d = sphere();
                    double v = s * (0.92 + random.nextDouble() * 0.1);
                    spawn(x, y, d[0] * v, d[1] * v, 1.7 + random.nextDouble() * 0.4, 8, 1.5, 1, c, GOLD, 0, 9);
                }
            }
            case DAHLIA -> {
                for (int i = 0; i < 190; i++) {
                    double[] d = sphere();
                    double a = Math.atan2(d[1], d[0]) / (Math.PI * 2) + 0.5;
                    int cc = RAINBOW[(int) (a * RAINBOW.length) % RAINBOW.length];
                    double v = s * (0.95 + random.nextDouble() * 0.08);
                    spawn(x, y, d[0] * v, d[1] * v, 1.7 + random.nextDouble() * 0.3, 8.5, 1.5, 1, cc, cc, 0, 0);
                }
            }
            case RING -> {
                ring(x, y, s, 110, random.nextDouble() * 1.2, random.nextDouble() * Math.PI, c, 0.4);
                if (random.nextBoolean()) {
                    ring(x, y, s * 0.55, 70, random.nextDouble() * 1.2, random.nextDouble() * Math.PI, alt, 0.4);
                }
            }
            case SATURN -> {
                for (int i = 0; i < 80; i++) {
                    double[] d = sphere();
                    double v = s * 0.42 * (0.9 + random.nextDouble() * 0.15);
                    spawn(x, y, d[0] * v, d[1] * v, 1.8, 7.5, 1.5, 0.4, c, c, 0, 0);
                }
                ring(x, y, s * 1.05, 120, 1.15, (random.nextDouble() - 0.5) * 0.7, alt, 0.4);
            }
            case WILLOW -> {
                for (int i = 0; i < 120; i++) {
                    double[] d = sphere();
                    double v = s * 0.85 * (0.9 + random.nextDouble() * 0.15);
                    spawn(x, y, d[0] * v, d[1] * v, 3.6 + random.nextDouble() * 0.8, 6.5, 2.8, 1, GOLD, AMBER,
                            GLITTER, 7);
                }
            }
            case PALM -> {
                int arms = 7 + random.nextInt(4);
                double base = random.nextDouble() * Math.PI * 2;
                for (int i = 0; i < arms; i++) {
                    double a = base + Math.PI * 2 * i / arms + (random.nextDouble() - 0.5) * 0.25;
                    double v = s * 1.05;
                    spawn(x, y, Math.cos(a) * v, Math.sin(a) * v - s * 0.15, 1.6, 11, 1.0, 1, GOLD, AMBER, 0, 55);
                }
                for (int i = 0; i < 40; i++) {
                    double[] d = sphere();
                    double v = s * 0.35;
                    spawn(x, y, d[0] * v, d[1] * v, 1.2, 7, 1.5, 1, c, c, 0, 0);
                }
            }
            case CROSSETTE -> {
                for (int i = 0; i < 26; i++) {
                    double[] d = sphere();
                    double v = s * 0.78 * (0.9 + random.nextDouble() * 0.2);
                    spawn(x, y, d[0] * v, d[1] * v, 1.5, 9, 1.2, 1, c, alt, CROSSETTE, 30);
                }
            }
            case STROBE -> {
                int sc = random.nextBoolean() ? WHITE : SILVER;
                for (int i = 0; i < 140; i++) {
                    double[] d = sphere();
                    double v = s * 0.8 * (0.9 + random.nextDouble() * 0.15);
                    spawn(x, y, d[0] * v, d[1] * v, 2.4 + random.nextDouble() * 0.6, 7.5, 1.7, 0.8, sc, sc, STROBE, 0);
                }
            }
            case CRACKLE -> {
                for (int i = 0; i < 100; i++) {
                    double[] d = sphere();
                    double v = s * 0.9 * (0.85 + random.nextDouble() * 0.2);
                    spawn(x, y, d[0] * v, d[1] * v, 1.2 + random.nextDouble() * 0.5, 7, 1.6, 1, GOLD, c, CRACKLE, 0);
                }
            }
            case HEART -> {
                double rot = (random.nextDouble() - 0.5) * 0.5;
                int hc = random.nextBoolean() ? PINK : RED;
                for (int i = 0; i < 130; i++) {
                    double t = Math.PI * 2 * i / 130;
                    double hx = 16 * Math.pow(Math.sin(t), 3) / 17;
                    double hy = -(13 * Math.cos(t) - 5 * Math.cos(2 * t) - 2 * Math.cos(3 * t) - Math.cos(4 * t)) / 17;
                    double rx = hx * Math.cos(rot) - hy * Math.sin(rot);
                    double ry = hx * Math.sin(rot) + hy * Math.cos(rot);
                    double v = s * 0.95;
                    spawn(x, y, rx * v, ry * v, 2.1, 8, 1.6, 0.3, hc, i % 3 == 0 ? WHITE : hc, 0, 0);
                }
            }
            case STAR -> {
                double rot = random.nextDouble() * Math.PI * 2;
                int per = 14;
                for (int e = 0; e < 10; e++) {
                    double a0 = rot + Math.PI * e / 5;
                    double a1 = rot + Math.PI * (e + 1) / 5;
                    double r0 = e % 2 == 0 ? 1 : 0.42;
                    double r1 = e % 2 == 0 ? 0.42 : 1;
                    for (int k = 0; k < per; k++) {
                        double f = k / (double) per;
                        double sx = lerp(Math.cos(a0) * r0, Math.cos(a1) * r1, f);
                        double sy = lerp(Math.sin(a0) * r0, Math.sin(a1) * r1, f);
                        spawn(x, y, sx * s, sy * s, 2.1, 8, 1.6, 0.3, c, alt, 0, 0);
                    }
                }
            }
            case TEXT -> {
                int n = textPoints.length / 2;
                for (int i = 0; i < n; i++) {
                    double tx = textPoints[i * 2];
                    double ty = textPoints[i * 2 + 1];
                    int cc = RAINBOW[(int) Math.min(RAINBOW.length - 1, (tx + 1) / 2 * RAINBOW.length)];
                    double v = s * 1.25;
                    spawn(x, y, tx * v, ty * v, 2.6 + random.nextDouble() * 0.3, 7, 2.2, 0.22, cc, cc, 0, 0);
                }
            }
        }
    }

    private void ring(double x, double y, double speed, int n, double tilt, double rot, int c, double g) {
        double cr = Math.cos(rot);
        double sr = Math.sin(rot);
        double ct = Math.cos(tilt);
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            double rx = Math.cos(a);
            double ry = Math.sin(a) * ct;
            double v = speed * (0.98 + random.nextDouble() * 0.04);
            spawn(x, y, (rx * cr - ry * sr) * v, (rx * sr + ry * cr) * v, 1.9, 8, 1.5, g, c, c, 0, 0);
        }
    }

    private double[] sphere() {
        double a = gauss();
        double b = gauss();
        double c = gauss();
        double l = Math.sqrt(a * a + b * b + c * c);
        return new double[] {a / l, b / l};
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }

    private float glitter() {
        glitterSeed ^= glitterSeed << 13;
        glitterSeed ^= glitterSeed >>> 7;
        glitterSeed ^= glitterSeed << 17;
        return (glitterSeed >>> 40) / (float) (1 << 24);
    }

    private void autoLaunch() {
        boolean finale = time < finaleUntil;
        if (!autoShow.isSelected() && !finale) {
            return;
        }
        if (time < nextLaunch) {
            return;
        }
        double perSecond = finale ? intensity.getValue() * 5 + 6 : intensity.getValue();
        nextLaunch = time - Math.log(1 - random.nextDouble()) / perSecond;
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        if (salvos.isSelected() && random.nextDouble() < (finale ? 0.3 : 0.12)) {
            Shell shell = randomShell();
            int n = 3 + random.nextInt(3);
            double ty = h * (0.16 + random.nextDouble() * 0.18);
            for (int i = 0; i < n; i++) {
                double tx = w * (0.5 + (i - (n - 1) / 2.0) * 0.14);
                fire(shell, tx, ty + Math.abs(i - (n - 1) / 2.0) * h * 0.04);
            }
        } else {
            launchRandomTarget(randomShell());
        }
    }

    private void step(double dt) {
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        double horizon = h * HORIZON;
        double g = gravityPx();
        double windPx = wind.getValue() * 60 * unit;
        int soft = (int) (Math.min(MAX_PARTICLES, budget.getValue()) * 0.92);
        rockets = 0;
        if (finaleBouquet && finaleUntil > 0 && time >= finaleUntil) {
            finaleBouquet = false;
            Shell[] all = Shell.values();
            for (int i = 0; i < 9; i++) {
                fire(all[random.nextInt(all.length - 1)], w * (0.12 + 0.76 * i / 8.0), h * (0.14 + random.nextDouble() * 0.12));
            }
        }
        for (int i = 0; i < count; i++) {
            age[i] += (float) dt;
            int f = flags[i];
            if ((f & ROCKET) != 0) {
                rockets++;
                if (vy[i] >= -grav[i] * 0.02 || age[i] >= life[i]) {
                    double bx = px[i];
                    double by = py[i];
                    int bc = index(color2[i]);
                    Shell shell = Shell.values()[shellOf[i]];
                    kill(i--);
                    burst(shell, bx, by, bc);
                    continue;
                }
            } else if (age[i] >= life[i]) {
                if ((f & CRACKLE) != 0) {
                    double cx = px[i];
                    double cy = py[i];
                    kill(i--);
                    for (int k = 0; k < 3 && count < soft; k++) {
                        double a = random.nextDouble() * Math.PI * 2;
                        double v = (40 + random.nextDouble() * 50) * unit;
                        spawn(cx, cy, Math.cos(a) * v, Math.sin(a) * v, 0.1 + random.nextDouble() * 0.15, 4.5, 6, 0.2,
                                WHITE, WHITE, 0, 0);
                    }
                } else {
                    kill(i--);
                }
                continue;
            } else if ((f & CROSSETTE) != 0 && age[i] > life[i] * 0.42) {
                double cx = px[i];
                double cy = py[i];
                double a0 = Math.atan2(vy[i], vx[i]) + Math.PI / 4;
                int cc = index(color2[i]);
                kill(i--);
                for (int k = 0; k < 4; k++) {
                    double a = a0 + Math.PI / 2 * k;
                    double v = 170 * unit;
                    spawn(cx, cy, Math.cos(a) * v, Math.sin(a) * v, 0.8 + random.nextDouble() * 0.2, 7, 1.6, 1, cc, cc,
                            0, 25);
                }
                continue;
            }
            if (rate[i] > 0 && count < soft) {
                accum[i] += rate[i] * (float) dt;
                while (accum[i] >= 1) {
                    accum[i] -= 1;
                    int sc = (f & ROCKET) != 0 ? (glitter() < 0.5 ? AMBER : GOLD) : index(color1[i]);
                    double jitter = 22 * unit;
                    spawn(px[i] - vx[i] * glitter() * (float) dt, py[i] - vy[i] * glitter() * (float) dt,
                            vx[i] * 0.12 + (glitter() - 0.5) * jitter, vy[i] * 0.12 + (glitter() - 0.5) * jitter,
                            0.35 + glitter() * 0.45, (f & ROCKET) != 0 ? 4 : 3.6, 3, 0.6, sc, sc, GLITTER, 0);
                }
            }
            double k = 1 - drag[i] * dt;
            if (k < 0) {
                k = 0;
            }
            vx[i] = (float) ((vx[i] + windPx * dt * (drag[i] > 0 ? 1 : 0.3)) * k);
            vy[i] = (float) ((vy[i] + ((f & ROCKET) != 0 ? grav[i] : g * grav[i]) * dt) * k);
            px[i] += (float) (vx[i] * dt);
            py[i] += (float) (vy[i] * dt);
            if ((f & ROCKET) == 0 && (py[i] > horizon + 4 || px[i] < -200 * unit || px[i] > w + 200 * unit)) {
                kill(i--);
            }
        }
        for (int i = 0; i < flashCount; i++) {
            flashes[i][3] += dt;
            if (flashes[i][3] > 0.5) {
                double[] done = flashes[i];
                flashes[i] = flashes[--flashCount];
                flashes[flashCount] = done;
                i--;
            }
        }
        for (int i = 0; i < smokeCount; i++) {
            double[] s = smoke[i];
            s[4] += dt;
            s[0] += (s[2] + windPx * 0.5) * dt;
            s[1] += s[3] * dt;
            if (s[4] >= s[5]) {
                smoke[i] = smoke[--smokeCount];
                smoke[smokeCount] = s;
                i--;
            }
        }
    }

    private double flashLevel() {
        double level = 0;
        for (int i = 0; i < flashCount; i++) {
            double t = flashes[i][3];
            level += Math.max(0, 1 - t / 0.5);
        }
        return Math.min(1.4, level);
    }

    private int strongestFlash() {
        int best = -1;
        double t = Double.MAX_VALUE;
        for (int i = 0; i < flashCount; i++) {
            if (flashes[i][3] < t) {
                t = flashes[i][3];
                best = (int) flashes[i][2];
            }
        }
        return best;
    }

    private void draw() {
        GraphicsContext g = sky.getGraphicsContext2D();
        GraphicsContext fx = canvas.getGraphicsContext2D();
        GraphicsContext fg = front.getGraphicsContext2D();
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        g.clearRect(0, 0, w, h);
        fx.clearRect(0, 0, w, h);
        fg.clearRect(0, 0, w, h);
        if (w < 1 || h < 1) {
            return;
        }
        sprites = 0;
        double horizon = h * HORIZON;
        double level = skyFlash.isSelected() ? flashLevel() : 0;
        int flashColor = strongestFlash();

        g.setFill(new LinearGradient(0, horizon - h * 0.3, 0, horizon, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT), new Stop(1, Color.color(0.9, 0.35, 0.6, 0.16))));
        g.fillRect(0, horizon - h * 0.3, w, h * 0.3);

        for (double[] s : starField) {
            double sz = s[2] * unit;
            g.setGlobalAlpha(Math.max(0, 0.3 + 0.5 * Math.sin(time * 1.8 + s[3]) - level * 0.25));
            g.drawImage(starSprite, s[0] * w - sz / 2, s[1] * h - sz / 2, sz, sz);
        }
        sprites += STARS;

        double mx = Math.max(0, w - 340) * 0.78;
        double my = h * 0.13;
        double mr = 28 * unit;
        g.setGlobalAlpha(0.3);
        g.drawImage(glows[SILVER], mx - mr * 4, my - mr * 4, mr * 8, mr * 8);
        g.setGlobalAlpha(1);
        g.setFill(Color.web("#f4f1e6"));
        g.fillOval(mx - mr, my - mr, mr * 2, mr * 2);
        g.setFill(Color.color(0.75, 0.73, 0.68, 0.5));
        g.fillOval(mx - mr * 0.45, my - mr * 0.3, mr * 0.5, mr * 0.42);
        g.fillOval(mx + mr * 0.2, my + mr * 0.2, mr * 0.34, mr * 0.3);
        g.fillOval(mx - mr * 0.1, my - mr * 0.75, mr * 0.24, mr * 0.2);

        if (level > 0 && flashColor >= 0) {
            Color c = Color.web(PALETTE[flashColor]);
            g.setGlobalAlpha(1);
            g.setFill(Color.color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(0.14, level * 0.08)));
            g.fillRect(0, 0, w, horizon);
        }

        if (smokeOn.isSelected()) {
            for (int i = 0; i < smokeCount; i++) {
                double[] s = smoke[i];
                double f = s[4] / s[5];
                double sz = s[6] * (0.6 + f);
                g.setGlobalAlpha(0.035 * Math.sin(Math.PI * Math.min(1, f * 1.4)) * (1 + level));
                g.drawImage(smokeSprite, s[0] - sz / 2, s[1] - sz / 2, sz, sz);
            }
            sprites += smokeCount;
        }

        boolean add = additive.isSelected();
        if (skyFlash.isSelected()) {
            g.setGlobalBlendMode(add ? BlendMode.ADD : BlendMode.SRC_OVER);
            drawFlashes(g, horizon, false);
            g.setGlobalBlendMode(BlendMode.SRC_OVER);
        }
        g.setGlobalAlpha(1);

        fx.setGlobalBlendMode(add ? BlendMode.ADD : BlendMode.SRC_OVER);
        drawParticles(fx, horizon, false);
        fx.setGlobalBlendMode(BlendMode.SRC_OVER);
        fx.setGlobalAlpha(1);

        g = fg;

        if (city.isSelected()) {
            ensureSkyline(w, h);
            if (skyline != null) {
                double top = skyline.getHeight();
                g.setGlobalAlpha(1);
                g.drawImage(skyline, 0, horizon - top);
                if (level > 0) {
                    g.setGlobalAlpha(Math.min(0.2, level * 0.1));
                    g.drawImage(skylineLit, 0, horizon - top);
                }
                for (double[] b : beacons) {
                    double on = Math.sin(time * 3 + b[2]) > 0.6 ? 1 : 0.15;
                    double bs = 14 * unit;
                    g.setGlobalAlpha(on);
                    g.drawImage(sparks[RED], b[0] - bs / 2, b[1] - bs / 2, bs, bs);
                }
            }
        }

        g.setGlobalAlpha(1);
        g.setFill(new LinearGradient(0, horizon, 0, h, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.color(0.03, 0.03, 0.1, 0.94)), new Stop(1, Color.color(0.01, 0.01, 0.04, 0.98))));
        g.fillRect(0, horizon, w, h - horizon);

        if (reflections.isSelected()) {
            if (city.isSelected() && skyline != null) {
                double top = skyline.getHeight();
                g.save();
                g.translate(0, horizon * 2);
                g.scale(1, -1);
                g.setGlobalAlpha(0.22 + Math.min(0.15, level * 0.1));
                g.drawImage(skyline, 0, horizon - top);
                g.restore();
            }
            if (add) {
                g.setGlobalBlendMode(BlendMode.ADD);
            }
            if (skyFlash.isSelected()) {
                drawFlashes(g, horizon, true);
            }
            drawParticles(g, horizon, true);
            g.setGlobalBlendMode(BlendMode.SRC_OVER);
            g.setGlobalAlpha(1);
            for (int i = 0; i < 26; i++) {
                double ly = horizon + (h - horizon) * ((i * 0.618 + time * 0.03) % 1);
                double lx = w * ((i * 0.37 + Math.sin(time * 0.3 + i) * 0.05) % 1);
                g.setFill(Color.color(0.7, 0.75, 1, 0.05 + level * 0.05));
                g.fillRect(lx, ly, w * (0.06 + (i % 5) * 0.025), 1.2 * unit);
            }
        }
        g.setGlobalAlpha(1);
    }

    private void drawFlashes(GraphicsContext g, double horizon, boolean mirror) {
        for (int i = 0; i < flashCount; i++) {
            double[] f = flashes[i];
            double t = f[3];
            double a = Math.pow(Math.max(0, 1 - t / 0.5), 2) * (mirror ? 0.14 : 0.3);
            double sz = (420 + 240 * t) * unit;
            double y = mirror ? horizon + (horizon - f[1]) * 0.3 : f[1];
            g.setGlobalAlpha(a);
            g.drawImage(glows[(int) f[2]], f[0] - sz / 2, y - sz / 2, sz, sz);
            sprites++;
        }
    }

    private void drawParticles(GraphicsContext g, double horizon, boolean mirror) {
        boolean streaks = trails.isSelected() && !mirror;
        double h = canvas.getHeight();
        double ripple = 3 * unit;
        for (int i = 0; i < count; i++) {
            double f = age[i] / life[i];
            int fl = flags[i];
            double a;
            if ((fl & ROCKET) != 0) {
                a = 1;
            } else {
                a = Math.min(1, (1 - f) * 2.2);
                if ((fl & STROBE) != 0 && ((int) (age[i] * 15 + phase[i] * 10) & 1) == 0) {
                    a *= 0.08;
                }
                if ((fl & GLITTER) != 0 && glitter() < 0.35) {
                    a *= 0.25;
                }
            }
            if (a <= 0.01) {
                continue;
            }
            double x = px[i];
            double y = py[i];
            if (mirror) {
                y = horizon + (horizon - y) * 0.3;
                if (y > h + 20 || y < horizon) {
                    continue;
                }
                x += Math.sin(y * 0.06 + time * 2.4) * ripple;
                a *= 0.3;
            }
            Image img = sparks[f < 0.55 ? index(color1[i]) : index(color2[i])];
            double s = size[i] * unit * ((fl & ROCKET) != 0 ? 1.2 : 0.7 + 0.55 * (1 - f));
            if (streaks && (fl & ROCKET) == 0) {
                double dx = vx[i] * 0.022;
                double dy = vy[i] * 0.022;
                g.setGlobalAlpha(a * 0.22);
                g.drawImage(img, x - dx * 2 - s * 0.3, y - dy * 2 - s * 0.3, s * 0.6, s * 0.6);
                g.setGlobalAlpha(a * 0.45);
                g.drawImage(img, x - dx - s * 0.4, y - dy - s * 0.4, s * 0.8, s * 0.8);
                sprites += 2;
            }
            g.setGlobalAlpha(a);
            g.drawImage(img, x - s / 2, y - s / 2, s, s);
            sprites++;
        }
    }

    private void startLoop() {
        new EyeCandyDemo.Loop() {
            long last;
            long window;
            int frames;
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
                    frames++;
                    sum += ms;
                    worst = Math.max(worst, ms);
                    window += (long) (dt * 1e9);
                    if (window >= 500_000_000L) {
                        fpsValue.setText(String.format(Locale.ROOT, "%.0f", frames * 1e9 / window));
                        frameValue.setText(String.format(Locale.ROOT, "%.1f", sum / frames));
                        worstValue.setText(String.format(Locale.ROOT, "%.1f", worst));
                        updateCounters();
                        if (++reports % 4 == 0) {
                            System.out.printf(Locale.ROOT, "fireworks: %s fps, %s ms avg, %s ms worst, %s particles, %s%n",
                                    fpsValue.getText(), frameValue.getText(), worstValue.getText(),
                                    particlesValue.getText(), spritesValue.getText());
                        }
                        frames = 0;
                        sum = 0;
                        worst = 0;
                        window = 0;
                    }
                    updateGraph();
                }
                unit = Math.max(0.5, canvas.getHeight() / 1000);
                double step = paused ? 0 : Math.min(dt, 0.05) * timeScale.getValue();
                time += step;
                if (step > 0) {
                    autoLaunch();
                    step(step);
                }
                draw();
                animateChrome();
            }
        }.start();
    }

    private void animateChrome() {
        double shift = time * 0.12;
        Stop[] stops = new Stop[RAINBOW.length + 1];
        for (int i = 0; i <= RAINBOW.length; i++) {
            stops[i] = new Stop(i / (double) RAINBOW.length, Color.web(PALETTE[RAINBOW[i % RAINBOW.length]]));
        }
        title.setFill(new LinearGradient(-shift, 0, 1 - shift, 0, true, CycleMethod.REPEAT, stops));
        logo.setRotate(time * 30);
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
        particlesValue.setText(String.format(Locale.ROOT, "%,d", count));
        rocketsValue.setText(String.format(Locale.ROOT, "%d", rockets));
        burstsValue.setText(String.format(Locale.ROOT, "%,d", bursts));
        spritesValue.setText(String.format(Locale.ROOT, "%,d sprites per frame · %d flashes · %d smoke",
                sprites, flashCount, smokeCount));
        double scale = stage.getOutputScaleX();
        sizeValue.setText(String.format(Locale.ROOT, "%.0f×%.0f @%.2f · %.0f×%.0f px", scene.getWidth(),
                scene.getHeight(), scale, scene.getWidth() * scale, scene.getHeight() * scale));
    }
}
