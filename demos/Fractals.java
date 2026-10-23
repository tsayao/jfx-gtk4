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
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
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
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;
import java.util.function.DoubleConsumer;
import java.util.stream.IntStream;

/**
 * A fractal explorer drawn with the Prism renderer (Vulkan or OpenGL ES) in an
 * extended stage over a translucent window backdrop: an escape time fractal
 * computed on all cores and streamed into a writable image, a Menger sponge of
 * thousands of boxes and a gallery of animated geometric fractals.
 */
@SuppressWarnings("deprecation")
public class Fractals implements EyeCandyDemo.Candy {

    // The selection of the candies, in the middle of the header bar
    private Node switcher;

    private static final String CSS = """
        .root {
            -fx-font-family: "Adwaita Sans", "Cantarell", "System";
            -fx-font-size: 13px;
            -fx-base: #161d2e;
            -fx-background: transparent;
            -fx-control-inner-background: #111726;
            -fx-accent: #22d3ee;
            -fx-focus-color: #a3e635;
            -fx-faint-focus-color: #a3e63522;
            -fx-text-fill: #e6f6ff;
        }
        .label, .check-box, .radio-button { -fx-text-fill: #e6f6ff; }
        .muted { -fx-text-fill: #8aa0b8; }
        .mono { -fx-font-family: "Adwaita Mono", "Monospace"; }
        .glass {
            -fx-background-color: rgba(6, 10, 20, 0.5);
            -fx-background-radius: 18;
            -fx-border-color: rgba(255, 255, 255, 0.16) rgba(255, 255, 255, 0.07)
                rgba(255, 255, 255, 0.04) rgba(255, 255, 255, 0.07);
            -fx-border-radius: 18;
            -fx-padding: 16;
        }
        .section { -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #8aa0b8; -fx-padding: 8 0 0 0; }
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
        .stat-key { -fx-font-size: 10px; -fx-text-fill: #8aa0b8; }
        .tile-label { -fx-font-size: 11px; -fx-text-fill: #c2d4e6; }
        .check-box > .box, .radio-button > .radio {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 5;
            -fx-border-color: rgba(255, 255, 255, 0.22);
            -fx-border-radius: 5;
        }
        .radio-button > .radio { -fx-background-radius: 999; -fx-border-radius: 999; }
        .check-box:selected > .box, .radio-button:selected > .radio {
            -fx-background-color: linear-gradient(to bottom right, #a3e635, #22d3ee, #6366f1);
            -fx-border-color: transparent;
        }
        .check-box:selected > .box > .mark, .radio-button:selected > .radio > .dot { -fx-background-color: white; }
        .slider .track { -fx-background-color: rgba(255, 255, 255, 0.14); -fx-background-radius: 4; }
        .slider .thumb {
            -fx-background-color: white;
            -fx-effect: dropshadow(gaussian, #22d3ee, 10, 0.3, 0, 0);
        }
        .button {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 9;
            -fx-text-fill: #e6f6ff;
            -fx-padding: 6 14 6 14;
            -fx-cursor: hand;
        }
        .button:hover { -fx-background-color: rgba(255, 255, 255, 0.15); }
        .button.accent {
            -fx-background-color: linear-gradient(to right, #a3e635, #22d3ee, #6366f1, #d946ef);
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

    private static final int GRAPH = 160;
    private static final double GRAPH_W = 268;
    private static final double GRAPH_H = 56;
    private static final double TILE_W = 118;
    private static final double TILE_H = 84;
    private static final double SPONGE = 240;
    private static final int PALETTE_SIZE = 1024;

    private static final String[] TYPES = {"Mandelbrot", "Julia (animated c)", "Burning ship", "Tricorn"};
    private static final double[][] HOME = {{-0.6, 0, 1.25}, {0, 0, 1.4}, {-0.45, 0.5, 1.3}, {-0.25, 0, 1.6}};
    private static final double[][] DIVE = {
        {-0.743643887037151, 0.131825904205330}, {0.0, 0.0}, {-1.7619, 0.0284}, {-0.3, 0.6}
    };
    private static final String[] PALETTE_NAMES = {"Neon", "Fire", "Ocean", "Aurora", "Candy"};
    private static final String[][] PALETTES = {
        {"#0b0221", "#3a0ca3", "#7209b7", "#f72585", "#ff9e00", "#fffd82", "#4cc9f0", "#3a0ca3"},
        {"#000000", "#3b0000", "#9d0208", "#e85d04", "#ffba08", "#fff3b0", "#e85d04", "#3b0000"},
        {"#00111c", "#002c4b", "#015f8f", "#00a6c8", "#9be7ff", "#ffffff", "#00a6c8", "#002c4b"},
        {"#050816", "#0f3460", "#16a085", "#50fa7b", "#c3f73a", "#bd93f9", "#ff79c6", "#0f3460"},
        {"#ff99c8", "#fcf6bd", "#d0f4de", "#a9def9", "#e4c1f9", "#ff99c8"}
    };

    private record Request(int type, double cx, double cy, double scale, int iterations, double cr, double ci,
                           int width, int height, int palette, double offset, boolean glass) {
        boolean sameGeometry(Request o) {
            return o != null && type == o.type && cx == o.cx && cy == o.cy && scale == o.scale
                    && iterations == o.iterations && cr == o.cr && ci == o.ci && width == o.width && height == o.height;
        }
    }

    private record Frame(int width, int height, int[] pixels, double computeMs, boolean computed) {
    }

    private Stage stage;
    private Scene scene;
    private boolean backdrop;
    private double time;
    private boolean paused;

    private final ImageView fractalView = new ImageView();
    private WritableImage fractalImage;
    private final Pane fractalLayer = new Pane(fractalView);
    private final AtomicReference<Request> pending = new AtomicReference<>();
    private final AtomicReference<Frame> ready = new AtomicReference<>();
    private boolean busy;
    private Thread worker;
    private volatile boolean disposed;
    private int type;
    private double centerX = HOME[0][0];
    private double centerY = HOME[0][1];
    private double scale = HOME[0][2];
    private double juliaAngle = 0.6;
    private double juliaRe = 0.7885 * Math.cos(0.6);
    private double juliaIm = 0.7885 * Math.sin(0.6);
    private double colorOffset;
    private int paletteIndex;
    private double lastComputeMs;
    private int computedFrames;
    private int shownFrames;
    private int effectiveIterations;

    private SubScene sponge;
    private final Group cubes = new Group();
    private final Rotate spongeYaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate spongePitch = new Rotate(-24, Rotate.X_AXIS);
    private final List<Box> boxes = new ArrayList<>();
    private double[] basePositions = new double[0];
    private int spongeLevel = -1;
    private PhongMaterial[] cubeMaterials;

    private final List<DoubleConsumer> tileUpdates = new ArrayList<>();
    private final ToggleGroup typeGroup = new ToggleGroup();
    private final ToggleGroup paletteGroup = new ToggleGroup();
    private final CheckBox dive = new CheckBox("Dive into the fractal");
    private final CheckBox cycle = new CheckBox("Color cycling");
    private final CheckBox animateC = new CheckBox("Animate Julia c");
    private final CheckBox glass = new CheckBox("Glass (translucent escape)");
    private final CheckBox smooth = new CheckBox("Smooth scaling");
    private final CheckBox showSponge = new CheckBox("Menger sponge");
    private final CheckBox breathe = new CheckBox("Breathing (moves every cube)");
    private final CheckBox showGallery = new CheckBox("Fractal gallery");
    private final Slider iterations = new Slider(50, 3000, 300);
    private final Slider resolution = new Slider(0.25, 1.5, 0.6);
    private final Slider cycleSpeed = new Slider(0, 2, 0.35);
    private final Slider level = new Slider(1, 3, 2);
    private final Slider tint = new Slider(0, 0.9, 0.2);

    private final Label fpsValue = new Label("–");
    private final Label frameValue = new Label("–");
    private final Label worstValue = new Label("–");
    private final Label renderValue = new Label("–");
    private final Label iterValue = new Label("–");
    private final Label zoomValue = new Label("–");
    private final Label centerValue = new Label("–");
    private final Label sizeValue = new Label("–");
    private final Label spongeValue = new Label("–");
    private final Polyline frameLine = new Polyline();
    private final Polygon frameArea = new Polygon();
    private final double[] frameTimes = new double[GRAPH];
    private int frameHead;
    private final Region fill = new Region();
    private final Text title = new Text("FRACTALS");
    private final Polygon logo = new Polygon();
    private Node panels;

    @Override
    public void dispose() {
        disposed = true;
        LockSupport.unpark(worker);
    }

    @Override
    public Scene scene(Stage stage, Node switcher, double width, double height) {
        this.switcher = switcher;
        this.stage = stage;
        backdrop = Boolean.getBoolean("glass.gtk4.backdrop.active");
        for (CheckBox c : List.of(cycle, animateC, glass, smooth, showSponge, breathe, showGallery)) {
            c.setSelected(true);
        }

        updateFill();
        tint.valueProperty().addListener(o -> updateFill());
        tint.setDisable(!backdrop);

        fractalLayer.setMinSize(0, 0);
        fractalView.setManaged(false);
        fractalView.fitWidthProperty().bind(fractalLayer.widthProperty());
        fractalView.fitHeightProperty().bind(fractalLayer.heightProperty());
        fractalView.smoothProperty().bind(smooth.selectedProperty());

        Pane spongeLayer = new Pane(sponge());
        spongeLayer.setMinSize(0, 0);
        spongeLayer.setPickOnBounds(false);
        sponge.setMouseTransparent(true);
        sponge.visibleProperty().bind(showSponge.selectedProperty());
        sponge.layoutXProperty().bind(spongeLayer.widthProperty().subtract(336 + 380));
        sponge.layoutYProperty().set(60);
        sponge.scaleXProperty().bind(Bindings.max(0.55, Bindings.min(1, spongeLayer.widthProperty().subtract(900).divide(700))));
        sponge.scaleYProperty().bind(sponge.scaleXProperty());

        BorderPane overlay = new BorderPane();
        overlay.setTop(header());
        overlay.setCenter(panels());
        overlay.setPickOnBounds(false);

        StackPane root = new StackPane(fill, fractalLayer, spongeLayer, overlay);
        root.setMinSize(0, 0);
        scene = new Scene(root, width, height, Color.TRANSPARENT);
        scene.getStylesheets().add(STYLESHEET);
        scene.getPreferences().setColorScheme(ColorScheme.DARK);
        mouse(root);
        keys();

        worker = Thread.ofPlatform().daemon().name("fractal-renderer").start(this::renderLoop);
        startLoop();
        return scene;
    }

    private void updateFill() {
        if (backdrop) {
            double a = tint.getValue();
            fill.setBackground(new Background(new BackgroundFill(Color.color(0.01, 0.02, 0.05, a), null, null)));
        } else {
            fill.setBackground(new Background(
                    new BackgroundFill(new RadialGradient(0, 0, 0.5, 0.45, 0.8, true, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.web("#101a33")), new Stop(1, Color.web("#03050c"))), null, null)));
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
        List<double[]> koch = koch(2);
        for (double[] p : koch) {
            logo.getPoints().addAll(p[0] * 11, p[1] * 11);
        }
        logo.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#a3e635")), new Stop(0.5, Color.web("#22d3ee")), new Stop(1, Color.web("#d946ef"))));
        logo.setEffect(new Glow(0.7));
        Label brand = new Label("Fractals");
        brand.getStyleClass().add("brand");
        Label sub = new Label("Prism renderer showcase");
        sub.getStyleClass().add("muted");
        HBox left = new HBox(10, logo, brand, sub);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 18));

        Circle dot = new Circle(4, Color.web("#a3e635"));
        dot.setEffect(new DropShadow(8, Color.web("#a3e635")));
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
        HBox glassPill = new HBox(new Label(backdrop ? "Backdrop blur" : "Opaque"));
        glassPill.getStyleClass().add("pill");

        Button diveButton = new Button("Dive  D");
        diveButton.getStyleClass().add("accent");
        diveButton.setOnAction(e -> dive.setSelected(!dive.isSelected()));
        Button hide = new Button("Panels  H");
        hide.setOnAction(e -> togglePanels());

        HBox right = new HBox(10, live, pipeline, glassPill, diveButton, hide);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setPadding(new Insets(0, 10, 0, 0));

        HeaderBar.setDragType(left, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar.setDragType(live, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar.setDragType(pipeline, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar.setDragType(glassPill, HeaderDragType.DRAGGABLE_SUBTREE);
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
            KeyCode code = e.getCode();
            if (code == KeyCode.H) {
                togglePanels();
            } else if (code == KeyCode.F || code == KeyCode.F11) {
                stage.setFullScreen(!stage.isFullScreen());
            } else if (code == KeyCode.SPACE || code == KeyCode.P) {
                paused = !paused;
            } else if (code == KeyCode.D) {
                dive.setSelected(!dive.isSelected());
            } else if (code == KeyCode.R) {
                home();
            } else if (code == KeyCode.C) {
                paletteGroup.selectToggle(paletteGroup.getToggles().get((paletteIndex + 1) % PALETTES.length));
            } else if (code.isDigitKey() && code.getCode() >= KeyCode.DIGIT1.getCode()
                    && code.getCode() <= KeyCode.DIGIT4.getCode()) {
                typeGroup.selectToggle(typeGroup.getToggles().get(code.getCode() - KeyCode.DIGIT1.getCode()));
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

    private double unitPerPixel() {
        return 2 * scale / Math.max(1, fractalLayer.getHeight());
    }

    private double[] toComplex(double sceneX, double sceneY) {
        Point2D p = fractalLayer.sceneToLocal(sceneX, sceneY);
        double k = unitPerPixel();
        return new double[] {
            centerX + (p.getX() - fractalLayer.getWidth() / 2) * k,
            centerY - (p.getY() - fractalLayer.getHeight() / 2) * k
        };
    }

    private void zoomAt(double sceneX, double sceneY, double factor) {
        double[] c = toComplex(sceneX, sceneY);
        Point2D p = fractalLayer.sceneToLocal(sceneX, sceneY);
        scale = Math.max(1e-13, Math.min(4, scale * factor));
        double k = unitPerPixel();
        centerX = c[0] - (p.getX() - fractalLayer.getWidth() / 2) * k;
        centerY = c[1] + (p.getY() - fractalLayer.getHeight() / 2) * k;
    }

    private void mouse(Node root) {
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
            dive.setSelected(false);
            double k = unitPerPixel();
            centerX -= (e.getSceneX() - drag[0]) * k;
            centerY += (e.getSceneY() - drag[1]) * k;
            drag[0] = e.getSceneX();
            drag[1] = e.getSceneY();
        });
        root.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (onChrome(e.getTarget()) || !e.isStillSincePress()) {
                return;
            }
            if (e.isShiftDown() && type == 0) {
                double[] c = toComplex(e.getSceneX(), e.getSceneY());
                juliaRe = c[0];
                juliaIm = c[1];
                animateC.setSelected(false);
                typeGroup.selectToggle(typeGroup.getToggles().get(1));
            } else if (e.getClickCount() == 2) {
                dive.setSelected(false);
                zoomAt(e.getSceneX(), e.getSceneY(), e.getButton() == MouseButton.SECONDARY ? 2.5 : 0.4);
            }
        });
        root.addEventHandler(ScrollEvent.SCROLL, e -> {
            if (!onChrome(e.getTarget())) {
                dive.setSelected(false);
                zoomAt(e.getSceneX(), e.getSceneY(), Math.exp(-e.getDeltaY() * 0.004));
            }
        });
    }

    private void home() {
        dive.setSelected(false);
        centerX = HOME[type][0];
        centerY = HOME[type][1];
        scale = HOME[type][2];
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

        Node gallery = gallery();
        gallery.visibleProperty().bind(showGallery.selectedProperty());
        AnchorPane.setLeftAnchor(gallery, 18.0);
        AnchorPane.setRightAnchor(gallery, 336.0);
        AnchorPane.setBottomAnchor(gallery, 18.0);

        area.getChildren().addAll(hud, controls, gallery);
        panels = area;
        return area;
    }

    private Node hud() {
        title.setFont(Font.font("Adwaita Sans", FontWeight.BLACK, 54));
        Reflection reflection = new Reflection(0, 0.45, 0.4, 0);
        reflection.setInput(new DropShadow(BlurType.GAUSSIAN, Color.web("#22d3eecc"), 28, 0.25, 0, 0));
        title.setEffect(reflection);
        Label subtitle = new Label("Prism · " + pipelineName() + " · " + System.getProperty("glass.platform", "gtk")
                + " glass · scroll to zoom, drag to pan");
        subtitle.getStyleClass().add("muted");

        GridPane stats = new GridPane(22, 10);
        stats.add(stat("FPS", fpsValue), 0, 0);
        stats.add(stat("Frame ms", frameValue), 1, 0);
        stats.add(stat("Worst ms", worstValue), 2, 0);
        stats.add(stat("Render ms", renderValue), 0, 1);
        stats.add(stat("Iterations", iterValue), 1, 1);
        stats.add(stat("Zoom", zoomValue), 2, 1);

        Pane graph = new Pane();
        graph.setPrefSize(GRAPH_W, GRAPH_H);
        graph.setMinSize(GRAPH_W, GRAPH_H);
        graph.setMaxSize(GRAPH_W, GRAPH_H);
        double target = GRAPH_H - GRAPH_H * 16.7 / 40;
        Line limit = new Line(0, target, GRAPH_W, target);
        limit.setStroke(Color.color(1, 1, 1, 0.3));
        limit.getStrokeDashArray().addAll(4.0, 4.0);
        frameArea.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#22d3ee66")), new Stop(1, Color.web("#a3e63500"))));
        frameLine.setStroke(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#a3e635")), new Stop(0.5, Color.web("#22d3ee")), new Stop(1, Color.web("#d946ef"))));
        frameLine.setStrokeWidth(1.6);
        frameLine.setStrokeLineJoin(StrokeLineJoin.ROUND);
        Text limitLabel = new Text(GRAPH_W - 52, target - 4, "16.7 ms");
        limitLabel.setFill(Color.color(1, 1, 1, 0.45));
        limitLabel.setFont(Font.font("Adwaita Mono", 10));
        graph.getChildren().addAll(frameArea, frameLine, limit, limitLabel);

        for (Label l : List.of(centerValue, sizeValue, spongeValue)) {
            l.getStyleClass().addAll("muted", "mono");
        }

        VBox card = new VBox(12, stats, graph, new VBox(3, centerValue, sizeValue, spongeValue));
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
        VBox types = new VBox(6);
        for (int i = 0; i < TYPES.length; i++) {
            RadioButton r = new RadioButton(TYPES[i] + "  " + (i + 1));
            r.setToggleGroup(typeGroup);
            r.setUserData(i);
            types.getChildren().add(r);
        }
        typeGroup.selectedToggleProperty().addListener((o, was, now) -> {
            if (now != null) {
                type = (int) now.getUserData();
                if (type == 1 && !animateC.isSelected()) {
                    dive.setSelected(false);
                    centerX = HOME[1][0];
                    centerY = HOME[1][1];
                    scale = HOME[1][2];
                } else {
                    home();
                }
            }
        });
        typeGroup.selectToggle(typeGroup.getToggles().get(0));

        FlowPane paletteBox = new FlowPane(12, 6);
        for (int i = 0; i < PALETTES.length; i++) {
            RadioButton r = new RadioButton(PALETTE_NAMES[i]);
            r.setToggleGroup(paletteGroup);
            r.setUserData(i);
            Stop[] stops = new Stop[PALETTES[i].length];
            for (int s = 0; s < stops.length; s++) {
                stops[s] = new Stop(s / (stops.length - 1.0), Color.web(PALETTES[i][s]));
            }
            Rectangle swatch = new Rectangle(26, 10, new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE, stops));
            swatch.setArcWidth(6);
            swatch.setArcHeight(6);
            r.setGraphic(swatch);
            r.setContentDisplay(ContentDisplay.RIGHT);
            paletteBox.getChildren().add(r);
        }
        paletteGroup.selectedToggleProperty().addListener((o, was, now) -> {
            if (now != null) {
                paletteIndex = (int) now.getUserData();
            }
        });
        paletteGroup.selectToggle(paletteGroup.getToggles().get(0));

        level.setMajorTickUnit(1);
        level.setMinorTickCount(0);
        level.setSnapToTicks(true);
        level.setBlockIncrement(1);

        Button reset = new Button("Reset view");
        reset.setOnAction(e -> home());
        Button diveButton = new Button("Dive");
        diveButton.getStyleClass().add("accent");
        diveButton.setOnAction(e -> dive.setSelected(!dive.isSelected()));
        Button full = new Button("Full screen");
        full.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));
        HBox buttons = new HBox(8, diveButton, reset, full);

        Label hint = new Label("Scroll to zoom · drag to pan · double click zoom\n"
                + "Shift click the Mandelbrot set for its Julia set\n"
                + "1–4 fractal · C palette · D dive · R reset\nH panels · F full screen · Space pause");
        hint.getStyleClass().add("muted");

        VBox box = new VBox(8,
                section("ESCAPE TIME FRACTAL"), types, dive, animateC,
                sliderRow("Max iterations", iterations, "%,.0f"),
                sliderRow("Resolution", resolution, "%.2f×"),
                section("COLOR"), paletteBox, cycle, sliderRow("Cycle speed", cycleSpeed, "%.2f"), glass, smooth,
                section("MENGER SPONGE"), showSponge, sliderRow("Level", level, "%.0f"), breathe,
                section("WINDOW"),
                sliderRow("Backdrop tint", tint, "%.2f"), showGallery,
                spacer(4), buttons, spacer(4), hint);
        box.setPadding(new Insets(0, 12, 0, 0));
        return box;
    }

    private static int[] palette(String[] colors) {
        int[] result = new int[PALETTE_SIZE];
        int segments = colors.length - 1;
        for (int i = 0; i < PALETTE_SIZE; i++) {
            double t = i / (double) PALETTE_SIZE * segments;
            int s = (int) t;
            Color c = Color.web(colors[s]).interpolate(Color.web(colors[Math.min(s + 1, segments)]), t - s);
            result[i] = argb(1, c.getRed(), c.getGreen(), c.getBlue());
        }
        return result;
    }

    private static int argb(double a, double r, double g, double b) {
        return (int) (Math.max(0, Math.min(1, a)) * 255) << 24
                | (int) (Math.max(0, Math.min(1, r)) * 255) << 16
                | (int) (Math.max(0, Math.min(1, g)) * 255) << 8
                | (int) (Math.max(0, Math.min(1, b)) * 255);
    }

    private void renderLoop() {
        Request last = null;
        float[] value = new float[0];
        float[] alpha = new float[0];
        int[] pixels = new int[0];
        while (!disposed) {
            Request r = pending.getAndSet(null);
            if (r == null) {
                LockSupport.park();
                continue;
            }
            int n = r.width() * r.height();
            if (value.length != n) {
                value = new float[n];
                alpha = new float[n];
                pixels = new int[n];
                last = null;
            }
            long start = System.nanoTime();
            boolean computed = !r.sameGeometry(last);
            if (computed) {
                compute(r, value, alpha);
                last = r;
            }
            colorize(r, value, alpha, pixels);
            ready.set(new Frame(r.width(), r.height(), pixels, (System.nanoTime() - start) / 1e6, computed));
        }
    }

    private static void compute(Request r, float[] value, float[] alpha) {
        int w = r.width();
        int h = r.height();
        double k = 2 * r.scale() / h;
        int max = r.iterations();
        double log2 = Math.log(2);
        IntStream.range(0, h).parallel().forEach(row -> {
            double im = r.cy() - (row - h / 2.0) * k;
            for (int col = 0; col < w; col++) {
                double re = r.cx() + (col - w / 2.0) * k;
                double x;
                double y;
                double cr;
                double ci;
                if (r.type() == 1) {
                    x = re;
                    y = im;
                    cr = r.cr();
                    ci = r.ci();
                } else {
                    x = 0;
                    y = 0;
                    cr = re;
                    ci = r.type() == 2 ? -im : im;
                }
                int n = 0;
                double x2 = x * x;
                double y2 = y * y;
                if (r.type() == 0) {
                    double q = (re - 0.25) * (re - 0.25) + im * im;
                    if (q * (q + (re - 0.25)) <= 0.25 * im * im || (re + 1) * (re + 1) + im * im <= 0.0625) {
                        n = max;
                    }
                }
                while (n < max && x2 + y2 <= 256) {
                    double xt = x2 - y2 + cr;
                    switch (r.type()) {
                        case 2 -> y = Math.abs(2 * x * y) + ci;
                        case 3 -> y = -2 * x * y + ci;
                        default -> y = 2 * x * y + ci;
                    }
                    x = xt;
                    x2 = x * x;
                    y2 = y * y;
                    n++;
                }
                int i = row * w + col;
                if (n >= max) {
                    value[i] = -1;
                    alpha[i] = 0;
                } else {
                    double mu = n + 1 - Math.log(Math.log(x2 + y2) * 0.5) / log2;
                    mu = Math.max(0, mu);
                    value[i] = (float) (Math.sqrt(mu) * 0.16);
                    double a = Math.max(0, Math.min(1, (mu - 1.5) / 9));
                    alpha[i] = (float) (a * Math.sqrt(a));
                }
            }
        });
    }

    private static void colorize(Request r, float[] value, float[] alpha, int[] pixels) {
        int w = r.width();
        int h = r.height();
        double offset = r.offset();
        boolean glass = r.glass();
        int[] colors = PALETTE_CACHE[r.palette()];
        IntStream.range(0, h).parallel().forEach(row -> {
            int base = row * w;
            for (int col = 0; col < w; col++) {
                int i = base + col;
                float v = value[i];
                if (v < 0) {
                    pixels[i] = glass ? 0x00000000 : 0xff020308;
                    continue;
                }
                int c = colors[(int) ((v + offset) * PALETTE_SIZE) & (PALETTE_SIZE - 1)];
                if (!glass) {
                    pixels[i] = c;
                    continue;
                }
                float a = alpha[i];
                int ai = (int) (a * 255);
                int red = (int) (((c >> 16) & 0xff) * a);
                int green = (int) (((c >> 8) & 0xff) * a);
                int blue = (int) ((c & 0xff) * a);
                pixels[i] = ai << 24 | red << 16 | green << 8 | blue;
            }
        });
    }

    private static final int[][] PALETTE_CACHE = new int[PALETTES.length][];

    static {
        for (int i = 0; i < PALETTES.length; i++) {
            PALETTE_CACHE[i] = palette(PALETTES[i]);
        }
    }

    private void updateFractal(double step) {
        if (step > 0) {
            if (cycle.isSelected()) {
                colorOffset = (colorOffset + step * cycleSpeed.getValue() * 0.1) % 1;
            }
            if (type == 1 && animateC.isSelected()) {
                juliaAngle += step * 0.22;
                double radius = 0.7885 + 0.02 * Math.sin(time * 0.7);
                juliaRe = radius * Math.cos(juliaAngle);
                juliaIm = radius * Math.sin(juliaAngle);
            }
            if (dive.isSelected()) {
                double[] target = DIVE[type];
                double pull = Math.min(1, step * 1.8);
                centerX += (target[0] - centerX) * pull;
                centerY += (target[1] - centerY) * pull;
                scale *= Math.exp(-step * 0.45);
                if (scale < 2e-12) {
                    home();
                    dive.setSelected(true);
                }
            }
        }
        effectiveIterations = (int) iterations.getValue();
        if (scale < 1) {
            effectiveIterations = (int) Math.max(effectiveIterations, 150 + 110 * Math.log10(1.5 / scale));
        }
        effectiveIterations = Math.min(effectiveIterations, 8000);

        Frame f = ready.getAndSet(null);
        if (f != null) {
            if (fractalImage == null || fractalImage.getWidth() != f.width() || fractalImage.getHeight() != f.height()) {
                fractalImage = new WritableImage(f.width(), f.height());
                fractalView.setImage(fractalImage);
            }
            fractalImage.getPixelWriter().setPixels(0, 0, f.width(), f.height(), PixelFormat.getIntArgbPreInstance(),
                    f.pixels(), 0, f.width());
            shownFrames++;
            if (f.computed()) {
                lastComputeMs = f.computeMs();
                computedFrames++;
            }
            busy = false;
        }
        double lw = fractalLayer.getWidth();
        double lh = fractalLayer.getHeight();
        if (!busy && lw > 0 && lh > 0) {
            int w = Math.max(32, (int) (lw * resolution.getValue()));
            int h = Math.max(32, (int) (lh * resolution.getValue()));
            busy = true;
            pending.set(new Request(type, centerX, centerY, scale, effectiveIterations, juliaRe, juliaIm, w, h,
                    paletteIndex, colorOffset, glass.isSelected()));
            LockSupport.unpark(worker);
        }
    }

    private SubScene sponge() {
        cubeMaterials = new PhongMaterial[12];
        for (int i = 0; i < cubeMaterials.length; i++) {
            Color c = Color.hsb(170 + i * 16, 0.75, 0.95);
            PhongMaterial m = new PhongMaterial(c);
            m.setSpecularColor(Color.color(1, 1, 1, 0.8));
            m.setSpecularPower(24);
            cubeMaterials[i] = m;
        }
        cubes.getTransforms().addAll(spongePitch, spongeYaw);
        PointLight warm = new PointLight(Color.web("#ffd0f5"));
        warm.setTranslateX(-500);
        warm.setTranslateY(-400);
        warm.setTranslateZ(-600);
        PointLight cool = new PointLight(Color.web("#7dd3fc"));
        cool.setTranslateX(600);
        cool.setTranslateY(300);
        cool.setTranslateZ(-300);
        AmbientLight ambient = new AmbientLight(Color.rgb(40, 40, 70));
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(1);
        camera.setFarClip(5000);
        camera.setFieldOfView(32);
        camera.setTranslateZ(-860);
        Group world = new Group(cubes, warm, cool, ambient, camera);
        sponge = new SubScene(world, 380, 380, true, SceneAntialiasing.BALANCED);
        sponge.setFill(Color.TRANSPARENT);
        sponge.setCamera(camera);
        buildSponge((int) level.getValue());
        level.valueProperty().addListener(o -> buildSponge((int) Math.round(level.getValue())));
        return sponge;
    }

    private void buildSponge(int lv) {
        if (lv == spongeLevel) {
            return;
        }
        spongeLevel = lv;
        List<double[]> positions = new ArrayList<>();
        menger(0, 0, 0, SPONGE, lv, positions);
        double s = SPONGE / Math.pow(3, lv);
        boxes.clear();
        basePositions = new double[positions.size() * 3];
        List<Node> nodes = new ArrayList<>(positions.size());
        for (int i = 0; i < positions.size(); i++) {
            double[] p = positions.get(i);
            Box b = new Box(s * 0.96, s * 0.96, s * 0.96);
            b.setTranslateX(p[0]);
            b.setTranslateY(p[1]);
            b.setTranslateZ(p[2]);
            double d = (p[0] + p[1] * 0.7 - p[2] * 0.4) / SPONGE + 0.5;
            b.setMaterial(cubeMaterials[(int) Math.max(0, Math.min(cubeMaterials.length - 1, d * cubeMaterials.length))]);
            basePositions[i * 3] = p[0];
            basePositions[i * 3 + 1] = p[1];
            basePositions[i * 3 + 2] = p[2];
            boxes.add(b);
            nodes.add(b);
        }
        cubes.getChildren().setAll(nodes);
    }

    private static void menger(double x, double y, double z, double size, int lv, List<double[]> out) {
        if (lv == 0) {
            out.add(new double[] {x, y, z});
            return;
        }
        double s = size / 3;
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                for (int k = -1; k <= 1; k++) {
                    int zeros = (i == 0 ? 1 : 0) + (j == 0 ? 1 : 0) + (k == 0 ? 1 : 0);
                    if (zeros < 2) {
                        menger(x + i * s, y + j * s, z + k * s, s, lv - 1, out);
                    }
                }
            }
        }
    }

    private void updateSponge(double step) {
        spongeYaw.setAngle(spongeYaw.getAngle() + step * 18);
        spongePitch.setAngle(-24 + Math.sin(time * 0.4) * 10);
        if (breathe.isSelected() && showSponge.isSelected()) {
            double k = 1 + 0.12 * (1 + Math.sin(time * 1.6));
            for (int i = 0; i < boxes.size(); i++) {
                Box b = boxes.get(i);
                b.setTranslateX(basePositions[i * 3] * k);
                b.setTranslateY(basePositions[i * 3 + 1] * k);
                b.setTranslateZ(basePositions[i * 3 + 2] * k);
            }
        }
    }

    private Node gallery() {
        HBox tiles = new HBox(10);
        tiles.setPadding(new Insets(2));

        Polygon snowflake = new Polygon();
        snowflake.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#22d3ee")), new Stop(1, Color.web("#6366f1"))));
        snowflake.setStroke(Color.WHITE);
        snowflake.setStrokeWidth(0.8);
        snowflake.setEffect(new DropShadow(BlurType.GAUSSIAN, Color.web("#22d3ee"), 12, 0.3, 0, 0));
        int[] kochLevel = {-1};
        tiles.getChildren().add(tile("Koch snowflake", snowflake, t -> {
            int lv = (int) (t * 0.6) % 6;
            if (lv != kochLevel[0]) {
                kochLevel[0] = lv;
                snowflake.getPoints().setAll(fit(koch(lv), 10));
            }
            snowflake.setRotate(t * 12);
        }));

        Polyline dragon = new Polyline();
        dragon.setStroke(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#f97316")), new Stop(0.5, Color.web("#f43f5e")), new Stop(1, Color.web("#d946ef"))));
        dragon.setStrokeWidth(1);
        dragon.setStrokeLineJoin(StrokeLineJoin.ROUND);
        int[] dragonLevel = {-1};
        tiles.getChildren().add(tile("Dragon curve", dragon, t -> {
            int lv = 1 + (int) (t * 0.7) % 12;
            if (lv != dragonLevel[0]) {
                dragonLevel[0] = lv;
                dragon.getPoints().setAll(fit(dragon(lv), 8));
            }
        }));

        Polyline hilbert = new Polyline();
        hilbert.setStroke(Color.web("#a3e635"));
        hilbert.setStrokeWidth(1.2);
        hilbert.getStrokeDashArray().addAll(10.0, 6.0);
        hilbert.setEffect(new Glow(0.6));
        int[] hilbertOrder = {-1};
        tiles.getChildren().add(tile("Hilbert curve", hilbert, t -> {
            int order = 1 + (int) (t * 0.5) % 5;
            if (order != hilbertOrder[0]) {
                hilbertOrder[0] = order;
                hilbert.getPoints().setAll(fit(hilbert(order), 8));
            }
            hilbert.setStrokeDashOffset(-t * 40);
            hilbert.setStroke(Color.hsb((t * 40) % 360, 0.7, 1));
        }));

        Canvas chaos = canvas();
        Random random = new Random(1);
        double[] chaosState = {TILE_W / 2, TILE_H / 2, -1};
        Color[] corners = {Color.web("#f43f5e"), Color.web("#22d3ee"), Color.web("#facc15")};
        tiles.getChildren().add(tile("Sierpinski (chaos game)", chaos, t -> {
            GraphicsContext g = chaos.getGraphicsContext2D();
            int cyclePass = (int) (t / 6);
            if (cyclePass != chaosState[2]) {
                chaosState[2] = cyclePass;
                g.clearRect(0, 0, TILE_W, TILE_H);
            }
            double[][] v = {{TILE_W / 2, 6}, {8, TILE_H - 6}, {TILE_W - 8, TILE_H - 6}};
            for (int i = 0; i < 500; i++) {
                int c = random.nextInt(3);
                chaosState[0] = (chaosState[0] + v[c][0]) / 2;
                chaosState[1] = (chaosState[1] + v[c][1]) / 2;
                g.setFill(corners[c]);
                g.fillRect(chaosState[0], chaosState[1], 0.8, 0.8);
            }
        }));

        Canvas fern = canvas();
        double[] fernState = {0, 0, -1};
        tiles.getChildren().add(tile("Barnsley fern", fern, t -> {
            GraphicsContext g = fern.getGraphicsContext2D();
            int pass = (int) (t / 7);
            if (pass != fernState[2]) {
                fernState[2] = pass;
                g.clearRect(0, 0, TILE_W, TILE_H);
            }
            g.setFill(Color.hsb(90 + (pass * 47) % 120, 0.75, 0.95));
            for (int i = 0; i < 700; i++) {
                double x = fernState[0];
                double y = fernState[1];
                double r = random.nextDouble();
                if (r < 0.01) {
                    fernState[0] = 0;
                    fernState[1] = 0.16 * y;
                } else if (r < 0.86) {
                    fernState[0] = 0.85 * x + 0.04 * y;
                    fernState[1] = -0.04 * x + 0.85 * y + 1.6;
                } else if (r < 0.93) {
                    fernState[0] = 0.2 * x - 0.26 * y;
                    fernState[1] = 0.23 * x + 0.22 * y + 1.6;
                } else {
                    fernState[0] = -0.15 * x + 0.28 * y;
                    fernState[1] = 0.26 * x + 0.24 * y + 0.44;
                }
                g.fillRect(TILE_W / 2 + fernState[0] * 7.5, TILE_H - 3 - fernState[1] * 7.6, 0.8, 0.8);
            }
        }));

        Canvas pythagoras = canvas();
        tiles.getChildren().add(tile("Pythagoras tree", pythagoras, t -> {
            GraphicsContext g = pythagoras.getGraphicsContext2D();
            g.clearRect(0, 0, TILE_W, TILE_H);
            double a = Math.toRadians(45 + 20 * Math.sin(t * 0.8));
            double s = 14;
            pythagoras(g, TILE_W / 2 - s / 2, TILE_H - 4, TILE_W / 2 + s / 2, TILE_H - 4, a, 9, t);
        }));

        Canvas tree = canvas();
        tiles.getChildren().add(tile("Binary tree · wind", tree, t -> {
            GraphicsContext g = tree.getGraphicsContext2D();
            g.clearRect(0, 0, TILE_W, TILE_H);
            g.setLineCap(StrokeLineCap.ROUND);
            branch(g, TILE_W / 2, TILE_H - 2, 22, Math.PI / 2, 10, t);
        }));

        Canvas carpet = canvas();
        tiles.getChildren().add(tile("Sierpinski carpet", carpet, t -> {
            GraphicsContext g = carpet.getGraphicsContext2D();
            g.clearRect(0, 0, TILE_W, TILE_H);
            double s = TILE_H - 8;
            double x = (TILE_W - s) / 2;
            g.setFill(Color.hsb((t * 30) % 360, 0.65, 0.95));
            g.fillRect(x, 4, s, s);
            carpet(g, x, 4, s, 4, t);
        }));

        ScrollPane scroll = new ScrollPane(tiles);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setFitToHeight(true);
        VBox box = new VBox(6, section("GEOMETRIC FRACTALS · LIVE"), scroll);
        box.getStyleClass().add("glass");
        box.setPadding(new Insets(10, 14, 12, 14));
        box.setMaxHeight(Region.USE_PREF_SIZE);
        return box;
    }

    private static Canvas canvas() {
        return new Canvas(TILE_W, TILE_H);
    }

    private Node tile(String name, Node art, DoubleConsumer update) {
        Rectangle bg = new Rectangle(TILE_W, TILE_H, Color.color(0, 0, 0, 0.35));
        Pane frame = new Pane(bg, art);
        frame.setMinSize(TILE_W, TILE_H);
        frame.setPrefSize(TILE_W, TILE_H);
        frame.setMaxSize(TILE_W, TILE_H);
        Rectangle clip = new Rectangle(TILE_W, TILE_H);
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        frame.setClip(clip);
        tileUpdates.add(update);
        Label label = new Label(name);
        label.getStyleClass().add("tile-label");
        return new VBox(6, frame, label);
    }

    private static List<double[]> koch(int lv) {
        List<double[]> pts = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            double a = -Math.PI / 2 + i * Math.PI * 2 / 3;
            pts.add(new double[] {Math.cos(a), Math.sin(a)});
        }
        for (int l = 0; l < lv; l++) {
            List<double[]> next = new ArrayList<>();
            for (int i = 0; i < pts.size(); i++) {
                double[] a = pts.get(i);
                double[] b = pts.get((i + 1) % pts.size());
                double dx = (b[0] - a[0]) / 3;
                double dy = (b[1] - a[1]) / 3;
                double[] p1 = {a[0] + dx, a[1] + dy};
                double[] p2 = {a[0] + 2 * dx, a[1] + 2 * dy};
                double[] peak = {p1[0] + dx * 0.5 + dy * Math.sqrt(3) / 2, p1[1] + dy * 0.5 - dx * Math.sqrt(3) / 2};
                next.add(a);
                next.add(p1);
                next.add(peak);
                next.add(p2);
            }
            pts = next;
        }
        return pts;
    }

    private static List<double[]> dragon(int lv) {
        int n = 1 << lv;
        List<double[]> pts = new ArrayList<>(n + 1);
        double x = 0;
        double y = 0;
        int dir = 0;
        pts.add(new double[] {x, y});
        for (int i = 1; i <= n; i++) {
            x += dir == 0 ? 1 : dir == 2 ? -1 : 0;
            y += dir == 1 ? 1 : dir == 3 ? -1 : 0;
            pts.add(new double[] {x, y});
            boolean left = (((i & -i) << 1) & i) != 0;
            dir = (dir + (left ? 1 : 3)) % 4;
        }
        return pts;
    }

    private static List<double[]> hilbert(int order) {
        int n = 1 << order;
        List<double[]> pts = new ArrayList<>(n * n);
        for (int d = 0; d < n * n; d++) {
            int x = 0;
            int y = 0;
            int t = d;
            for (int s = 1; s < n; s *= 2) {
                int rx = 1 & (t / 2);
                int ry = 1 & (t ^ rx);
                if (ry == 0) {
                    if (rx == 1) {
                        x = s - 1 - x;
                        y = s - 1 - y;
                    }
                    int tmp = x;
                    x = y;
                    y = tmp;
                }
                x += s * rx;
                y += s * ry;
                t /= 4;
            }
            pts.add(new double[] {x, y});
        }
        return pts;
    }

    private static List<Double> fit(List<double[]> pts, double margin) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        for (double[] p : pts) {
            minX = Math.min(minX, p[0]);
            minY = Math.min(minY, p[1]);
            maxX = Math.max(maxX, p[0]);
            maxY = Math.max(maxY, p[1]);
        }
        double k = Math.min((TILE_W - margin * 2) / Math.max(1e-9, maxX - minX),
                (TILE_H - margin * 2) / Math.max(1e-9, maxY - minY));
        double ox = (TILE_W - (maxX - minX) * k) / 2;
        double oy = (TILE_H - (maxY - minY) * k) / 2;
        List<Double> out = new ArrayList<>(pts.size() * 2);
        for (double[] p : pts) {
            out.add(ox + (p[0] - minX) * k);
            out.add(oy + (p[1] - minY) * k);
        }
        return out;
    }

    private static void pythagoras(GraphicsContext g, double x1, double y1, double x2, double y2, double a, int depth,
                                   double t) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double x3 = x2 + dy;
        double y3 = y2 - dx;
        double x4 = x1 + dy;
        double y4 = y1 - dx;
        g.setFill(Color.hsb((depth * 28 + t * 25) % 360, 0.7, 0.4 + depth * 0.06));
        g.fillPolygon(new double[] {x1, x2, x3, x4}, new double[] {y1, y2, y3, y4}, 4);
        if (depth == 0) {
            return;
        }
        double c = Math.cos(a);
        double s = Math.sin(a);
        double x5 = x4 + (dx * c + dy * s) * c;
        double y5 = y4 + (-dx * s + dy * c) * c;
        pythagoras(g, x4, y4, x5, y5, a, depth - 1, t);
        pythagoras(g, x5, y5, x3, y3, a, depth - 1, t);
    }

    private static void branch(GraphicsContext g, double x, double y, double len, double angle, int depth, double t) {
        double sway = Math.sin(t * 1.3 + depth * 0.5) * 0.06 * (11 - depth);
        double a = angle + sway * 0.2;
        double x2 = x + Math.cos(a) * len;
        double y2 = y - Math.sin(a) * len;
        g.setStroke(Color.hsb(20 + (10 - depth) * 12, 0.7, 0.55 + (10 - depth) * 0.045));
        g.setLineWidth(Math.max(0.5, depth * 0.45));
        g.strokeLine(x, y, x2, y2);
        if (depth > 0) {
            double spread = 0.42 + 0.08 * Math.sin(t * 0.7);
            branch(g, x2, y2, len * 0.74, a + spread, depth - 1, t);
            branch(g, x2, y2, len * 0.74, a - spread, depth - 1, t);
        }
    }

    private static void carpet(GraphicsContext g, double x, double y, double s, int depth, double t) {
        double third = s / 3;
        g.setFill(Color.color(0.02, 0.03, 0.08, 0.85));
        g.fillRect(x + third, y + third, third, third);
        if (depth <= 1) {
            return;
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i != 1 || j != 1) {
                    carpet(g, x + i * third, y + j * third, third, depth - 1, t);
                }
            }
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
                            System.out.printf(Locale.ROOT, "fractals: %s fps, %s ms avg, %s ms worst, %s ms render, %s, %s%n",
                                    fpsValue.getText(), frameValue.getText(), worstValue.getText(),
                                    renderValue.getText(), zoomValue.getText(), spongeValue.getText());
                        }
                        frames = 0;
                        sum = 0;
                        worst = 0;
                        window = 0;
                    }
                    updateGraph();
                }
                double step = paused ? 0 : Math.min(dt, 0.1);
                time += step;
                updateFractal(step);
                updateSponge(step);
                logo.setRotate(time * 20);
                title.setFill(new LinearGradient(-time * 0.1, 0, 1 - time * 0.1, 0, true, CycleMethod.REFLECT,
                        new Stop(0, Color.web("#a3e635")), new Stop(0.5, Color.web("#22d3ee")),
                        new Stop(1, Color.web("#d946ef"))));
                if (showGallery.isSelected() && panels.isVisible()) {
                    for (DoubleConsumer u : tileUpdates) {
                        u.accept(time);
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
        renderValue.setText(String.format(Locale.ROOT, "%.1f", lastComputeMs));
        iterValue.setText(String.format(Locale.ROOT, "%,d", effectiveIterations));
        double zoom = HOME[type][2] / scale;
        zoomValue.setText(zoom < 1e4 ? String.format(Locale.ROOT, "%.1f×", zoom)
                : String.format(Locale.ROOT, "%.1e×", zoom).replace("e+0", "e").replace("e+", "e"));
        centerValue.setText(String.format(Locale.ROOT, "%s · c = %+.12f %+.12fi", TYPES[type].split(" ")[0],
                centerX, centerY));
        String image = fractalImage == null ? "–" : String.format(Locale.ROOT, "%.0f×%.0f",
                fractalImage.getWidth(), fractalImage.getHeight());
        double outScale = stage.getOutputScaleX();
        sizeValue.setText(String.format(Locale.ROOT, "%.0f×%.0f @%.2f · fractal %s px · %d frames/s, %d computed",
                scene.getWidth(), scene.getHeight(), outScale, image, shownFrames * 2, computedFrames * 2));
        computedFrames = 0;
        shownFrames = 0;
        spongeValue.setText(String.format(Locale.ROOT, "Menger level %d · %,d boxes · %,d triangles",
                spongeLevel, boxes.size(), boxes.size() * 12L));
    }
}
