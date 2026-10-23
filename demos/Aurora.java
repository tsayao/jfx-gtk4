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
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * A showcase of JavaFX on Wayland: a 3D scene animated on the Vulkan pipeline,
 * in an extended stage of the GTK 4 glass over a translucent window backdrop.
 */
@SuppressWarnings("deprecation")
public class Aurora implements EyeCandyDemo.Candy {

    // The selection of the candies, in the middle of the header bar
    private Node switcher;

    private static final String CSS = """
        .root {
            -fx-font-family: "Adwaita Sans", "Cantarell", "System";
            -fx-font-size: 13px;
            -fx-base: #262b3a;
            -fx-background: transparent;
            -fx-control-inner-background: #1b1f2b;
            -fx-control-inner-background-alt: #20253300;
            -fx-accent: #38bdf8;
            -fx-focus-color: #38bdf8;
            -fx-faint-focus-color: #38bdf822;
            -fx-default-button: #2563eb;
            -fx-text-fill: #e6e9f2;
            -aurora-text: #e6e9f2;
            -aurora-muted: #8b93a7;
            -aurora-card: rgba(255, 255, 255, 0.06);
            -aurora-card-border: rgba(255, 255, 255, 0.12);
            -aurora-sidebar: rgba(14, 16, 24, 0.30);
            -aurora-tint: rgba(10, 12, 20, 0.18);
        }
        .root.light {
            -fx-base: #eef1f6;
            -fx-control-inner-background: #ffffff;
            -fx-text-fill: #1c2030;
            -aurora-text: #1c2030;
            -aurora-muted: #5f6678;
            -aurora-card: rgba(255, 255, 255, 0.45);
            -aurora-card-border: rgba(0, 0, 0, 0.08);
            -aurora-sidebar: rgba(245, 247, 252, 0.30);
            -aurora-tint: rgba(250, 251, 255, 0.22);
        }
        .window-fill { -fx-background-color: -aurora-tint; }
        .window-fill.opaque {
            -fx-background-color:
                radial-gradient(center 15% 10%, radius 55%, rgba(56,189,248,0.35), transparent),
                radial-gradient(center 85% 90%, radius 60%, rgba(168,85,247,0.30), transparent),
                linear-gradient(to bottom right, #0b0e17, #121626);
        }
        .root.light .window-fill.opaque {
            -fx-background-color:
                radial-gradient(center 15% 10%, radius 55%, rgba(56,189,248,0.25), transparent),
                radial-gradient(center 85% 90%, radius 60%, rgba(168,85,247,0.18), transparent),
                linear-gradient(to bottom right, #f6f8fc, #e9edf7);
        }
        .label, .text { -fx-text-fill: -aurora-text; -fx-fill: -aurora-text; }
        .muted { -fx-text-fill: -aurora-muted; }
        .mono { -fx-font-family: "Adwaita Mono", "Monospace"; }

        .sidebar {
            -fx-background-color: -aurora-sidebar;
            -fx-border-color: transparent -aurora-card-border transparent transparent;
            -fx-padding: 8 12 14 12;
            -fx-spacing: 4;
        }
        .brand { -fx-font-size: 18px; -fx-font-weight: bold; }
        .brand-sub { -fx-font-size: 11px; -fx-text-fill: -aurora-muted; }
        .nav-section { -fx-font-size: 11px; -fx-text-fill: -aurora-muted; -fx-padding: 14 10 4 10; }
        .nav {
            -fx-background-color: transparent;
            -fx-background-radius: 9;
            -fx-padding: 8 10 8 10;
            -fx-alignment: center-left;
            -fx-graphic-text-gap: 10;
            -fx-text-fill: -aurora-text;
            -fx-font-size: 13px;
            -fx-cursor: hand;
        }
        .nav:hover { -fx-background-color: rgba(255,255,255,0.07); }
        .root.light .nav:hover { -fx-background-color: rgba(0,0,0,0.05); }
        .nav:selected {
            -fx-background-color: linear-gradient(to right, rgba(56,189,248,0.95), rgba(139,92,246,0.95));
            -fx-text-fill: white;
        }
        .nav .icon { -fx-fill: -aurora-muted; }
        .nav:selected .icon { -fx-fill: white; }

        .chip {
            -fx-background-color: rgba(56,189,248,0.14);
            -fx-background-radius: 999;
            -fx-border-color: rgba(56,189,248,0.35);
            -fx-border-radius: 999;
            -fx-padding: 3 10 3 10;
            -fx-font-size: 11px;
            -fx-text-fill: #7dd3fc;
        }
        .chip.violet { -fx-background-color: rgba(168,85,247,0.14); -fx-border-color: rgba(168,85,247,0.4);
                       -fx-text-fill: #d8b4fe; }
        .chip.green { -fx-background-color: rgba(34,197,94,0.14); -fx-border-color: rgba(34,197,94,0.4);
                      -fx-text-fill: #86efac; }
        .chip.amber { -fx-background-color: rgba(245,158,11,0.14); -fx-border-color: rgba(245,158,11,0.4);
                      -fx-text-fill: #fcd34d; }
        .root.light .chip { -fx-text-fill: #0369a1; }
        .root.light .chip.violet { -fx-text-fill: #7e22ce; }
        .root.light .chip.green { -fx-text-fill: #15803d; }
        .root.light .chip.amber { -fx-text-fill: #b45309; }

        .header { -fx-padding: 6 14 6 20; }
        .page-title { -fx-font-size: 21px; -fx-font-weight: bold; }
        .page-subtitle { -fx-font-size: 12px; -fx-text-fill: -aurora-muted; }
        .text-input { -fx-prompt-text-fill: -aurora-muted; }
        .text-input:focused { -fx-prompt-text-fill: transparent; }
        .status-pill {
            -fx-background-color: rgba(255,255,255,0.06);
            -fx-background-radius: 999;
            -fx-border-color: -aurora-card-border;
            -fx-border-radius: 999;
            -fx-padding: 5 12 5 10;
            -fx-spacing: 7;
            -fx-alignment: center;
        }
        .root.light .status-pill { -fx-background-color: rgba(255,255,255,0.6); }
        .status-pill .label { -fx-font-size: 12px; }
        .status-pill .value { -fx-font-weight: bold; }
        .header-divider { -fx-background-color: -aurora-card-border; -fx-pref-width: 1; -fx-max-height: 22; }
        .icon-button {
            -fx-background-color: transparent;
            -fx-background-radius: 8;
            -fx-padding: 6;
            -fx-cursor: hand;
        }
        .icon-button:hover { -fx-background-color: rgba(255,255,255,0.08); }
        .icon-button .icon { -fx-fill: -aurora-text; }

        .card {
            -fx-background-color: -aurora-card;
            -fx-background-radius: 16;
            -fx-border-color: -aurora-card-border;
            -fx-border-radius: 16;
            -fx-padding: 16;
            -fx-spacing: 10;
        }
        .card-title { -fx-font-size: 13px; -fx-font-weight: bold; }
        .metric { -fx-font-size: 28px; -fx-font-weight: bold; }
        .metric-unit { -fx-font-size: 13px; -fx-text-fill: -aurora-muted; }

        .chart { -fx-padding: 0; }
        .chart-plot-background { -fx-background-color: transparent; }
        .chart-vertical-grid-lines, .chart-horizontal-grid-lines { -fx-stroke: rgba(127,127,127,0.12); }
        .chart-alternative-row-fill { -fx-fill: transparent; }
        .axis { -fx-tick-label-fill: -aurora-muted; -fx-tick-label-font-size: 10px; }
        .axis-tick-mark, .axis-minor-tick-mark { -fx-stroke: transparent; }
        .chart-legend { -fx-background-color: transparent; }
        .chart-legend-item { -fx-text-fill: -aurora-muted; }
        .default-color0.chart-series-area-line { -fx-stroke: #38bdf8; -fx-stroke-width: 2px; }
        .default-color0.chart-series-area-fill {
            -fx-fill: linear-gradient(to bottom, rgba(56,189,248,0.45), rgba(56,189,248,0.0)); }
        .default-color1.chart-series-area-line { -fx-stroke: #a855f7; -fx-stroke-width: 2px; }
        .default-color1.chart-series-area-fill {
            -fx-fill: linear-gradient(to bottom, rgba(168,85,247,0.35), rgba(168,85,247,0.0)); }
        .default-color0.chart-area-symbol { -fx-background-color: #38bdf8; }
        .default-color1.chart-area-symbol { -fx-background-color: #a855f7; }
        .chart-pie-label { -fx-fill: -aurora-muted; }
        .default-color0.chart-pie { -fx-pie-color: #38bdf8; }
        .default-color1.chart-pie { -fx-pie-color: #8b5cf6; }
        .default-color2.chart-pie { -fx-pie-color: #22c55e; }
        .default-color3.chart-pie { -fx-pie-color: #f59e0b; }
        .chart-pie { -fx-border-color: transparent; }
        .default-color0.chart-bar {
            -fx-background-color: linear-gradient(to top, #2563eb, #38bdf8); -fx-background-radius: 6 6 0 0; }

        .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-pane { -fx-padding: 0; }
        .scroll-bar { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: rgba(127,127,127,0.35); -fx-background-radius: 6; }
        .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 0; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-shape: " "; -fx-padding: 0; }

        .button.accent {
            -fx-background-color: linear-gradient(to right, #38bdf8, #8b5cf6);
            -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;
        }
        .button.outline {
            -fx-background-color: transparent; -fx-border-color: #38bdf8; -fx-border-radius: 8;
            -fx-text-fill: #38bdf8;
        }
        .segmented .toggle-button { -fx-background-radius: 0; -fx-padding: 6 14 6 14; }
        .segmented .toggle-button:first-child { -fx-background-radius: 8 0 0 8; }
        .segmented .toggle-button:last-child { -fx-background-radius: 0 8 8 0; }
        .segmented .toggle-button:selected { -fx-base: #2563eb; }
        .progress-bar > .bar {
            -fx-background-color: linear-gradient(to right, #38bdf8, #8b5cf6);
            -fx-background-radius: 6; -fx-background-insets: 0;
        }
        .progress-bar > .track { -fx-background-radius: 6; }
        .table-view, .list-view, .tree-view, .tab-pane > .tab-content-area, .text-area .content {
            -fx-background-color: rgba(0,0,0,0.18);
        }
        .root.light .table-view, .root.light .list-view, .root.light .tree-view,
        .root.light .text-area .content { -fx-background-color: rgba(255,255,255,0.6); }
        .table-view .column-header-background, .table-view .column-header { -fx-background-color: transparent; }
        .table-row-cell, .list-cell, .tree-cell { -fx-background-color: transparent; -fx-text-fill: -aurora-text; }
        /* The list of a combo box pops up over the page: opaque */
        .combo-box-popup > .list-view { -fx-background-color: #161a26; -fx-background-radius: 8; }
        .root.light .combo-box-popup > .list-view { -fx-background-color: #f6f7fb; }
        .table-row-cell:selected, .list-cell:selected, .tree-cell:selected {
            -fx-background-color: rgba(56,189,248,0.25);
        }
        .table-cell { -fx-text-fill: -aurora-text; }
        .tooltip { -fx-background-color: rgba(20,22,32,0.92); -fx-background-radius: 8; -fx-text-fill: white; }
        .info-key { -fx-text-fill: -aurora-muted; }
        .hud { -fx-background-color: rgba(10,12,20,0.55); }
        .root.light .hud { -fx-background-color: rgba(255,255,255,0.65); }
        .hud-title { -fx-font-size: 22px; -fx-font-weight: bold; }
        .stat { -fx-font-size: 20px; -fx-font-weight: bold; }
        .stat-key { -fx-font-size: 11px; -fx-text-fill: -aurora-muted; }
        .opaque-panel { -fx-background-color: #121626; }
        .root.light .opaque-panel { -fx-background-color: #f2f4f9; }
        .side-panel {
            -fx-border-color: transparent -aurora-card-border transparent transparent;
            -fx-padding: 14; -fx-spacing: 10;
        }
        .preset { -fx-background-radius: 8; }
        .info-value { -fx-font-family: "Adwaita Mono", "Monospace"; }
        """;

    private static final String STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));

    private final Label pageTitle = new Label();
    private final Label pageSubtitle = new Label();
    private final Label fpsValue = new Label("–");
    private Scene scene;
    private Stage stage;
    private boolean backdrop;
    private double fps;
    @Override
    public Scene scene(Stage stage, Node switcher, double width, double height) {
        this.switcher = switcher;
        this.stage = stage;
        backdrop = Boolean.getBoolean("glass.gtk4.backdrop.active");

        Region fill = new Region();
        fill.getStyleClass().add("window-fill");
        if (!backdrop) {
            fill.getStyleClass().add("opaque");
        }

        pageTitle.setText("Vulkan");
        pageSubtitle.setText("Real-time 3D on the Prism Vulkan pipeline");
        BorderPane body = new BorderPane();
        body.setTop(header());
        body.setCenter(vulkan());

        body.setMinSize(0, 0);
        StackPane root = new StackPane(fill, body);
        root.setAlignment(Pos.TOP_LEFT);
        scene = new Scene(root, width, height, Color.TRANSPARENT);
        styles(scene, false);

        startMeters();
        return scene;
    }

    // ---------------------------------------------------------------------
    // Shell

    private static SVGPath icon(String path, double size) {
        SVGPath icon = new SVGPath();
        icon.setContent(path);
        icon.getStyleClass().add("icon");
        double scale = size / 24.0;
        icon.setScaleX(scale);
        icon.setScaleY(scale);
        return icon;
    }

    private static Group sized(SVGPath icon, double size) {
        StackPane wrap = new StackPane(icon);
        wrap.setMinSize(size, size);
        wrap.setPrefSize(size, size);
        wrap.setMaxSize(size, size);
        return new Group(wrap);
    }

    private static final String ICON_MOON = "M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z";
    private static final String ICON_BELL = "M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9M13.7 21a2 2 0 0 1-3.4 0";

    private Node header() {
        pageTitle.getStyleClass().add("page-title");
        pageSubtitle.getStyleClass().add("page-subtitle");
        VBox titles = new VBox(1, pageTitle, pageSubtitle);
        titles.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(4, Color.web("#22c55e"));
        dot.setEffect(new javafx.scene.effect.DropShadow(8, Color.web("#22c55e")));
        FadeTransition pulse = new FadeTransition(Duration.millis(900), dot);
        pulse.setFromValue(1);
        pulse.setToValue(0.35);
        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
        Label fps = new Label();
        fps.getStyleClass().addAll("value", "mono");
        fps.textProperty().bind(fpsValue.textProperty().concat(" fps"));
        HBox live = new HBox(dot, fps);
        live.getStyleClass().add("status-pill");

        Label pipeline = new Label(pipelineName() + " · GTK 4");
        HBox stack = new HBox(pipeline);
        stack.getStyleClass().add("status-pill");

        Region divider = new Region();
        divider.getStyleClass().add("header-divider");

        Button theme = new Button();
        theme.getStyleClass().setAll("icon-button");
        SVGPath moon = icon(ICON_MOON, 18);
        moon.setFill(Color.TRANSPARENT);
        moon.setStroke(Color.web("#8b93a7"));
        moon.setStrokeWidth(2.4);
        theme.setGraphic(sized(moon, 18));
        theme.setTooltip(new Tooltip("Toggle light/dark theme"));
        theme.setOnAction(e -> toggleTheme());

        MenuButton bell = new MenuButton();
        bell.getStyleClass().setAll("icon-button");
        SVGPath b = icon(ICON_BELL, 18);
        b.setFill(Color.TRANSPARENT);
        b.setStroke(Color.web("#8b93a7"));
        b.setStrokeWidth(2.4);
        bell.setGraphic(sized(b, 18));
        bell.getItems().addAll(new MenuItem("Vulkan pipeline initialized"),
                new MenuItem("Backdrop " + (backdrop ? "active" : "unavailable in this compositor")),
                new SeparatorMenuItem(), new MenuItem("Mark all as read"));

        HBox right = new HBox(10, live, stack, divider, theme, bell);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setPadding(new Insets(0, 8, 0, 0));

        HeaderBar.setDragType(titles, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar.setDragType(live, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar.setDragType(stack, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar.setDragType(divider, HeaderDragType.DRAGGABLE);
        HeaderBar.setDragType(right, HeaderDragType.DRAGGABLE);

        HeaderBar header = new HeaderBar(titles, switcher, right);
        header.setLeftSystemPadding(false);
        header.getStyleClass().add("header");
        header.setMinHeight(64);
        header.setBackground(null);
        HeaderBar.setAlignment(titles, Pos.CENTER_LEFT);
        return header;
    }

    private static void styles(Scene scene, boolean light) {
        scene.getStylesheets().add(STYLESHEET);
        if (light) {
            scene.getRoot().getStyleClass().add("light");
        }
        scene.getPreferences().setColorScheme(light ? ColorScheme.LIGHT : ColorScheme.DARK);
    }

    private void toggleTheme() {
        boolean light = !scene.getRoot().getStyleClass().contains("light");
        if (light) {
            scene.getRoot().getStyleClass().add("light");
        } else {
            scene.getRoot().getStyleClass().remove("light");
        }
        scene.getPreferences().setColorScheme(light ? ColorScheme.LIGHT : ColorScheme.DARK);
    }

    // ---------------------------------------------------------------------
    // 3D

    static TriangleMesh torus(int rings, int sides, float radius, float tube) {
        TriangleMesh mesh = new TriangleMesh();
        for (int i = 0; i < rings; i++) {
            double u = 2 * Math.PI * i / rings;
            for (int j = 0; j < sides; j++) {
                double v = 2 * Math.PI * j / sides;
                float x = (float) ((radius + tube * Math.cos(v)) * Math.cos(u));
                float y = (float) (tube * Math.sin(v));
                float z = (float) ((radius + tube * Math.cos(v)) * Math.sin(u));
                mesh.getPoints().addAll(x, y, z);
            }
        }
        for (int i = 0; i <= rings; i++) {
            for (int j = 0; j <= sides; j++) {
                mesh.getTexCoords().addAll((float) i / rings, (float) j / sides);
            }
        }
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < sides; j++) {
                int a = i * sides + j;
                int b = ((i + 1) % rings) * sides + j;
                int c = ((i + 1) % rings) * sides + (j + 1) % sides;
                int d = i * sides + (j + 1) % sides;
                int ta = i * (sides + 1) + j;
                int tb = (i + 1) * (sides + 1) + j;
                int tc = (i + 1) * (sides + 1) + j + 1;
                int td = i * (sides + 1) + j + 1;
                mesh.getFaces().addAll(a, ta, d, td, b, tb);
                mesh.getFaces().addAll(b, tb, d, td, c, tc);
            }
        }
        return mesh;
    }

    // ---------------------------------------------------------------------
    // Vulkan

    private static PhongMaterial[] palette(int count, String... stops) {
        PhongMaterial[] materials = new PhongMaterial[count];
        for (int i = 0; i < count; i++) {
            double f = (double) i / (count - 1) * (stops.length - 1);
            int k = Math.min((int) f, stops.length - 2);
            Color c = Color.web(stops[k]).interpolate(Color.web(stops[k + 1]), f - k);
            PhongMaterial m = new PhongMaterial(c);
            m.setSpecularColor(c.interpolate(Color.WHITE, 0.3));
            m.setSpecularPower(48);
            materials[i] = m;
        }
        return materials;
    }

    private static Pane fit(SubScene sub) {
        Pane view = new Pane(sub);
        sub.setManaged(false);
        sub.widthProperty().bind(view.widthProperty());
        sub.heightProperty().bind(view.heightProperty());
        Rectangle clip = new Rectangle();
        clip.setArcWidth(32);
        clip.setArcHeight(32);
        clip.widthProperty().bind(view.widthProperty());
        clip.heightProperty().bind(view.heightProperty());
        view.setClip(clip);
        view.setMinSize(0, 0);
        return view;
    }

    private Node vulkan() {
        int n = 30;
        double pitch = 18;
        Box[] boxes = new Box[n * n];
        double[] dist = new double[n * n];
        int[] shade = new int[n * n];
        Group field = new Group();
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                Box b = new Box(pitch * 0.82, 1, pitch * 0.82);
                double x = (i - (n - 1) / 2.0) * pitch;
                double z = (j - (n - 1) / 2.0) * pitch;
                b.setTranslateX(x);
                b.setTranslateZ(z);
                boxes[i * n + j] = b;
                dist[i * n + j] = Math.hypot(x, z);
                shade[i * n + j] = -1;
                field.getChildren().add(b);
            }
        }
        PhongMaterial[][] palettes = {
            palette(32, "#1e3a8a", "#0ea5e9", "#a855f7", "#f472b6"),
            palette(32, "#064e3b", "#22c55e", "#facc15", "#f97316"),
            palette(32, "#450a0a", "#dc2626", "#f59e0b", "#fef3c7"),
            palette(32, "#0f172a", "#475569", "#cbd5e1", "#ffffff")
        };
        String[] paletteNames = {"Aurora", "Meadow", "Ember", "Graphite"};

        MeshView core = new MeshView(torus(72, 36, 90, 26));
        PhongMaterial metal = new PhongMaterial(Color.web("#1e293b"));
        metal.setSpecularColor(Color.web("#e0f2fe"));
        metal.setSpecularPower(32);
        core.setMaterial(metal);
        MeshView cage = new MeshView(torus(36, 18, 90, 29));
        cage.setDrawMode(DrawMode.LINE);
        cage.setMaterial(new PhongMaterial(Color.web("#38bdf8")));
        Sphere heart = new Sphere(34, 64);
        PhongMaterial glow = new PhongMaterial(Color.web("#a855f7"));
        glow.setSpecularColor(Color.WHITE);
        heart.setMaterial(glow);
        Rotate spinX = new Rotate(0, Rotate.X_AXIS);
        Rotate spinY = new Rotate(0, Rotate.Y_AXIS);
        Group ring = new Group(core, cage);
        ring.getTransforms().addAll(spinY, spinX);
        Group reactor = new Group(ring, heart);
        reactor.setTranslateY(-190);

        int count = 140;
        Sphere[] sparks = new Sphere[count];
        Group particles = new Group();
        PhongMaterial[] sparkColors = palette(8, "#38bdf8", "#a855f7", "#22c55e", "#f59e0b");
        for (int i = 0; i < count; i++) {
            Sphere s = new Sphere(3.2, 10);
            s.setMaterial(sparkColors[i % sparkColors.length]);
            sparks[i] = s;
            particles.getChildren().add(s);
        }

        PointLight key = new PointLight(Color.web("#bae6fd"));
        key.setTranslateX(-520);
        key.setTranslateY(-620);
        key.setTranslateZ(-480);
        PointLight rim = new PointLight(Color.web("#f0abfc"));
        rim.setTranslateX(520);
        rim.setTranslateY(-360);
        rim.setTranslateZ(420);
        PointLight pulse = new PointLight(Color.web("#a855f7"));
        pulse.setTranslateY(-190);
        pulse.setMaxRange(700);
        AmbientLight ambient = new AmbientLight(Color.rgb(46, 50, 72));

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setNearClip(1);
        camera.setFarClip(6000);
        camera.setFieldOfView(42);
        camera.setTranslateZ(-1050);
        Rotate yaw = new Rotate(30, Rotate.Y_AXIS);
        Rotate tilt = new Rotate(-28, Rotate.X_AXIS);
        Group rig = new Group(camera);
        rig.getTransforms().addAll(yaw, tilt);
        rig.setTranslateY(-80);

        Group world = new Group(field, reactor, particles, key, rim, pulse, ambient, rig);
        SubScene sub = new SubScene(world, 800, 600, true, SceneAntialiasing.BALANCED);
        sub.setFill(Color.TRANSPARENT);
        sub.setCamera(camera);

        double[] drag = new double[2];
        sub.setOnMousePressed(e -> {
            drag[0] = e.getSceneX();
            drag[1] = e.getSceneY();
        });
        sub.setOnMouseDragged(e -> {
            yaw.setAngle(yaw.getAngle() + (e.getSceneX() - drag[0]) * 0.3);
            tilt.setAngle(Math.max(-85, Math.min(-4, tilt.getAngle() - (e.getSceneY() - drag[1]) * 0.3)));
            drag[0] = e.getSceneX();
            drag[1] = e.getSceneY();
        });
        sub.setOnScroll(e -> camera.setTranslateZ(
                Math.max(-2600, Math.min(-380, camera.getTranslateZ() + e.getDeltaY() * 1.5))));

        Slider speed = new Slider(0, 3, 1);
        Slider amplitude = new Slider(10, 160, 90);
        ChoiceBox<String> colors = new ChoiceBox<>(FXCollections.observableArrayList(paletteNames));
        colors.setValue(paletteNames[0]);
        colors.valueProperty().addListener((o, a, b) -> java.util.Arrays.fill(shade, -1));
        CheckBox orbit = new CheckBox("Orbit camera");
        orbit.setSelected(true);
        CheckBox wire = new CheckBox("Wireframe field");
        wire.selectedProperty().addListener((o, a, b) -> {
            for (Box box : boxes) {
                box.setDrawMode(b ? DrawMode.LINE : DrawMode.FILL);
            }
        });
        CheckBox showReactor = new CheckBox("Reactor and particles");
        showReactor.setSelected(true);
        reactor.visibleProperty().bind(showReactor.selectedProperty());
        particles.visibleProperty().bind(showReactor.selectedProperty());
        pulse.lightOnProperty().bind(showReactor.selectedProperty());
        ToggleButton pause = new ToggleButton("Pause");

        Label title = new Label("Prism · Vulkan");
        title.getStyleClass().add("hud-title");
        Label subtitle = new Label("Live 3D rendered through the Vulkan pipeline via FFM");
        subtitle.getStyleClass().add("muted");
        Label fpsStat = new Label();
        fpsStat.textProperty().bind(fpsValue.textProperty());
        int triangles = boxes.length * 12
                + ((TriangleMesh) core.getMesh()).getFaces().size() / 6
                + ((TriangleMesh) cage.getMesh()).getFaces().size() / 6
                + 64 * 64 + count * 10 * 10;
        HBox stats = new HBox(22,
                stat("FPS", fpsStat),
                stat("Nodes", new Label(String.valueOf(boxes.length + count + 3))),
                stat("Triangles", new Label(String.format(java.util.Locale.ROOT, "%,d", triangles))),
                stat("Lights", new Label("3 + ambient")),
                stat("MSAA", new Label("4×")));
        VBox hud = new VBox(8, title, subtitle, stats);
        hud.getStyleClass().addAll("card", "hud");
        hud.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        GridPane form = new GridPane(10, 8);
        form.addRow(0, new Label("Speed"), speed);
        form.addRow(1, new Label("Amplitude"), amplitude);
        form.addRow(2, new Label("Palette"), colors);
        VBox controls = new VBox(10, form, orbit, wire, showReactor, pause);
        controls.getStyleClass().addAll("card", "hud");
        controls.setPrefWidth(280);
        controls.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Label hint = new Label("Drag to orbit · scroll to zoom");
        hint.getStyleClass().add("muted");

        StackPane overlay = new StackPane(hud, controls, hint);
        overlay.setPickOnBounds(false);
        overlay.setPadding(new Insets(18));
        StackPane.setAlignment(hud, Pos.TOP_LEFT);
        StackPane.setAlignment(controls, Pos.BOTTOM_RIGHT);
        StackPane.setAlignment(hint, Pos.BOTTOM_LEFT);

        Pane view = fit(sub);
        StackPane frame = new StackPane(view, overlay);
        frame.getStyleClass().add("card");
        frame.setPadding(Insets.EMPTY);
        StackPane page = new StackPane(frame);
        page.setPadding(new Insets(4, 18, 18, 18));

        new EyeCandyDemo.Loop() {
            long last;
            double t;

            @Override
            public void handle(long now) {
                double dt = last == 0 ? 0 : Math.min((now - last) / 1e9, 0.1);
                last = now;
                if (!page.isVisible() || pause.isSelected()) {
                    return;
                }
                t += dt * speed.getValue();
                if (orbit.isSelected()) {
                    yaw.setAngle(yaw.getAngle() + dt * 9);
                }
                double amp = amplitude.getValue();
                PhongMaterial[] materials = palettes[java.util.Arrays.asList(paletteNames).indexOf(colors.getValue())];
                for (int k = 0; k < boxes.length; k++) {
                    double d = dist[k];
                    double wave = Math.sin(d * 0.021 - t * 2.4) * 0.6 + Math.sin(d * 0.007 + t * 1.1) * 0.4;
                    double falloff = 1 - Math.min(d / 420, 0.75);
                    double v = (wave + 1) / 2 * falloff;
                    double h = 4 + amp * v;
                    Box b = boxes[k];
                    b.setScaleY(h);
                    b.setTranslateY(-h / 2);
                    int m = (int) Math.round(Math.min(v, 1) * (materials.length - 1));
                    if (m != shade[k]) {
                        shade[k] = m;
                        b.setMaterial(materials[m]);
                    }
                }
                spinY.setAngle(t * 40);
                spinX.setAngle(Math.sin(t * 0.7) * 35 + 70);
                double beat = 1 + 0.12 * Math.sin(t * 4);
                heart.setScaleX(beat);
                heart.setScaleY(beat);
                heart.setScaleZ(beat);
                reactor.setTranslateY(-190 + Math.sin(t * 1.3) * 18);
                pulse.setTranslateY(reactor.getTranslateY());
                pulse.setColor(Color.hsb(275 + 25 * Math.sin(t * 2), 0.7, 0.6 + 0.4 * (beat - 0.88) / 0.24));
                for (int i = 0; i < count; i++) {
                    double a = i * 2.39996 + t * (0.5 + (i % 5) * 0.08);
                    double r = 150 + 70 * Math.sin(i * 0.37 + t * 0.9);
                    Sphere s = sparks[i];
                    s.setTranslateX(Math.cos(a) * r);
                    s.setTranslateZ(Math.sin(a) * r);
                    s.setTranslateY(reactor.getTranslateY() + Math.sin(a * 2 + i) * 90);
                }
            }
        }.start();
        return page;
    }

    private static VBox stat(String key, Label value) {
        value.getStyleClass().addAll("stat", "mono");
        Label k = new Label(key.toUpperCase());
        k.getStyleClass().add("stat-key");
        return new VBox(0, k, value);
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

    // ---------------------------------------------------------------------
    // Meters

    private void startMeters() {
        new EyeCandyDemo.Loop() {
            long last;
            int count;

            @Override
            public void handle(long now) {
                count++;
                if (last == 0) {
                    last = now;
                } else if (now - last >= 500_000_000L) {
                    fps = count * 1e9 / (now - last);
                    count = 0;
                    last = now;
                    fpsValue.setText(String.format("%.0f", fps));
                }
            }
        }.start();
    }
}
