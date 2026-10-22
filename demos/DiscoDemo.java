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
import javafx.scene.transform.Translate;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * A disco night drawn with the Prism renderer (Vulkan or OpenGL ES) in an
 * extended stage over a translucent window backdrop: a spinning mirror ball of
 * hundreds of boxes, sweeping spot lights, a light-up dance floor and a crew of
 * articulated dancers, all on the beat, under canvas light beams and confetti.
 */
@SuppressWarnings("deprecation")
public class DiscoDemo extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    private static final String CSS = """
        .root {
            -fx-font-family: "Adwaita Sans", "Cantarell", "System";
            -fx-font-size: 13px;
            -fx-base: #241433;
            -fx-background: transparent;
            -fx-control-inner-background: #1b0f28;
            -fx-accent: #ff2d95;
            -fx-focus-color: #ffd23f;
            -fx-faint-focus-color: #ffd23f22;
            -fx-text-fill: #fff0fa;
        }
        .label, .check-box { -fx-text-fill: #fff0fa; }
        .muted { -fx-text-fill: #b59ac8; }
        .mono { -fx-font-family: "Adwaita Mono", "Monospace"; }
        .glass {
            -fx-background-color: rgba(18, 4, 28, 0.5);
            -fx-background-radius: 18;
            -fx-border-color: rgba(255, 255, 255, 0.16) rgba(255, 255, 255, 0.07)
                rgba(255, 255, 255, 0.04) rgba(255, 255, 255, 0.07);
            -fx-border-radius: 18;
            -fx-padding: 16;
        }
        .section { -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #b59ac8; -fx-padding: 8 0 0 0; }
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
        .stat-key { -fx-font-size: 10px; -fx-text-fill: #b59ac8; }
        .check-box > .box {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 5;
            -fx-border-color: rgba(255, 255, 255, 0.22);
            -fx-border-radius: 5;
        }
        .check-box:selected > .box {
            -fx-background-color: linear-gradient(to bottom right, #ffd23f, #ff2d95, #9d4dff);
            -fx-border-color: transparent;
        }
        .check-box:selected > .box > .mark { -fx-background-color: white; }
        .slider .track { -fx-background-color: rgba(255, 255, 255, 0.14); -fx-background-radius: 4; }
        .slider .thumb {
            -fx-background-color: white;
            -fx-effect: dropshadow(gaussian, #ff2d95, 10, 0.3, 0, 0);
        }
        .button {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 9;
            -fx-text-fill: #fff0fa;
            -fx-padding: 6 14 6 14;
            -fx-cursor: hand;
        }
        .button:hover { -fx-background-color: rgba(255, 255, 255, 0.15); }
        .button.active { -fx-border-color: #ffd23f; -fx-background-color: rgba(255, 210, 63, 0.18); }
        .button.accent {
            -fx-background-color: linear-gradient(to right, #ffd23f, #ff2d95, #9d4dff, #3df5ff);
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

    private static final String[] NEON = {
        "#ff2d95", "#ffd23f", "#3df5ff", "#9d4dff", "#3dff8b", "#ff7a2d", "#ff3df5", "#4d7dff"
    };
    private static final String[] MOVES = {"Auto", "Hustle", "Point", "Robot", "Twist", "Roller", "YMCA"};
    private static final int AUTO = 0;
    private static final int HUSTLE = 1;
    private static final int POINT = 2;
    private static final int ROBOT = 3;
    private static final int TWIST = 4;
    private static final int ROLLER = 5;
    private static final int YMCA = 6;

    private static final int PX = 0;
    private static final int PY = 1;
    private static final int PRZ = 2;
    private static final int PRY = 3;
    private static final int SRY = 4;
    private static final int SRZ = 5;
    private static final int SRX = 6;
    private static final int HRY = 7;
    private static final int HRZ = 8;
    private static final int HRX = 9;
    private static final int LSO = 10;
    private static final int LSF = 11;
    private static final int LEL = 12;
    private static final int RSO = 13;
    private static final int RSF = 14;
    private static final int REL = 15;
    private static final int LHF = 16;
    private static final int LKN = 17;
    private static final int RHF = 18;
    private static final int RKN = 19;
    private static final int LHO = 20;
    private static final int RHO = 21;
    private static final int SPIN = 22;
    private static final int POSE = 23;
    private static final int[][] MIRROR_SWAP = {{LSO, RSO}, {LSF, RSF}, {LEL, REL}, {LHF, RHF}, {LKN, RKN}, {LHO, RHO}};
    private static final int[] MIRROR_NEGATE = {PX, PRZ, PRY, SRY, SRZ, HRY, HRZ};

    private static final int GRAPH = 160;
    private static final double GRAPH_W = 268;
    private static final double GRAPH_H = 56;
    private static final int BARS = 28;
    private static final int FLOOR_COLS = 12;
    private static final int FLOOR_ROWS = 6;
    private static final double FLOOR_TILE = 42;
    private static final int SPARKLES = 280;
    private static final int MAX_CONFETTI = 900;
    private static final int MAX_NOTES = 40;
    private static final double BALL_Y = -400;
    private static final double BALL_R = 62;

    private Stage stage;
    private Scene scene;
    private boolean backdrop;
    private double time;
    private double beatTime;
    private boolean paused;
    private int lastBeat = -1;
    private double dropStart = -100;
    private int move = AUTO;
    private int currentMove = HUSTLE;
    private final Random random = new Random(1977);

    private SubScene club;
    private final Group world = new Group();
    private final Group ball = new Group();
    private final Rotate ballSpin = new Rotate(0, Rotate.Y_AXIS);
    private final Group crew = new Group();
    private final List<Dancer> dancers = new ArrayList<>();
    private final PhongMaterial[] floorMaterials = new PhongMaterial[FLOOR_COLS * FLOOR_ROWS];
    private Image[][] glowMaps;
    private Image darkMap;
    private final SpotLight[] spots = {new SpotLight(Color.web(NEON[0])), new SpotLight(Color.web(NEON[2]))};
    private final Point3D[] spotPositions = {new Point3D(-460, -420, -80), new Point3D(460, -420, -80)};
    private final Point3D[] spotTargets = new Point3D[2];
    private final int[] spotColors = {0, 2};
    private final PointLight front = new PointLight(Color.web("#ffe9f5"));
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Rotate yaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate pitch = new Rotate(-8, Rotate.X_AXIS);
    private int shapes;
    private long triangles;

    private final Canvas back = new Canvas();
    private final Canvas fore = new Canvas();
    private final Canvas equalizer = new Canvas(GRAPH_W, 60);
    private Image[] glows;
    private final double[][] sparkles = new double[SPARKLES][4];
    private final double[][] confetti = new double[MAX_CONFETTI][9];
    private int confettiCount;
    private final double[][] notes = new double[MAX_NOTES][6];
    private int noteCount;
    private final double[] levels = new double[BARS];
    private final double[] peaks = new double[BARS];
    private int sprites;

    private final Slider bpm = new Slider(80, 160, 118);
    private final Slider crewSize = new Slider(1, 5, 3);
    private final Slider tint = new Slider(0, 0.9, 0.25);
    private final CheckBox mirror = new CheckBox("Mirror alternate dancers");
    private final CheckBox wave = new CheckBox("Wave (offset in time)");
    private final CheckBox orbit = new CheckBox("Camera sway");
    private final CheckBox showBall = new CheckBox("Mirror ball (boxes)");
    private final CheckBox beams = new CheckBox("Light beams");
    private final CheckBox showSparkles = new CheckBox("Wall reflections");
    private final CheckBox floorLights = new CheckBox("Floor lights (self illumination)");
    private final CheckBox spotsOn = new CheckBox("Spot lights");
    private final CheckBox confettiOn = new CheckBox("Confetti");
    private final CheckBox notesOn = new CheckBox("Music notes");
    private final CheckBox strobe = new CheckBox("Strobe on drop");

    private final Label fpsValue = new Label("–");
    private final Label frameValue = new Label("–");
    private final Label worstValue = new Label("–");
    private final Label bpmValue = new Label("–");
    private final Label beatValue = new Label("–");
    private final Label moveValue = new Label("–");
    private final Label sceneValue = new Label("–");
    private final Label sizeValue = new Label("–");
    private final Polyline frameLine = new Polyline();
    private final Polygon frameArea = new Polygon();
    private final double[] frameTimes = new double[GRAPH];
    private int frameHead;
    private final Region fill = new Region();
    private final Text title = new Text("DISCO");
    private final DropShadow titleGlow = new DropShadow(BlurType.GAUSSIAN, Color.web("#ff2d95"), 30, 0.3, 0, 0);
    private final Circle logo = new Circle(10);
    private final Circle beatDot = new Circle(4, Color.web("#ffd23f"));
    private final List<Button> moveButtons = new ArrayList<>();
    private Node panels;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        backdrop = Boolean.getBoolean("glass.gtk4.backdrop.active");
        stage.initStyle(StageStyle.EXTENDED);
        for (CheckBox c : List.of(mirror, orbit, showBall, beams, showSparkles, floorLights, spotsOn, confettiOn,
                notesOn, strobe)) {
            c.setSelected(true);
        }
        updateFill();
        tint.valueProperty().addListener(o -> updateFill());
        tint.setDisable(!backdrop);
        seedSprites();

        Pane backLayer = canvasLayer(back);
        Pane clubLayer = new Pane(club());
        club.setManaged(false);
        club.widthProperty().bind(clubLayer.widthProperty());
        club.heightProperty().bind(clubLayer.heightProperty());
        clubLayer.setMinSize(0, 0);
        Pane foreLayer = canvasLayer(fore);
        foreLayer.setMouseTransparent(true);

        BorderPane overlay = new BorderPane();
        overlay.setTop(header());
        overlay.setCenter(panels());
        overlay.setPickOnBounds(false);

        StackPane root = new StackPane(fill, backLayer, clubLayer, foreLayer, overlay);
        root.setMinSize(0, 0);
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width = Math.max(1200, Math.min(1760, screen.getWidth() * 0.88));
        double height = Math.max(760, Math.min(1100, screen.getHeight() * 0.9));
        scene = new Scene(root, width, height, Color.TRANSPARENT);
        scene.getStylesheets().add(STYLESHEET);
        scene.getPreferences().setColorScheme(ColorScheme.DARK);
        cameraControls(root);
        keys();

        stage.setScene(scene);
        stage.setTitle("Disco — Prism renderer showcase");
        stage.setMinWidth(1100);
        stage.setMinHeight(700);
        stage.show();

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

    private static Pane canvasLayer(Canvas canvas) {
        Pane layer = new Pane(canvas);
        canvas.setManaged(false);
        canvas.widthProperty().bind(layer.widthProperty());
        canvas.heightProperty().bind(layer.heightProperty());
        layer.setMinSize(0, 0);
        return layer;
    }

    private void updateFill() {
        if (backdrop) {
            double a = tint.getValue();
            fill.setBackground(new Background(new BackgroundFill(new LinearGradient(0, 0, 0, 1, true,
                    CycleMethod.NO_CYCLE, new Stop(0, Color.color(0.05, 0.0, 0.08, a)),
                    new Stop(1, Color.color(0.1, 0.02, 0.12, Math.min(1, a + 0.25)))), null, null)));
        } else {
            fill.setBackground(new Background(new BackgroundFill(new RadialGradient(0, 0, 0.5, 0.3, 0.9, true,
                    CycleMethod.NO_CYCLE, new Stop(0, Color.web("#2a0838")), new Stop(0.6, Color.web("#12031d")),
                    new Stop(1, Color.web("#06010b"))), null, null)));
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
        logo.setFill(new RadialGradient(0, 0, 0.35, 0.35, 0.8, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(0.4, Color.web("#c9c3d9")), new Stop(1, Color.web("#3a3050"))));
        logo.setStroke(Color.web("#ffffffaa"));
        logo.setStrokeWidth(0.6);
        logo.getStrokeDashArray().addAll(2.0, 2.0);
        logo.setEffect(new Glow(0.8));
        Label brand = new Label("Disco");
        brand.getStyleClass().add("brand");
        Label sub = new Label("Prism renderer showcase");
        sub.getStyleClass().add("muted");
        HBox left = new HBox(10, logo, brand, sub);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 18));

        beatDot.setEffect(new DropShadow(8, Color.web("#ffd23f")));
        Label tempo = new Label();
        tempo.getStyleClass().add("mono");
        tempo.textProperty().bind(Bindings.format(Locale.ROOT, "%.0f bpm", bpm.valueProperty()));
        HBox beat = new HBox(beatDot, tempo);
        beat.getStyleClass().add("pill");
        Label fps = new Label();
        fps.getStyleClass().add("mono");
        fps.textProperty().bind(fpsValue.textProperty().concat(" fps"));
        HBox live = new HBox(fps);
        live.getStyleClass().add("pill");
        HBox pipeline = new HBox(new Label(pipelineName() + " · GTK 4"));
        pipeline.getStyleClass().add("pill");
        HBox glass = new HBox(new Label(backdrop ? "Backdrop blur" : "Opaque"));
        glass.getStyleClass().add("pill");

        Button drop = new Button("Drop!  D");
        drop.getStyleClass().add("accent");
        drop.setOnAction(e -> drop());
        Button hide = new Button("Panels  H");
        hide.setOnAction(e -> togglePanels());

        HBox right = new HBox(10, beat, live, pipeline, glass, drop, hide);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setPadding(new Insets(0, 10, 0, 0));

        HeaderBar.setDragType(left, HeaderDragType.DRAGGABLE_SUBTREE);
        for (Node n : List.of(beat, live, pipeline, glass)) {
            HeaderBar.setDragType(n, HeaderDragType.DRAGGABLE_SUBTREE);
        }
        HeaderBar.setDragType(right, HeaderDragType.DRAGGABLE);

        HeaderBar header = new HeaderBar(left, null, right);
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
            } else if (code == KeyCode.D || code == KeyCode.ENTER) {
                drop();
            } else if (code == KeyCode.UP) {
                bpm.setValue(bpm.getValue() + 2);
            } else if (code == KeyCode.DOWN) {
                bpm.setValue(bpm.getValue() - 2);
            } else if (code.isDigitKey() && code.getCode() >= KeyCode.DIGIT1.getCode()
                    && code.getCode() < KeyCode.DIGIT1.getCode() + MOVES.length) {
                selectMove(code.getCode() - KeyCode.DIGIT1.getCode());
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

    private void cameraControls(Node root) {
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
            orbit.setSelected(false);
            yaw.setAngle(yaw.getAngle() + (e.getSceneX() - drag[0]) * 0.3);
            pitch.setAngle(Math.max(-40, Math.min(15, pitch.getAngle() - (e.getSceneY() - drag[1]) * 0.2)));
            drag[0] = e.getSceneX();
            drag[1] = e.getSceneY();
        });
        root.addEventHandler(ScrollEvent.SCROLL, e -> {
            if (!onChrome(e.getTarget())) {
                camera.setTranslateZ(Math.max(-2200, Math.min(-500, camera.getTranslateZ() + e.getDeltaY() * 2)));
            }
        });
    }

    private void selectMove(int m) {
        move = m;
        for (int i = 0; i < moveButtons.size(); i++) {
            moveButtons.get(i).getStyleClass().remove("active");
            if (i == m) {
                moveButtons.get(i).getStyleClass().add("active");
            }
        }
    }

    private void drop() {
        dropStart = beatTime;
        if (confettiOn.isSelected()) {
            double w = fore.getWidth();
            for (int i = 0; i < 380; i++) {
                addConfetti(random.nextDouble() * w, -20 - random.nextDouble() * 200, true);
            }
        }
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

        Node moves = movesBar();
        AnchorPane.setLeftAnchor(moves, 18.0);
        AnchorPane.setRightAnchor(moves, 336.0);
        AnchorPane.setBottomAnchor(moves, 18.0);

        area.getChildren().addAll(hud, controls, moves);
        panels = area;
        return area;
    }

    private Node hud() {
        title.setFont(Font.font("Adwaita Sans", FontWeight.BLACK, 58));
        Reflection reflection = new Reflection(0, 0.45, 0.4, 0);
        reflection.setInput(titleGlow);
        title.setEffect(reflection);
        Label subtitle = new Label("Prism · " + pipelineName() + " · " + System.getProperty("glass.platform", "gtk")
                + " glass · drag to look around");
        subtitle.getStyleClass().add("muted");

        GridPane stats = new GridPane(22, 10);
        stats.add(stat("FPS", fpsValue), 0, 0);
        stats.add(stat("Frame ms", frameValue), 1, 0);
        stats.add(stat("Worst ms", worstValue), 2, 0);
        stats.add(stat("BPM", bpmValue), 0, 1);
        stats.add(stat("Bar · beat", beatValue), 1, 1);
        stats.add(stat("Move", moveValue), 2, 1);

        Pane graph = new Pane();
        graph.setPrefSize(GRAPH_W, GRAPH_H);
        graph.setMinSize(GRAPH_W, GRAPH_H);
        graph.setMaxSize(GRAPH_W, GRAPH_H);
        double target = GRAPH_H - GRAPH_H * 16.7 / 40;
        Line limit = new Line(0, target, GRAPH_W, target);
        limit.setStroke(Color.color(1, 1, 1, 0.3));
        limit.getStrokeDashArray().addAll(4.0, 4.0);
        frameArea.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ff2d9566")), new Stop(1, Color.web("#9d4dff00"))));
        frameLine.setStroke(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#ffd23f")), new Stop(0.5, Color.web("#ff2d95")), new Stop(1, Color.web("#3df5ff"))));
        frameLine.setStrokeWidth(1.6);
        frameLine.setStrokeLineJoin(StrokeLineJoin.ROUND);
        Text limitLabel = new Text(GRAPH_W - 52, target - 4, "16.7 ms");
        limitLabel.setFill(Color.color(1, 1, 1, 0.45));
        limitLabel.setFont(Font.font("Adwaita Mono", 10));
        graph.getChildren().addAll(frameArea, frameLine, limit, limitLabel);

        sceneValue.getStyleClass().addAll("muted", "mono");
        sizeValue.getStyleClass().addAll("muted", "mono");

        VBox card = new VBox(12, stats, equalizer, graph, new VBox(3, sceneValue, sizeValue));
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
        crewSize.setMajorTickUnit(1);
        crewSize.setMinorTickCount(0);
        crewSize.setSnapToTicks(true);
        crewSize.valueProperty().addListener(o -> buildCrew());
        mirror.selectedProperty().addListener(o -> buildCrew());

        Button drop = new Button("Drop!");
        drop.getStyleClass().add("accent");
        drop.setOnAction(e -> drop());
        Button full = new Button("Full screen");
        full.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));
        HBox buttons = new HBox(8, drop, full);

        Label hint = new Label("Drag to look around · scroll to zoom\n1–7 moves · D drop · ↑↓ tempo\n"
                + "H panels · F full screen · Space pause");
        hint.getStyleClass().add("muted");

        VBox box = new VBox(8,
                section("MUSIC"), sliderRow("Tempo", bpm, "%.0f bpm"),
                section("CREW"), sliderRow("Dancers", crewSize, "%.0f"), mirror, wave,
                section("CLUB"), showBall, spotsOn, floorLights, beams, showSparkles, confettiOn, notesOn, strobe,
                orbit,
                section("WINDOW"), sliderRow("Backdrop tint", tint, "%.2f"),
                spacer(4), buttons, spacer(4), hint);
        box.setPadding(new Insets(0, 12, 0, 0));
        return box;
    }

    private Node movesBar() {
        FlowPane buttons = new FlowPane(8, 8);
        for (int i = 0; i < MOVES.length; i++) {
            int m = i;
            Button b = new Button(MOVES[i] + "  " + (i + 1));
            b.setOnAction(e -> selectMove(m));
            moveButtons.add(b);
            buttons.getChildren().add(b);
        }
        Button drop = new Button("Drop!");
        drop.getStyleClass().add("accent");
        drop.setOnAction(e -> drop());
        buttons.getChildren().add(drop);
        selectMove(AUTO);
        VBox box = new VBox(8, section("DANCE MOVES"), buttons);
        box.getStyleClass().add("glass");
        box.setPadding(new Insets(10, 14, 14, 14));
        box.setMaxHeight(Region.USE_PREF_SIZE);
        return box;
    }

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

    private static Image solid(Color color) {
        WritableImage image = new WritableImage(2, 2);
        for (int y = 0; y < 2; y++) {
            for (int x = 0; x < 2; x++) {
                image.getPixelWriter().setColor(x, y, color);
            }
        }
        return image;
    }

    private void seedSprites() {
        glows = new Image[NEON.length + 1];
        for (int i = 0; i < NEON.length; i++) {
            glows[i] = sprite(Color.web(NEON[i]), 64, 3);
        }
        glows[NEON.length] = sprite(Color.WHITE, 64, 4);
        glowMaps = new Image[NEON.length][4];
        for (int i = 0; i < NEON.length; i++) {
            for (int l = 0; l < 4; l++) {
                glowMaps[i][l] = solid(Color.web(NEON[i]).deriveColor(0, 1, 0.35 + l * 0.22, 1));
            }
        }
        darkMap = solid(Color.web("#0c0614"));
        for (double[] s : sparkles) {
            s[0] = random.nextDouble() * Math.PI * 2;
            s[1] = random.nextDouble() * 2 - 1;
            s[2] = 3 + random.nextDouble() * 7;
            s[3] = random.nextDouble() < 0.7 ? NEON.length : random.nextInt(NEON.length);
        }
    }

    private SubScene club() {
        shapes = 0;
        triangles = 0;
        Group floor = new Group();
        for (int r = 0; r < FLOOR_ROWS; r++) {
            for (int c = 0; c < FLOOR_COLS; c++) {
                Box tile = new Box(FLOOR_TILE - 3, 6, FLOOR_TILE - 3);
                tile.setTranslateX((c - (FLOOR_COLS - 1) / 2.0) * FLOOR_TILE);
                tile.setTranslateY(3);
                tile.setTranslateZ((r - 1.5) * FLOOR_TILE);
                PhongMaterial m = new PhongMaterial(Color.web("#140a1e"));
                m.setSpecularColor(Color.color(1, 1, 1, 0.6));
                m.setSpecularPower(30);
                m.setSelfIlluminationMap(darkMap);
                floorMaterials[r * FLOOR_COLS + c] = m;
                tile.setMaterial(m);
                floor.getChildren().add(tile);
                count(tile);
            }
        }

        PhongMaterial[] mirrors = new PhongMaterial[5];
        for (int i = 0; i < mirrors.length; i++) {
            double v = 0.45 + i * 0.12;
            PhongMaterial m = new PhongMaterial(Color.color(v, v, v * 1.05 > 1 ? 1 : v * 1.05));
            m.setSpecularColor(Color.WHITE);
            m.setSpecularPower(48 + i * 20);
            mirrors[i] = m;
        }
        Sphere core = new Sphere(BALL_R - 3, 24);
        core.setMaterial(new PhongMaterial(Color.web("#1a1424")));
        ball.getChildren().add(core);
        count(core);
        int bands = 16;
        for (int b = 0; b < bands; b++) {
            double lat = -90 + 180.0 * (b + 0.5) / bands;
            int n = Math.max(6, (int) Math.round(Math.cos(Math.toRadians(lat)) * 34));
            double edge = BALL_R * Math.PI / bands;
            for (int k = 0; k < n; k++) {
                double lon = 360.0 * (k + (b % 2) * 0.5) / n;
                Box mirrorTile = new Box(Math.max(4, 2 * Math.PI * BALL_R * Math.cos(Math.toRadians(lat)) / n - 1.2),
                        edge - 1.2, 2);
                mirrorTile.getTransforms().addAll(new Rotate(lon, Rotate.Y_AXIS), new Rotate(lat, Rotate.X_AXIS),
                        new Translate(0, 0, -BALL_R));
                mirrorTile.setMaterial(mirrors[random.nextInt(mirrors.length)]);
                ball.getChildren().add(mirrorTile);
                count(mirrorTile);
            }
        }
        Cylinder string = new Cylinder(1.2, 260, 6);
        string.setTranslateY(BALL_Y - BALL_R - 130);
        string.setTranslateZ(60);
        string.setMaterial(new PhongMaterial(Color.web("#8a80a0")));
        count(string);
        ball.getTransforms().add(ballSpin);
        ball.setTranslateY(BALL_Y);
        ball.setTranslateZ(60);
        ball.visibleProperty().bind(showBall.selectedProperty());

        crew.setTranslateZ(20);
        buildCrew();

        for (int i = 0; i < spots.length; i++) {
            spots[i].setTranslateX(spotPositions[i].getX());
            spots[i].setTranslateY(spotPositions[i].getY());
            spots[i].setTranslateZ(spotPositions[i].getZ());
            spots[i].setInnerAngle(8);
            spots[i].setOuterAngle(22);
            spots[i].setFalloff(1);
            spots[i].lightOnProperty().bind(spotsOn.selectedProperty());
        }
        front.setTranslateY(-260);
        front.setTranslateZ(-520);
        AmbientLight ambient = new AmbientLight(Color.rgb(46, 30, 60));

        camera.setNearClip(5);
        camera.setFarClip(8000);
        camera.setFieldOfView(38);
        camera.setTranslateZ(-940);
        Group rig = new Group(camera);
        rig.setTranslateY(-190);
        rig.setTranslateZ(40);
        rig.getTransforms().addAll(yaw, pitch);

        world.getChildren().addAll(floor, crew, ball, string, spots[0], spots[1], front, ambient, rig);
        club = new SubScene(world, 800, 600, true, SceneAntialiasing.BALANCED);
        club.setFill(Color.TRANSPARENT);
        club.setCamera(camera);
        return club;
    }

    private void count(Shape3D shape) {
        shapes++;
        if (shape instanceof Box) {
            triangles += 12;
        } else if (shape instanceof Sphere s) {
            triangles += (long) s.getDivisions() * s.getDivisions();
        } else if (shape instanceof Cylinder c) {
            triangles += 4L * c.getDivisions();
        }
    }

    private void buildCrew() {
        int n = (int) Math.round(crewSize.getValue());
        for (Dancer d : dancers) {
            d.uncount(this);
        }
        dancers.clear();
        List<Node> nodes = new ArrayList<>();
        String[][] outfits = {
            {"#f5f5f5", "#ff2d95"}, {"#ff2d95", "#ffd23f"}, {"#3df5ff", "#9d4dff"}, {"#ffd23f", "#ff7a2d"},
            {"#9d4dff", "#3dff8b"}
        };
        String[] skins = {"#c68a62", "#f1c7a4", "#8d5a3b", "#e0ac7e", "#5c3a24"};
        for (int i = 0; i < n; i++) {
            double x = (i - (n - 1) / 2.0) * 120;
            double z = Math.abs(x) * 0.25;
            String[] outfit = outfits[i % outfits.length];
            Dancer d = new Dancer(Color.web(outfit[0]), Color.web(outfit[1]), Color.web(skins[i % skins.length]),
                    i % 2 == 1, this);
            d.root.setTranslateX(x);
            d.root.setTranslateZ(z);
            d.mirrored = mirror.isSelected() && i % 2 == 1;
            d.offset = i;
            dancers.add(d);
            nodes.add(d.root);
        }
        crew.getChildren().setAll(nodes);
    }

    private static final class Joint {
        final Group group = new Group();
        final Rotate rx = new Rotate(0, Rotate.X_AXIS);
        final Rotate ry = new Rotate(0, Rotate.Y_AXIS);
        final Rotate rz = new Rotate(0, Rotate.Z_AXIS);

        Joint(double x, double y, double z) {
            group.setTranslateX(x);
            group.setTranslateY(y);
            group.setTranslateZ(z);
            group.getTransforms().addAll(ry, rz, rx);
        }
    }

    private static final class Dancer {
        final Group root = new Group();
        final Rotate facing = new Rotate(0, Rotate.Y_AXIS);
        final Joint pelvis = new Joint(0, -95, 0);
        final Joint spine = new Joint(0, -8, 0);
        final Joint neck = new Joint(0, -56, 0);
        final Joint lShoulder = new Joint(-24, -50, 0);
        final Joint rShoulder = new Joint(24, -50, 0);
        final Joint lElbow = new Joint(0, 32, 0);
        final Joint rElbow = new Joint(0, 32, 0);
        final Joint lHip = new Joint(-10, 4, 0);
        final Joint rHip = new Joint(10, 4, 0);
        final Joint lKnee = new Joint(0, 44, 0);
        final Joint rKnee = new Joint(0, 44, 0);
        final double[] current = new double[POSE];
        final double[] target = new double[POSE];
        final List<Shape3D> parts = new ArrayList<>();
        boolean mirrored;
        int offset;

        Dancer(Color suit, Color shirt, Color skin, boolean afro, DiscoDemo owner) {
            PhongMaterial suitMat = shiny(suit, 40);
            PhongMaterial shirtMat = shiny(shirt, 60);
            PhongMaterial skinMat = shiny(skin, 12);
            PhongMaterial hairMat = shiny(Color.web("#1b1210"), 8);
            PhongMaterial shoeMat = shiny(Color.web("#f5f5f5"), 90);
            PhongMaterial glassesMat = shiny(Color.web("#101018"), 120);

            Box hips = box(34, 16, 20, suitMat);
            pelvis.group.getChildren().add(hips);
            Box chest = box(38, 34, 21, shirtMat);
            chest.setTranslateY(-34);
            Box belly = box(32, 22, 19, suitMat);
            belly.setTranslateY(-11);
            Box collar = box(26, 6, 22, suitMat);
            collar.setTranslateY(-50);
            spine.group.getChildren().addAll(belly, chest, collar);

            Sphere head = sphere(13, skinMat);
            head.setTranslateY(-16);
            Sphere hair = sphere(afro ? 18 : 13.5, hairMat);
            hair.setTranslateY(afro ? -24 : -19);
            hair.setTranslateZ(afro ? 3 : 2);
            hair.setScaleY(afro ? 0.85 : 0.75);
            Box glasses = box(22, 5, 3, glassesMat);
            glasses.setTranslateY(-17);
            glasses.setTranslateZ(-12);
            neck.group.getChildren().addAll(sphere(5, skinMat), hair, head, glasses);

            limb(lShoulder, lElbow, 5.5, 32, 4.5, 30, suitMat, skinMat, 6);
            limb(rShoulder, rElbow, 5.5, 32, 4.5, 30, suitMat, skinMat, 6);
            limb(lHip, lKnee, 7.5, 44, 6.5, 42, suitMat, shoeMat, 0);
            limb(rHip, rKnee, 7.5, 44, 6.5, 42, suitMat, shoeMat, 0);

            spine.group.getChildren().addAll(neck.group, lShoulder.group, rShoulder.group);
            pelvis.group.getChildren().addAll(spine.group, lHip.group, rHip.group);
            root.getChildren().add(pelvis.group);
            root.getTransforms().add(facing);
            for (Shape3D s : parts) {
                owner.count(s);
            }
        }

        private void limb(Joint upper, Joint lower, double r1, double l1, double r2, double l2, PhongMaterial mat,
                          PhongMaterial end, double hand) {
            Cylinder a = cylinder(r1, l1, mat);
            a.setTranslateY(l1 / 2);
            Sphere joint = sphere(r1 * 0.95, mat);
            upper.group.getChildren().addAll(sphere(r1, mat), a, lower.group);
            Cylinder b = cylinder(r2, l2, mat);
            b.setTranslateY(l2 / 2);
            lower.group.getChildren().addAll(joint, b);
            if (hand > 0) {
                Sphere h = sphere(hand, end);
                h.setTranslateY(l2 + 3);
                lower.group.getChildren().add(h);
            } else {
                Box shoe = box(13, 7, 24, end);
                shoe.setTranslateY(l2 + 2);
                shoe.setTranslateZ(-5);
                lower.group.getChildren().add(shoe);
            }
        }

        private static PhongMaterial shiny(Color c, double power) {
            PhongMaterial m = new PhongMaterial(c);
            m.setSpecularColor(Color.color(1, 1, 1, 0.9));
            m.setSpecularPower(power);
            return m;
        }

        private Box box(double w, double h, double d, PhongMaterial m) {
            Box b = new Box(w, h, d);
            b.setMaterial(m);
            parts.add(b);
            return b;
        }

        private Sphere sphere(double r, PhongMaterial m) {
            Sphere s = new Sphere(r, 14);
            s.setMaterial(m);
            parts.add(s);
            return s;
        }

        private Cylinder cylinder(double r, double h, PhongMaterial m) {
            Cylinder c = new Cylinder(r, h, 12);
            c.setMaterial(m);
            parts.add(c);
            return c;
        }

        void uncount(DiscoDemo owner) {
            for (Shape3D s : parts) {
                owner.shapes--;
                if (s instanceof Box) {
                    owner.triangles -= 12;
                } else if (s instanceof Sphere sp) {
                    owner.triangles -= (long) sp.getDivisions() * sp.getDivisions();
                } else if (s instanceof Cylinder c) {
                    owner.triangles -= 4L * c.getDivisions();
                }
            }
        }

        void update(double dt, int move, double beat, double spin) {
            pose(move, beat, target);
            if (mirrored) {
                for (int[] pair : MIRROR_SWAP) {
                    double t = target[pair[0]];
                    target[pair[0]] = target[pair[1]];
                    target[pair[1]] = t;
                }
                for (int i : MIRROR_NEGATE) {
                    target[i] = -target[i];
                }
            }
            target[SPIN] = spin;
            double rate = move == ROBOT ? 40 : 15;
            double k = 1 - Math.exp(-dt * rate);
            for (int i = 0; i < POSE; i++) {
                current[i] += (target[i] - current[i]) * (i == SPIN ? 1 : k);
            }
            apply();
        }

        private void apply() {
            double[] p = current;
            double left = 44 * Math.cos(Math.toRadians(p[LHF])) + 44 * Math.cos(Math.toRadians(p[LKN] - p[LHF]));
            double right = 44 * Math.cos(Math.toRadians(p[RHF])) + 44 * Math.cos(Math.toRadians(p[RKN] - p[RHF]));
            double leg = Math.max(left, right) * Math.cos(Math.toRadians(Math.max(Math.abs(p[LHO]), Math.abs(p[RHO]))));
            pelvis.group.setTranslateX(p[PX]);
            pelvis.group.setTranslateY(-95 + (88 - leg) + p[PY]);
            pelvis.rz.setAngle(p[PRZ]);
            pelvis.ry.setAngle(p[PRY]);
            spine.ry.setAngle(p[SRY]);
            spine.rz.setAngle(p[SRZ]);
            spine.rx.setAngle(p[SRX]);
            neck.ry.setAngle(p[HRY]);
            neck.rz.setAngle(p[HRZ]);
            neck.rx.setAngle(p[HRX]);
            lShoulder.rz.setAngle(p[LSO]);
            lShoulder.rx.setAngle(-p[LSF]);
            lElbow.rx.setAngle(-p[LEL]);
            rShoulder.rz.setAngle(-p[RSO]);
            rShoulder.rx.setAngle(-p[RSF]);
            rElbow.rx.setAngle(-p[REL]);
            lHip.rx.setAngle(-p[LHF]);
            lHip.rz.setAngle(p[LHO]);
            lKnee.rx.setAngle(p[LKN]);
            rHip.rx.setAngle(-p[RHF]);
            rHip.rz.setAngle(-p[RHO]);
            rKnee.rx.setAngle(p[RKN]);
            facing.setAngle(p[SPIN]);
        }
    }

    private static void pose(int move, double t, double[] p) {
        Arrays.fill(p, 0);
        int n = (int) Math.floor(t);
        double b = t - n;
        double s = Math.sin(2 * Math.PI * b);
        double c = Math.cos(2 * Math.PI * b);
        double dip = 0.5 + 0.5 * Math.cos(2 * Math.PI * b);
        double sway = Math.sin(Math.PI * t);
        p[LSO] = 8;
        p[RSO] = 8;
        switch (move) {
            case HUSTLE -> {
                p[PX] = 10 * sway;
                p[PRZ] = -6 * sway;
                p[SRZ] = 5 * sway;
                p[SRY] = 14 * s;
                p[LHF] = 16 * dip;
                p[RHF] = 16 * dip;
                p[LKN] = 32 * dip;
                p[RKN] = 32 * dip;
                p[LSO] = 18;
                p[RSO] = 18;
                p[LSF] = 50 * s;
                p[RSF] = -50 * s;
                p[LEL] = 75;
                p[REL] = 75;
                p[HRX] = 12 * dip;
                p[HRZ] = 6 * sway;
            }
            case POINT -> {
                boolean up = n % 2 == 0;
                p[RSO] = up ? 160 : -28;
                p[RSF] = up ? 15 : 30;
                p[LSO] = 48;
                p[LSF] = -12;
                p[LEL] = 115;
                p[PX] = up ? -12 : 12;
                p[PRZ] = up ? 9 : -9;
                p[SRZ] = up ? -9 : 7;
                p[HRZ] = up ? -14 : 8;
                p[HRX] = up ? -12 : 14;
                p[LHF] = up ? 18 : 6;
                p[LKN] = up ? 34 : 12;
                p[RHO] = up ? 0 : 10;
            }
            case ROBOT -> {
                int step = n % 4;
                p[PY] = b < 0.12 ? 4 : 0;
                p[LKN] = 10;
                p[RKN] = 10;
                p[LHF] = 5;
                p[RHF] = 5;
                switch (step) {
                    case 0 -> {
                        p[LSO] = 90;
                        p[RSO] = 90;
                        p[LEL] = 90;
                        p[REL] = 90;
                    }
                    case 1 -> {
                        p[SRY] = 28;
                        p[HRY] = -38;
                        p[RSF] = 90;
                        p[LSO] = 12;
                        p[LEL] = 90;
                    }
                    case 2 -> {
                        p[SRY] = -28;
                        p[HRY] = 38;
                        p[LSF] = 90;
                        p[RSO] = 12;
                        p[REL] = 90;
                    }
                    default -> {
                        p[LSO] = 165;
                        p[RSO] = 20;
                        p[REL] = 90;
                        p[SRZ] = -10;
                        p[HRZ] = 16;
                    }
                }
            }
            case TWIST -> {
                p[LHF] = 22;
                p[RHF] = 22;
                p[LKN] = 42 + 10 * dip;
                p[RKN] = 42 + 10 * dip;
                p[PRY] = -35 * s;
                p[SRY] = 45 * s;
                p[SRX] = 8;
                p[LSO] = 38;
                p[RSO] = 38;
                p[LEL] = 95;
                p[REL] = 95;
                p[LSF] = 20 * s;
                p[RSF] = -20 * s;
                p[HRY] = -18 * s;
                p[PX] = 5 * s;
            }
            case ROLLER -> {
                p[LSO] = 16 + 14 * c;
                p[LSF] = 62 + 16 * s;
                p[RSO] = 16 - 14 * c;
                p[RSF] = 62 - 16 * s;
                p[LEL] = 105;
                p[REL] = 105;
                p[PX] = 16 * sway;
                p[LHO] = sway > 0 ? 14 * sway : 0;
                p[RHO] = sway < 0 ? -14 * sway : 0;
                p[LKN] = 16 * dip;
                p[RKN] = 16 * dip;
                p[LHF] = 8 * dip;
                p[RHF] = 8 * dip;
                p[HRX] = 10 * dip;
                p[SRY] = 12 * sway;
            }
            default -> {
                switch (n % 4) {
                    case 0 -> {
                        p[LSO] = 152;
                        p[RSO] = 152;
                    }
                    case 1 -> {
                        p[LSO] = 125;
                        p[RSO] = 125;
                        p[LEL] = 120;
                        p[REL] = 120;
                    }
                    case 2 -> {
                        p[LSO] = 150;
                        p[RSO] = 45;
                        p[RSF] = 35;
                        p[SRZ] = -14;
                        p[HRZ] = -12;
                    }
                    default -> {
                        p[LSO] = 170;
                        p[RSO] = 170;
                        p[LEL] = 30;
                        p[REL] = 30;
                    }
                }
                p[LHF] = 12 * dip;
                p[RHF] = 12 * dip;
                p[LKN] = 24 * dip;
                p[RKN] = 24 * dip;
                p[HRX] = 10 * dip;
            }
        }
    }

    private int activeMove() {
        if (move != AUTO) {
            return move;
        }
        return 1 + (int) (beatTime / 8) % (MOVES.length - 1);
    }

    private void onBeat(int beat) {
        spotColors[0] = (spotColors[0] + 1 + random.nextInt(NEON.length - 1)) % NEON.length;
        spotColors[1] = (spotColors[0] + 3) % NEON.length;
        spots[0].setColor(Color.web(NEON[spotColors[0]]));
        spots[1].setColor(Color.web(NEON[spotColors[1]]));
        titleGlow.setColor(Color.web(NEON[spotColors[0]]));
        if (notesOn.isSelected() && noteCount < MAX_NOTES) {
            double[] note = notes[noteCount++];
            double w = fore.getWidth();
            note[0] = w * (0.3 + random.nextDouble() * 0.4);
            note[1] = fore.getHeight() * (0.55 + random.nextDouble() * 0.15);
            note[2] = 0;
            note[3] = random.nextInt(3);
            note[4] = random.nextInt(NEON.length);
            note[5] = 24 + random.nextDouble() * 20;
        }
    }

    private void updateFloor() {
        int n = (int) Math.floor(beatTime);
        double b = beatTime - n;
        boolean dropping = beatTime - dropStart < 4;
        int pattern = dropping ? 5 : (int) (beatTime / 16) % 5;
        int level = (int) Math.max(0, Math.min(3, Math.round((1 - b) * 3)));
        for (int r = 0; r < FLOOR_ROWS; r++) {
            for (int c = 0; c < FLOOR_COLS; c++) {
                int color = -1;
                switch (pattern) {
                    case 0 -> color = (r + c + n) % 2 == 0 ? (n + r) % NEON.length : -1;
                    case 1 -> color = (c + r + n) % NEON.length;
                    case 2 -> {
                        double d = Math.hypot(c - (FLOOR_COLS - 1) / 2.0, (r - (FLOOR_ROWS - 1) / 2.0) * 1.4);
                        double ring = (n % 4 + b) * 2;
                        color = Math.abs(d - ring) < 1 ? n % NEON.length : -1;
                    }
                    case 3 -> color = (c % 4 == n % 4 || r % 3 == n % 3) ? (c + n) % NEON.length : -1;
                    case 4 -> color = hash(c, r, n) < 0.45 ? (int) (hash(r, c, n + 7) * NEON.length) : -1;
                    default -> color = hash(c, r, (int) (beatTime * 4)) < 0.5
                            ? (int) (hash(r, c, (int) (beatTime * 4) + 3) * NEON.length) : -1;
                }
                PhongMaterial m = floorMaterials[r * FLOOR_COLS + c];
                Image map = !floorLights.isSelected() || color < 0 ? darkMap
                        : glowMaps[color][pattern == 1 ? 2 : level];
                if (m.getSelfIlluminationMap() != map) {
                    m.setSelfIlluminationMap(map);
                }
            }
        }
    }

    private static double hash(int x, int y, int z) {
        int h = x * 374761393 + y * 668265263 + z * 1274126177;
        h = (h ^ (h >>> 13)) * 1274126177;
        h ^= h >>> 16;
        return (h & 0x7fffffff) / (double) 0x7fffffff;
    }

    private void updateScene(double dt) {
        int n = (int) Math.floor(beatTime);
        double b = beatTime - n;
        if (n != lastBeat) {
            lastBeat = n;
            onBeat(n);
        }
        currentMove = activeMove();
        double sinceDrop = beatTime - dropStart;
        double spin = sinceDrop >= 0 && sinceDrop < 2 ? 360 * ease(sinceDrop / 2) : 0;
        for (Dancer d : dancers) {
            double t = beatTime - (wave.isSelected() ? d.offset * 0.18 : 0);
            d.update(dt, currentMove, t, spin);
        }
        ballSpin.setAngle(ballSpin.getAngle() + dt * 40);
        for (int i = 0; i < spots.length; i++) {
            double a = time * (0.55 + i * 0.12) + i * Math.PI;
            Point3D target = new Point3D(Math.sin(a) * 240, 0, 40 + Math.cos(a * 0.8 + i) * 90);
            spotTargets[i] = target;
            spots[i].setDirection(target.subtract(spotPositions[i]));
        }
        double pulse = Math.pow(1 - b, 3);
        Color frontColor = Color.web(NEON[spotColors[1]]).interpolate(Color.WHITE, 0.6);
        front.setColor(frontColor.deriveColor(0, 1, 0.55 + 0.45 * pulse, 1));
        if (orbit.isSelected()) {
            yaw.setAngle(Math.sin(time * 0.18) * 24);
            pitch.setAngle(-8 + Math.sin(time * 0.11) * 5);
        }
        updateFloor();
        title.setScaleX(1 + 0.07 * pulse);
        title.setScaleY(1 + 0.07 * pulse);
        title.setFill(new LinearGradient(-time * 0.15, 0, 1 - time * 0.15, 0, true, CycleMethod.REPEAT,
                new Stop(0, Color.web("#ffd23f")), new Stop(0.33, Color.web("#ff2d95")),
                new Stop(0.66, Color.web("#3df5ff")), new Stop(1, Color.web("#ffd23f"))));
        beatDot.setOpacity(0.25 + 0.75 * pulse);
        logo.setRotate(time * 90);
    }

    private static double ease(double t) {
        t = Math.max(0, Math.min(1, t));
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    private Point2D project(Point3D p) {
        Point3D s = world.localToScene(p, true);
        if (s == null || Double.isNaN(s.getX())) {
            return null;
        }
        return back.sceneToLocal(s.getX(), s.getY());
    }

    private void drawBack() {
        GraphicsContext g = back.getGraphicsContext2D();
        double w = back.getWidth();
        double h = back.getHeight();
        g.clearRect(0, 0, w, h);
        sprites = 0;
        if (w < 1 || h < 1) {
            return;
        }
        double b = beatTime - Math.floor(beatTime);
        double pulse = Math.pow(1 - b, 3);
        Point2D ballPos = project(new Point3D(0, BALL_Y, 60));
        double bx = ballPos == null ? w / 2 : ballPos.getX();
        double by = ballPos == null ? h * 0.2 : ballPos.getY();

        if (spotsOn.isSelected()) {
            for (int i = 0; i < spots.length; i++) {
                if (spotTargets[i] == null) {
                    continue;
                }
                Point2D from = project(spotPositions[i]);
                Point2D to = project(spotTargets[i]);
                if (from == null || to == null) {
                    continue;
                }
                Color c = Color.web(NEON[spotColors[i]]);
                double dx = to.getX() - from.getX();
                double dy = to.getY() - from.getY();
                double len = Math.hypot(dx, dy);
                double nx = -dy / len;
                double ny = dx / len;
                double spread = 70;
                g.setFill(new LinearGradient(from.getX(), from.getY(), to.getX(), to.getY(), false, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.35)),
                        new Stop(1, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.08))));
                g.fillPolygon(new double[] {from.getX() + nx * 4, to.getX() + nx * spread, to.getX() - nx * spread,
                        from.getX() - nx * 4},
                        new double[] {from.getY() + ny * 4, to.getY() + ny * spread * 0.3, to.getY() - ny * spread * 0.3,
                        from.getY() - ny * 4}, 4);
                g.setGlobalAlpha(0.6);
                g.drawImage(glows[spotColors[i]], to.getX() - 110, to.getY() - 30, 220, 60);
                g.setGlobalAlpha(1);
                sprites += 2;
            }
        }

        if (showBall.isSelected()) {
            g.setGlobalAlpha(0.35 + 0.35 * pulse);
            double gs = 360 + 80 * pulse;
            g.drawImage(glows[NEON.length], bx - gs / 2, by - gs / 2, gs, gs);
            g.setGlobalAlpha(1);
            sprites++;
            if (beams.isSelected()) {
                double spinRad = Math.toRadians(ballSpin.getAngle());
                double length = Math.max(w, h) * 1.2;
                for (int i = 0; i < 14; i++) {
                    double a = spinRad * 0.6 + i * Math.PI * 2 / 14 + Math.sin(time * 0.3 + i) * 0.1;
                    double half = 0.03 + 0.015 * Math.sin(time + i);
                    Color c = i % 3 == 0 ? Color.WHITE : Color.web(NEON[(i + spotColors[0]) % NEON.length]);
                    double ex = bx + Math.cos(a) * length;
                    double ey = by + Math.sin(a) * length;
                    g.setFill(new LinearGradient(bx, by, ex, ey, false, CycleMethod.NO_CYCLE,
                            new Stop(0, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.22 + 0.15 * pulse)),
                            new Stop(0.6, Color.color(c.getRed(), c.getGreen(), c.getBlue(), 0.0))));
                    g.fillPolygon(new double[] {bx, bx + Math.cos(a - half) * length, bx + Math.cos(a + half) * length},
                            new double[] {by, by + Math.sin(a - half) * length, by + Math.sin(a + half) * length}, 3);
                }
            }
            if (showSparkles.isSelected()) {
                double spinRad = Math.toRadians(ballSpin.getAngle());
                for (double[] s : sparkles) {
                    double a = s[0] + spinRad;
                    double facing = Math.cos(a);
                    if (facing < -0.1) {
                        continue;
                    }
                    double sx = w / 2 + Math.sin(a) * w * 0.62;
                    double sy = by + (s[1] * 0.5 + 0.5) * (h - by) * 1.05 - 40;
                    double flicker = 0.55 + 0.45 * Math.sin(time * 9 + s[0] * 13);
                    double size = s[2] * (0.8 + 0.6 * pulse);
                    g.setGlobalAlpha(Math.min(1, (facing + 0.1) * flicker));
                    g.drawImage(glows[(int) s[3]], sx - size, sy - size, size * 2, size * 2);
                    sprites++;
                }
                g.setGlobalAlpha(1);
            }
        }
    }

    private void addConfetti(double x, double y, boolean burst) {
        if (confettiCount >= MAX_CONFETTI) {
            return;
        }
        double[] c = confetti[confettiCount++];
        c[0] = x;
        c[1] = y;
        c[2] = (random.nextDouble() - 0.5) * (burst ? 160 : 40);
        c[3] = 40 + random.nextDouble() * 80;
        c[4] = random.nextDouble() * Math.PI * 2;
        c[5] = (random.nextDouble() - 0.5) * 8;
        c[6] = 5 + random.nextDouble() * 6;
        c[7] = random.nextInt(NEON.length);
        c[8] = random.nextDouble() * Math.PI * 2;
    }

    private void drawFore(double dt) {
        GraphicsContext g = fore.getGraphicsContext2D();
        double w = fore.getWidth();
        double h = fore.getHeight();
        g.clearRect(0, 0, w, h);
        if (w < 1 || h < 1) {
            return;
        }
        if (confettiOn.isSelected() && dt > 0 && random.nextDouble() < dt * 14) {
            addConfetti(random.nextDouble() * w, -10, false);
        }
        double[] xs = new double[4];
        double[] ys = new double[4];
        for (int i = 0; i < confettiCount; i++) {
            double[] c = confetti[i];
            c[2] += Math.sin(time * 2 + c[8]) * 30 * dt;
            c[2] *= 1 - dt * 0.8;
            c[3] = Math.min(c[3] + 120 * dt, 90 + c[6] * 4);
            c[0] += c[2] * dt;
            c[1] += c[3] * dt;
            c[4] += c[5] * dt;
            c[8] += dt * 6;
            if (c[1] > h + 20) {
                confetti[i] = confetti[--confettiCount];
                confetti[confettiCount] = c;
                i--;
                continue;
            }
            double cos = Math.cos(c[4]);
            double sin = Math.sin(c[4]);
            double hw = c[6];
            double hh = c[6] * 0.5 * Math.abs(Math.cos(c[8]));
            double[][] corners = {{-hw, -hh}, {hw, -hh}, {hw, hh}, {-hw, hh}};
            for (int k = 0; k < 4; k++) {
                xs[k] = c[0] + corners[k][0] * cos - corners[k][1] * sin;
                ys[k] = c[1] + corners[k][0] * sin + corners[k][1] * cos;
            }
            Color col = Color.web(NEON[(int) c[7]]);
            g.setFill(col.deriveColor(0, 1, 0.7 + 0.3 * Math.abs(Math.cos(c[8])), 1));
            g.fillPolygon(xs, ys, 4);
        }

        String[] symbols = {"♪", "♫", "♬"};
        for (int i = 0; i < noteCount; i++) {
            double[] note = notes[i];
            note[2] += dt;
            note[1] -= 55 * dt;
            if (note[2] > 3.2) {
                notes[i] = notes[--noteCount];
                notes[noteCount] = note;
                i--;
                continue;
            }
            double a = Math.sin(Math.PI * Math.min(1, note[2] / 3.2));
            g.setGlobalAlpha(a);
            g.setFont(Font.font("Adwaita Sans", FontWeight.BOLD, note[5]));
            g.setFill(Color.web(NEON[(int) note[4]]));
            g.fillText(symbols[(int) note[3]], note[0] + Math.sin(note[2] * 3 + i) * 14, note[1]);
        }
        g.setGlobalAlpha(1);

        double sinceDrop = beatTime - dropStart;
        if (strobe.isSelected() && sinceDrop >= 0 && sinceDrop < 2 && ((int) (beatTime * 8) & 1) == 0) {
            g.setFill(Color.color(1, 1, 1, 0.45));
            g.fillRect(0, 0, w, h);
        }
    }

    private void drawEqualizer(double dt) {
        GraphicsContext g = equalizer.getGraphicsContext2D();
        double w = equalizer.getWidth();
        double h = equalizer.getHeight();
        g.clearRect(0, 0, w, h);
        double b = beatTime - Math.floor(beatTime);
        double kick = Math.pow(1 - b, 4);
        double hat = Math.exp(-Math.pow((b - 0.5) * 9, 2));
        double gap = 2;
        double bw = (w - gap * (BARS - 1)) / BARS;
        for (int i = 0; i < BARS; i++) {
            double f = i / (double) (BARS - 1);
            double target = (1 - f) * kick * 0.9 + f * hat * 0.75
                    + 0.25 * (0.5 + 0.5 * Math.sin(time * (3 + i * 0.7) + i)) * (0.6 + 0.4 * Math.sin(beatTime * 0.5));
            target = Math.min(1, target);
            levels[i] += (target - levels[i]) * Math.min(1, dt * (target > levels[i] ? 30 : 8));
            peaks[i] = Math.max(levels[i], peaks[i] - dt * 0.6);
            double bh = Math.max(2, levels[i] * (h - 6));
            double x = i * (bw + gap);
            Color c = Color.hsb(330 - f * 280, 0.8, 1);
            g.setFill(new LinearGradient(0, h, 0, h - bh, false, CycleMethod.NO_CYCLE,
                    new Stop(0, c.deriveColor(0, 1, 0.5, 0.8)), new Stop(1, c)));
            g.fillRoundRect(x, h - bh, bw, bh, 3, 3);
            g.setFill(Color.WHITE);
            g.fillRect(x, h - 4 - peaks[i] * (h - 6), bw, 2);
        }
    }

    private void startLoop() {
        new AnimationTimer() {
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
                            System.out.printf(Locale.ROOT, "disco: %s fps, %s ms avg, %s ms worst, %s%n",
                                    fpsValue.getText(), frameValue.getText(), worstValue.getText(), sceneValue.getText());
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
                beatTime += step * bpm.getValue() / 60;
                updateScene(step);
                drawBack();
                drawFore(step);
                drawEqualizer(step);
                bpmValue.setText(String.format(Locale.ROOT, "%.0f", bpm.getValue()));
                int beat = (int) Math.floor(beatTime);
                beatValue.setText(String.format(Locale.ROOT, "%d · %d", beat / 4 + 1, beat % 4 + 1));
                moveValue.setText(MOVES[currentMove]);
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
        sceneValue.setText(String.format(Locale.ROOT, "%,d shapes · %,d triangles · %,d sprites · %d confetti",
                shapes, triangles, sprites, confettiCount));
        double scale = stage.getOutputScaleX();
        sizeValue.setText(String.format(Locale.ROOT, "%.0f×%.0f @%.2f · %.0f×%.0f px · %d dancers", scene.getWidth(),
                scene.getHeight(), scale, scene.getWidth() * scale, scene.getHeight() * scale, dancers.size()));
    }
}
