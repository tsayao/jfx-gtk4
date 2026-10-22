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
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HeaderBar;
import javafx.scene.layout.HeaderDragType;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Paint;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

/**
 * A laboratory for stages: creates stages of every style, with or without a
 * window backdrop, modality and owner, and edits their properties live. It
 * covers the cases of the TestStage and BackdropTest manual tests in a command
 * window drawn as glass over a backdrop.
 */
public class StageDemo extends Application {

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
            -fx-accent: #2dd4bf;
            -fx-focus-color: #5eead4;
            -fx-faint-focus-color: #5eead422;
            -fx-text-fill: #e8eef7;
        }
        .label, .check-box { -fx-text-fill: #e8eef7; }
        .muted { -fx-text-fill: #93a1b8; }
        .mono { -fx-font-family: "Adwaita Mono", "Monospace"; }
        .glass {
            -fx-background-color: rgba(10, 14, 24, 0.52);
            -fx-background-radius: 18;
            -fx-border-color: rgba(255, 255, 255, 0.16) rgba(255, 255, 255, 0.07)
                rgba(255, 255, 255, 0.04) rgba(255, 255, 255, 0.07);
            -fx-border-radius: 18;
            -fx-padding: 16;
        }
        .section { -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #93a1b8; }
        .title { -fx-font-size: 22px; -fx-font-weight: bold; }
        .hero { -fx-font-size: 30px; -fx-font-weight: bold; }
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
        .badge {
            -fx-background-color: rgba(45, 212, 191, 0.18);
            -fx-background-radius: 999;
            -fx-padding: 1 8 1 8;
            -fx-font-size: 10px;
            -fx-text-fill: #99f6e4;
        }
        .badge.off { -fx-background-color: rgba(255, 255, 255, 0.07); -fx-text-fill: #93a1b8; }
        .check-box > .box {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 5;
            -fx-border-color: rgba(255, 255, 255, 0.22);
            -fx-border-radius: 5;
        }
        .check-box:selected > .box {
            -fx-background-color: linear-gradient(to bottom right, #2dd4bf, #6366f1);
            -fx-border-color: transparent;
        }
        .check-box:selected > .box > .mark { -fx-background-color: white; }
        .button, .toggle-button {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 9;
            -fx-text-fill: #e8eef7;
            -fx-padding: 6 14 6 14;
            -fx-cursor: hand;
        }
        .button:hover, .toggle-button:hover { -fx-background-color: rgba(255, 255, 255, 0.15); }
        .button:disabled { -fx-opacity: 0.4; }
        .toggle-button:selected { -fx-background-color: rgba(45, 212, 191, 0.3); -fx-border-color: #2dd4bf; }
        .button.accent {
            -fx-background-color: linear-gradient(to right, #2dd4bf, #6366f1, #d946ef);
            -fx-border-color: transparent;
            -fx-text-fill: white;
            -fx-font-weight: bold;
        }
        .combo-box, .text-field, .spinner, .text-area, .date-picker, .color-picker {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.16);
            -fx-border-radius: 9;
        }
        .text-field, .text-area, .spinner .text-field, .combo-box .list-cell { -fx-text-fill: #e8eef7; }
        .text-area .content { -fx-background-color: transparent; }
        .spinner .text-field, .date-picker .text-field { -fx-background-color: transparent; -fx-border-color: transparent; }
        .combo-box-popup .list-view, .context-menu {
            -fx-background-color: rgba(18, 22, 34, 0.96);
            -fx-background-radius: 10;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 10;
        }
        .list-view {
            -fx-background-color: transparent;
            -fx-border-color: transparent;
        }
        .list-view .list-cell { -fx-background-color: transparent; -fx-text-fill: #e8eef7; -fx-padding: 6 8 6 8; }
        .list-view .list-cell:filled:hover { -fx-background-color: rgba(255, 255, 255, 0.06); -fx-background-radius: 9; }
        .list-view .list-cell:filled:selected {
            -fx-background-color: rgba(45, 212, 191, 0.22);
            -fx-background-radius: 9;
        }
        .slider .track { -fx-background-color: rgba(255, 255, 255, 0.14); -fx-background-radius: 4; }
        .slider .thumb { -fx-background-color: white; }
        .progress-bar > .track { -fx-background-color: rgba(255, 255, 255, 0.12); -fx-background-radius: 6; }
        .progress-bar > .bar {
            -fx-background-color: linear-gradient(to right, #2dd4bf, #6366f1);
            -fx-background-radius: 6;
            -fx-background-insets: 0;
        }
        .dialog-pane {
            -fx-background-color: #151a26;
            -fx-border-color: rgba(255, 255, 255, 0.12);
        }
        .dialog-pane > .header-panel {
            -fx-background-color: linear-gradient(to right, rgba(45, 212, 191, 0.18), rgba(99, 102, 241, 0.18));
            -fx-border-color: transparent transparent rgba(255, 255, 255, 0.1) transparent;
            -fx-padding: 16 18 16 18;
        }
        .dialog-pane > .header-panel .label { -fx-text-fill: #e8eef7; -fx-font-size: 15px; -fx-font-weight: bold; }
        .dialog-pane > .content.label { -fx-text-fill: #e8eef7; -fx-padding: 16 18 8 18; }
        .dialog-pane > .button-bar > .container { -fx-padding: 12 18 16 18; }
        .dialog-pane:header .header-panel .graphic-container { -fx-padding: 0 0 0 12; }
        .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-pane { -fx-padding: 0; }
        .scroll-bar { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: rgba(255, 255, 255, 0.25); -fx-background-radius: 6; }
        .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 0; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-shape: " "; -fx-padding: 0; }
        """;

    private static final String STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));

    private static final double MAX_WIDTH = 7680;
    private static final double MAX_HEIGHT = 4320;
    private static final int LOG_SIZE = 200;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final String[] ACCENTS = {"#2dd4bf", "#6366f1", "#d946ef", "#f59e0b", "#22c55e", "#f43f5e"};

    private record BackdropChoice(String name) {
        @Override
        public String toString() {
            return name == null ? "None" : name;
        }
    }

    private enum FillChoice {
        TRANSPARENT("Transparent", Color.TRANSPARENT),
        NONE("None (null)", null),
        TINT("Dark tint (35%)", Color.color(0.05, 0.07, 0.12, 0.35)),
        RED("Red (50%)", Color.color(1, 0, 0, 0.5)),
        GREEN("Green (20%)", Color.color(0, 1, 0, 0.2)),
        IMAGE("Blue image (20%)", solidImage(Color.color(0, 0, 1, 0.2))),
        OPAQUE("Opaque gradient", new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#0f172a")), new Stop(1, Color.web("#312e81"))));

        final String label;
        final Paint paint;

        FillChoice(String label, Paint paint) {
            this.label = label;
            this.paint = paint;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private enum Content {
        INFO("Stage info"),
        TEXT("Text input"),
        TOOLTIP("Tooltip box"),
        CONTROLS("Controls gallery");

        final String label;

        Content(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static ImagePattern solidImage(Color color) {
        WritableImage image = new WritableImage(1, 1);
        image.getPixelWriter().setColor(0, 0, color);
        return new ImagePattern(image, 0, 0, 1, 1, true);
    }

    private Stage commandStage;
    private boolean backdropSupported;
    private final List<BackdropChoice> backdropChoices = new ArrayList<>();
    private final ObservableList<Stage> stages = FXCollections.observableArrayList();
    private final Map<Stage, ObservableList<String>> logs = new HashMap<>();
    private final Map<Stage, StringProperty> lastEvents = new HashMap<>();
    private int counter;
    private Stage current;
    private boolean selecting;

    private final ComboBox<StageStyle> styleBox = new ComboBox<>(FXCollections.observableArrayList(StageStyle.values()));
    private final ComboBox<BackdropChoice> backdropBox = new ComboBox<>();
    private final ComboBox<Modality> modalityBox = new ComboBox<>(FXCollections.observableArrayList(Modality.values()));
    private final ComboBox<Stage> ownerBox = new ComboBox<>();
    private final ComboBox<Content> contentBox = new ComboBox<>(FXCollections.observableArrayList(Content.values()));
    private final ComboBox<FillChoice> fillBox = new ComboBox<>(FXCollections.observableArrayList(FillChoice.values()));
    private final ComboBox<ColorScheme> schemeBox = new ComboBox<>(FXCollections.observableArrayList(ColorScheme.values()));
    private final Slider opacity = new Slider(0.2, 1, 1);
    private final ListView<Stage> stageList = new ListView<>(stages);
    private final ListView<String> logView = new ListView<>();
    private final PropertyGrid stageProps = new PropertyGrid();
    private final PropertyGrid sceneProps = new PropertyGrid();
    private final Label selectedTitle = new Label("No stage selected");
    private final List<Button> stageButtons = new ArrayList<>();
    private final Label countPill = new Label();

    @Override
    public void start(Stage stage) {
        commandStage = stage;
        backdropSupported = Boolean.getBoolean("glass.gtk4.backdrop.active");
        backdropChoices.add(new BackdropChoice(null));
        if (backdropSupported) {
            backdropChoices.add(new BackdropChoice("Window"));
            backdropChoices.add(new BackdropChoice("Partial"));
        }

        stage.initStyle(StageStyle.EXTENDED);

        BorderPane body = new BorderPane();
        body.setTop(header());
        HBox columns = new HBox(14, column(createCard(), stagesCard(), dialogsCard()), column(propertiesCard(), logCard()));
        HBox.setHgrow(columns.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(columns.getChildren().get(1), Priority.ALWAYS);
        columns.setPadding(new Insets(4, 18, 18, 18));
        body.setCenter(columns);

        StackPane root = new StackPane(backgroundLayer(), body);
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, Math.min(1180, screen.getWidth() * 0.9), Math.min(920, screen.getHeight() * 0.92),
                Color.TRANSPARENT);
        scene.getStylesheets().add(STYLESHEET);
        scene.getPreferences().setColorScheme(ColorScheme.DARK);
        stage.setScene(scene);
        stage.setTitle("Stage Lab");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        selectStage(null);
        stage.show();
    }

    private Region backgroundLayer() {
        Region fill = new Region();
        if (backdropSupported) {
            fill.setBackground(new Background(new BackgroundFill(Color.color(0.02, 0.03, 0.06, 0.22), null, null)));
        } else {
            fill.setBackground(new Background(new BackgroundFill(new RadialGradient(0, 0, 0.2, 0.1, 1.2, true,
                    CycleMethod.NO_CYCLE, new Stop(0, Color.web("#1e293b")), new Stop(0.6, Color.web("#0b1120")),
                    new Stop(1, Color.web("#050811"))), null, null)));
        }
        return fill;
    }

    private static VBox column(Node... cards) {
        VBox box = new VBox(14, cards);
        box.setFillWidth(true);
        box.setMinWidth(0);
        return box;
    }

    private Node header() {
        Circle logo = new Circle(10, new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#2dd4bf")), new Stop(1, Color.web("#6366f1"))));
        logo.setEffect(new Glow(0.6));
        Label brand = new Label("Stage Lab");
        brand.getStyleClass().add("brand");
        Label sub = new Label("styles · backdrops · modality · properties");
        sub.getStyleClass().add("muted");
        HBox left = new HBox(10, logo, brand, sub);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 18));

        countPill.textProperty().bind(Bindings.size(stages).asString("%d stages"));
        HBox count = pill(countPill);
        HBox backdrop = pill(new Label(backdropSupported ? "Backdrop: " + (backdropChoices.size() - 1) + " styles"
                : "Backdrop unsupported"));
        HBox platform = pill(new Label(System.getProperty("glass.platform", "default") + " glass"));
        HBox right = new HBox(10, count, backdrop, platform);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setPadding(new Insets(0, 10, 0, 0));

        HeaderBar.setDragType(left, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar.setDragType(right, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar header = new HeaderBar(left, null, right);
        header.setBackground(null);
        header.setMinHeight(52);
        HeaderBar.setAlignment(left, Pos.CENTER_LEFT);
        return header;
    }

    private static HBox pill(Label label) {
        HBox box = new HBox(label);
        box.getStyleClass().add("pill");
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

    private static Label muted(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        return l;
    }

    private Node createCard() {
        styleBox.setValue(StageStyle.DECORATED);
        backdropBox.getItems().setAll(backdropChoices);
        backdropBox.setValue(backdropChoices.size() > 1 ? backdropChoices.get(1) : backdropChoices.get(0));
        backdropBox.setDisable(!backdropSupported);
        modalityBox.setValue(Modality.NONE);
        contentBox.setValue(Content.INFO);
        fillBox.setValue(FillChoice.TRANSPARENT);
        schemeBox.setValue(Platform.getPreferences().getColorScheme());
        ownerBox.itemsProperty().bind(Bindings.createObjectBinding(() -> {
            ObservableList<Stage> list = FXCollections.observableArrayList();
            list.add(null);
            list.add(commandStage);
            list.addAll(stages);
            return list;
        }, stages));
        ownerBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(Stage s) {
                return s == null ? "None" : s.getTitle();
            }

            @Override
            public Stage fromString(String string) {
                return null;
            }
        });
        ownerBox.setValue(null);

        GridPane grid = new GridPane(12, 8);
        addRow(grid, 0, "Style", styleBox, "Backdrop", backdropBox);
        addRow(grid, 1, "Modality", modalityBox, "Owner", ownerBox);
        addRow(grid, 2, "Content", contentBox, "Scene fill", fillBox);
        addRow(grid, 3, "Color scheme", schemeBox, "Opacity", opacity);

        Button create = new Button("Create");
        create.setTooltip(new Tooltip("Create a stage without showing it"));
        create.setOnAction(e -> createStage(false));
        Button createShow = new Button("Create + show");
        createShow.getStyleClass().add("accent");
        createShow.setOnAction(e -> createStage(true));
        Button gallery = new Button("Backdrop gallery");
        gallery.setTooltip(new Tooltip("One extended stage for every backdrop style"));
        gallery.setOnAction(e -> backdropGallery());
        gallery.setDisable(!backdropSupported);
        HBox buttons = new HBox(8, createShow, create, gallery);

        Label hint = muted(backdropSupported
                ? "The backdrop is set before the stage is shown and shines through transparent fills."
                : "This platform does not support window backdrops.");
        hint.setWrapText(true);
        return card("New stage", grid, buttons, hint);
    }

    private static void addRow(GridPane grid, int row, String l1, Region c1, String l2, Region c2) {
        Label a = muted(l1);
        Label b = muted(l2);
        c1.setMaxWidth(Double.MAX_VALUE);
        c2.setMaxWidth(Double.MAX_VALUE);
        grid.add(a, 0, row);
        grid.add(c1, 1, row);
        grid.add(b, 2, row);
        grid.add(c2, 3, row);
        GridPane.setHgrow(c1, Priority.ALWAYS);
        GridPane.setHgrow(c2, Priority.ALWAYS);
        c1.setMinWidth(120);
        c2.setMinWidth(120);
    }

    private Node stagesCard() {
        stageList.setPlaceholder(muted("Create a stage to begin"));
        stageList.setPrefHeight(170);
        stageList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Stage s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Circle dot = new Circle(5, Color.web((String) s.getProperties().getOrDefault("demo.accent", "#2dd4bf")));
                Label name = new Label(s.getTitle());
                Label info = muted(s.getStyle() + " · " + backdropName(s) + " · " + s.getModality());
                Label showing = badge(s.isShowing() ? "showing" : "hidden", s.isShowing());
                Label focused = badge(s.isFocused() ? "focused" : "", s.isFocused());
                focused.setVisible(s.isFocused());
                Region gap = new Region();
                HBox.setHgrow(gap, Priority.ALWAYS);
                HBox row = new HBox(8, dot, new VBox(1, name, info), gap, focused, showing);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                setText(null);
            }
        });
        stageList.getSelectionModel().selectedItemProperty().addListener((o, was, now) -> {
            if (!selecting) {
                selectStage(now);
            }
        });

        Button show = stageButton("Show", s -> s.show());
        Button hide = stageButton("Hide / close", s -> {
            if (s.isShowing()) {
                s.getProperties().put("demo.hideOnly", true);
                s.hide();
            } else {
                removeStage(s);
            }
        });
        hide.setTooltip(new Tooltip("Hide the stage, or forget it when it is already hidden"));
        Button front = stageButton("To front", Stage::toFront);
        Button back = stageButton("To back", Stage::toBack);
        Button focus = stageButton("Focus", Stage::requestFocus);
        Button size = stageButton("Size to scene", Stage::sizeToScene);
        Button center = stageButton("Center", Stage::centerOnScreen);
        FlowPane ops = new FlowPane(8, 8, show, hide, front, back, focus, size, center);
        return card("Stages", stageList, ops);
    }

    private static Label badge(String text, boolean on) {
        Label l = new Label(text);
        l.getStyleClass().add("badge");
        if (!on) {
            l.getStyleClass().add("off");
        }
        return l;
    }

    private Button stageButton(String text, Consumer<Stage> action) {
        Button b = new Button(text);
        b.setOnAction(e -> {
            if (current != null) {
                action.accept(current);
                stageList.refresh();
            }
        });
        stageButtons.add(b);
        return b;
    }

    private Node dialogsCard() {
        FlowPane flow = new FlowPane(8, 8,
                action("Alert", () -> alert(false)),
                action("Alert · window modal", () -> alert(true)),
                action("Open", () -> openFile(false, false)),
                action("Open multiple", () -> openFile(true, false)),
                action("Open filtered", () -> openFile(false, true)),
                action("Save", () -> saveFile(false)),
                action("Save filtered", () -> saveFile(true)),
                action("Folder", this::chooseFolder),
                action("Stage icon", this::chooseIcon));
        return card("Dialogs for the selected stage", flow);
    }

    private static Button action(String text, Runnable r) {
        Button b = new Button(text);
        b.setOnAction(e -> r.run());
        return b;
    }

    private Node propertiesCard() {
        selectedTitle.getStyleClass().add("title");
        ScrollPane stageScroll = new ScrollPane(stageProps);
        stageScroll.setFitToWidth(true);
        stageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(stageScroll, Priority.ALWAYS);
        Label sceneLabel = new Label("SCENE");
        sceneLabel.getStyleClass().add("section");
        VBox box = card("Properties", selectedTitle, stageScroll, sceneLabel, sceneProps);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private Node logCard() {
        logView.setPrefHeight(170);
        logView.setPlaceholder(muted("Events of the selected stage appear here"));
        logView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty ? null : s);
                getStyleClass().add("mono");
                setStyle("-fx-padding: 1 8 1 8; -fx-font-size: 11px;");
            }
        });
        Button clear = new Button("Clear");
        clear.setOnAction(e -> {
            if (current != null) {
                logs.get(current).clear();
            }
        });
        return card("Event log", logView, clear);
    }

    private void selectStage(Stage s) {
        current = s;
        for (Button b : stageButtons) {
            b.setDisable(s == null);
        }
        stageProps.clear();
        sceneProps.clear();
        selectedTitle.textProperty().unbind();
        if (s == null) {
            selectedTitle.setText("No stage selected");
            logView.setItems(FXCollections.observableArrayList());
            return;
        }
        selectedTitle.textProperty().bind(s.titleProperty());
        logView.setItems(logs.get(s));
        if (stageList.getSelectionModel().getSelectedItem() != s) {
            selecting = true;
            try {
                stageList.getSelectionModel().select(s);
            } finally {
                selecting = false;
            }
        }
        stageProps.addString("Title", s.titleProperty(), s::setTitle);
        stageProps.addBoolean("Always on top", s.alwaysOnTopProperty(), s::setAlwaysOnTop);
        stageProps.addBoolean("Full screen", s.fullScreenProperty(), s::setFullScreen);
        stageProps.addBoolean("Maximized", s.maximizedProperty(), s::setMaximized);
        stageProps.addBoolean("Iconified", s.iconifiedProperty(), s::setIconified);
        stageProps.addBoolean("Resizable", s.resizableProperty(), s::setResizable);
        stageProps.addBoolean("Focused", s.focusedProperty(), null);
        stageProps.addDouble("X", s.xProperty(), s::setX, -MAX_WIDTH, MAX_WIDTH * 2, 1);
        stageProps.addDouble("Y", s.yProperty(), s::setY, -MAX_HEIGHT, MAX_HEIGHT * 2, 1);
        stageProps.addDouble("Width", s.widthProperty(), s::setWidth, 1, MAX_WIDTH, 1);
        stageProps.addDouble("Height", s.heightProperty(), s::setHeight, 1, MAX_HEIGHT, 1);
        stageProps.addDouble("Min width", s.minWidthProperty(), s::setMinWidth, 0, MAX_WIDTH, 1);
        stageProps.addDouble("Min height", s.minHeightProperty(), s::setMinHeight, 0, MAX_HEIGHT, 1);
        stageProps.addDouble("Max width", s.maxWidthProperty(), s::setMaxWidth, 1, Double.MAX_VALUE, 1);
        stageProps.addDouble("Max height", s.maxHeightProperty(), s::setMaxHeight, 1, Double.MAX_VALUE, 1);
        stageProps.addDouble("Render scale X", s.renderScaleXProperty(), s::setRenderScaleX, 0.25, 4, 0.25);
        stageProps.addDouble("Render scale Y", s.renderScaleYProperty(), s::setRenderScaleY, 0.25, 4, 0.25);
        stageProps.addDouble("Opacity", s.opacityProperty(), s::setOpacity, 0, 1, 0.1);
        bindScene(s);
    }

    private void bindScene(Stage s) {
        Scene scene = s.getScene();
        if (scene == null) {
            return;
        }
        ComboBox<FillChoice> fill = new ComboBox<>(FXCollections.observableArrayList(FillChoice.values()));
        fill.setValue((FillChoice) s.getProperties().getOrDefault("demo.fill", FillChoice.TRANSPARENT));
        fill.setOnAction(e -> {
            scene.setFill(fill.getValue().paint);
            s.getProperties().put("demo.fill", fill.getValue());
        });
        ComboBox<ColorScheme> scheme = new ComboBox<>(FXCollections.observableArrayList(ColorScheme.values()));
        scheme.setValue(scene.getPreferences().getColorScheme());
        scheme.setOnAction(e -> scene.getPreferences().setColorScheme(scheme.getValue()));
        ComboBox<Content> content = new ComboBox<>(FXCollections.observableArrayList(Content.values()));
        content.setValue((Content) s.getProperties().getOrDefault("demo.content", Content.INFO));
        content.setOnAction(e -> setContent(s, content.getValue()));
        sceneProps.addNode("Fill", fill);
        sceneProps.addNode("Color scheme", scheme);
        sceneProps.addNode("Content", content);
        sceneProps.addLabel("Size", Bindings.format(Locale.ROOT, "%.0f × %.0f at %.0f, %.0f",
                scene.widthProperty(), scene.heightProperty(), scene.xProperty(), scene.yProperty()));
        sceneProps.addLabel("Backdrop", new SimpleStringProperty(backdropName(s)));
    }

    private static String backdropName(Stage s) {
        Object b = s.getProperties().get("demo.backdrop");
        return b == null ? "no backdrop" : b.toString();
    }

    private Window ownerForDialogs() {
        return current != null && current.isShowing() ? current : commandStage;
    }

    private void createStage(boolean show) {
        createStage(styleBox.getValue(), backdropBox.getValue(), show);
    }

    private Stage createStage(StageStyle style, BackdropChoice backdrop, boolean show) {
        Stage s = new Stage();
        counter++;
        s.initStyle(style);
        s.initModality(modalityBox.getValue());
        s.initOwner(ownerBox.getValue());
        if (backdrop != null && backdrop.name() != null) {
            s.getProperties().put("demo.backdrop", backdrop.name());
        }
        s.setTitle("Stage " + counter);
        s.setOpacity(opacity.getValue());
        s.getProperties().put("demo.accent", ACCENTS[counter % ACCENTS.length]);
        s.getProperties().put("demo.fill", fillBox.getValue());

        ObservableList<String> log = FXCollections.observableArrayList();
        logs.put(s, log);
        StringProperty lastEvent = new SimpleStringProperty("–");
        lastEvents.put(s, lastEvent);
        s.addEventHandler(Event.ANY, e -> {
            String name = e.getEventType().getName();
            lastEvent.set(name);
            log.addFirst(LocalTime.now().format(CLOCK) + "  " + name);
            if (log.size() > LOG_SIZE) {
                log.removeLast();
            }
        });
        s.focusedProperty().addListener((o, was, now) -> {
            stageList.refresh();
            if (now) {
                selectStage(s);
            }
        });
        s.showingProperty().addListener(o -> stageList.refresh());
        s.setOnHidden(e -> {
            if (s.getProperties().remove("demo.hideOnly") != null) {
                stageList.refresh();
            } else {
                removeStage(s);
            }
        });

        setContent(s, contentBox.getValue());
        s.getScene().getPreferences().setColorScheme(schemeBox.getValue());
        stages.add(s);
        selectStage(s);
        if (show) {
            s.show();
        }
        return s;
    }

    private void removeStage(Stage s) {
        stages.remove(s);
        logs.remove(s);
        lastEvents.remove(s);
        if (current == s) {
            selectStage(stages.isEmpty() ? null : stages.getLast());
        }
    }

    private void backdropGallery() {
        int i = 0;
        for (BackdropChoice choice : backdropChoices) {
            if (choice.name() == null) {
                continue;
            }
            Stage s = createStage(StageStyle.EXTENDED, choice, false);
            s.setTitle(choice.toString());
            s.setWidth(480);
            s.setHeight(440);
            s.show();
            s.setX(40 + (i % 3) * 440);
            s.setY(60 + (i / 3) * 340);
            i++;
        }
    }

    private void setContent(Stage s, Content content) {
        s.getProperties().put("demo.content", content);
        String accent = (String) s.getProperties().get("demo.accent");
        boolean backdrop = s.getProperties().get("demo.backdrop") != null;

        Node body = switch (content) {
            case INFO -> infoContent(s, accent);
            case TEXT -> textContent();
            case TOOLTIP -> tooltipContent(accent);
            case CONTROLS -> controlsContent();
        };
        VBox glass = new VBox(14, body);
        glass.getStyleClass().add("glass");
        glass.setMaxWidth(Double.MAX_VALUE);
        glass.setMinHeight(0);
        VBox.setVgrow(glass, Priority.ALWAYS);

        VBox root = new VBox();
        if (s.getStyle() == StageStyle.EXTENDED) {
            root.getChildren().add(stageHeader(s, accent));
        } else if (s.getStyle() == StageStyle.UNDECORATED || s.getStyle() == StageStyle.TRANSPARENT) {
            root.getChildren().add(windowButtons(s));
            enableDragging(root, s);
        }
        root.getChildren().add(glass);
        root.setPadding(new Insets(s.getStyle() == StageStyle.EXTENDED ? 0 : 14, 14, 14, 14));
        root.setSpacing(10);
        if (s.getStyle() == StageStyle.EXTENDED) {
            VBox.setMargin(glass, new Insets(0, 0, 0, 0));
        }

        StackPane stack = new StackPane();
        if (!backdrop) {
            Region bg = new Region();
            bg.setBackground(new Background(new BackgroundFill(new LinearGradient(0, 0, 1, 1, true,
                    CycleMethod.NO_CYCLE, new Stop(0, Color.web("#0f172a")),
                    new Stop(1, Color.web(accent).deriveColor(0, 1, 0.35, 1))), null, null)));
            if (s.getStyle() == StageStyle.TRANSPARENT) {
                bg.setOpacity(0.55);
            }
            stack.getChildren().add(bg);
        }
        stack.getChildren().add(root);
        setupContextMenu(stack, s);

        Scene old = s.getScene();
        Scene scene = new Scene(stack, old != null ? old.getWidth() : 500, old != null ? old.getHeight() : 480);
        scene.getStylesheets().add(STYLESHEET);
        FillChoice fill = (FillChoice) s.getProperties().getOrDefault("demo.fill", FillChoice.TRANSPARENT);
        scene.setFill(!backdrop && s.getStyle() != StageStyle.TRANSPARENT && fill.paint == Color.TRANSPARENT
                ? Color.web("#0b1120") : fill.paint);
        if (old != null) {
            scene.getPreferences().setColorScheme(old.getPreferences().getColorScheme());
        }
        s.setScene(scene);
        if (s == current) {
            sceneProps.clear();
            bindScene(s);
        }
    }

    private Node stageHeader(Stage s, String accent) {
        Circle dot = new Circle(6, Color.web(accent));
        dot.setEffect(new DropShadow(8, Color.web(accent)));
        Label title = new Label();
        title.textProperty().bind(s.titleProperty());
        title.getStyleClass().add("brand");
        HBox left = new HBox(10, dot, title);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 16));
        HeaderBar.setDragType(left, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar header = new HeaderBar(left, null, null);
        header.setBackground(null);
        header.setMinHeight(46);
        header.setPrefHeight(46);
        HeaderBar.setAlignment(left, Pos.CENTER_LEFT);
        return header;
    }

    private static Node windowButtons(Stage s) {
        Button full = new Button("Full screen");
        full.setOnAction(e -> s.setFullScreen(!s.isFullScreen()));
        Button max = new Button("Maximize");
        max.setOnAction(e -> s.setMaximized(!s.isMaximized()));
        Button min = new Button("Minimize");
        min.setOnAction(e -> s.setIconified(true));
        Button close = new Button("Close");
        close.setOnAction(e -> s.close());
        FlowPane box = new FlowPane(6, 6, full, max, min, close);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private static void enableDragging(Node node, Stage s) {
        double[] offset = new double[2];
        node.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            offset[0] = e.getScreenX() - s.getX();
            offset[1] = e.getScreenY() - s.getY();
        });
        node.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (e.getTarget() == node) {
                s.setX(e.getScreenX() - offset[0]);
                s.setY(e.getScreenY() - offset[1]);
            }
        });
    }

    private Node infoContent(Stage s, String accent) {
        Label hero = new Label();
        hero.textProperty().bind(s.titleProperty());
        hero.getStyleClass().add("hero");
        hero.setTextFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.WHITE), new Stop(1, Color.web(accent))));

        GridPane grid = new GridPane(16, 6);
        int row = 0;
        row = info(grid, row, "Style", new Label(s.getStyle().toString()));
        row = info(grid, row, "Backdrop", new Label(backdropName(s)));
        row = info(grid, row, "Modality", new Label(s.getModality().toString()));
        Window owner = s.getOwner();
        row = info(grid, row, "Owner", new Label(owner instanceof Stage o ? o.getTitle() : "none"));
        row = info(grid, row, "Focused", bound(Bindings.convert(s.focusedProperty())));
        row = info(grid, row, "Size", bound(Bindings.format(Locale.ROOT, "%.0f × %.0f", s.widthProperty(),
                s.heightProperty())));
        row = info(grid, row, "Position", bound(Bindings.format(Locale.ROOT, "%.0f, %.0f", s.xProperty(),
                s.yProperty())));
        StringProperty last = lastEvents.getOrDefault(s, new SimpleStringProperty("–"));
        info(grid, row, "Last event", bound(last));

        Label hint = muted("Right click for scenes and dialogs");
        return new VBox(12, hero, grid, windowButtons(s), hint);
    }

    private static Label bound(javafx.beans.value.ObservableValue<String> value) {
        Label l = new Label();
        l.textProperty().bind(value);
        l.getStyleClass().add("mono");
        return l;
    }

    private static int info(GridPane grid, int row, String key, Label value) {
        Label k = muted(key);
        grid.add(k, 0, row);
        grid.add(value, 1, row);
        return row + 1;
    }

    private static Node textContent() {
        TextField field = new TextField();
        field.setPromptText("Type here — IME, selection and clipboard");
        TextArea area = new TextArea("The quick brown fox jumps over the lazy dog.\nÀ noite, vovô Kowalsky vê o ímã cair "
                + "no pé do pinguim queixoso e vovó põe açúcar no chá de tâmaras do jabuti feliz. 🦊");
        area.setWrapText(true);
        VBox.setVgrow(area, Priority.ALWAYS);
        return new VBox(10, muted("Text input"), field, area);
    }

    private static Node tooltipContent(String accent) {
        StackPane box = new StackPane(new Label("Hover me"));
        box.setMinHeight(160);
        box.setBackground(new Background(new BackgroundFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web(accent)), new Stop(1, Color.web("#6366f1"))), new javafx.scene.layout.CornerRadii(14),
                null)));
        Tooltip.install(box, new Tooltip("The quick brown fox jumps over the lazy dog."));
        VBox.setVgrow(box, Priority.ALWAYS);
        return new VBox(10, muted("Tooltip"), box);
    }

    private static Node controlsContent() {
        Slider slider = new Slider(0, 100, 40);
        ProgressBar progress = new ProgressBar();
        progress.progressProperty().bind(slider.valueProperty().divide(100));
        progress.setMaxWidth(Double.MAX_VALUE);
        ComboBox<String> combo = new ComboBox<>(FXCollections.observableArrayList("Aurora", "Nebula", "Fractals", "Disco"));
        combo.setValue("Nebula");
        ToggleButton toggle = new ToggleButton("Toggle");
        CheckBox check = new CheckBox("Check");
        DatePicker date = new DatePicker();
        ColorPicker color = new ColorPicker(Color.web("#2dd4bf"));
        Spinner<Integer> spinner = new Spinner<>(0, 10, 3);
        HBox row = new HBox(8, combo, toggle, check);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox row2 = new HBox(8, date, color, spinner);
        row2.setAlignment(Pos.CENTER_LEFT);
        return new VBox(12, muted("Controls"), slider, progress, row, row2);
    }

    private void setupContextMenu(Node root, Stage s) {
        ContextMenu menu = new ContextMenu();
        for (Content c : Content.values()) {
            MenuItem item = new MenuItem(c.label);
            item.setOnAction(e -> setContent(s, c));
            menu.getItems().add(item);
        }
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().addAll(
                menuItem("Alert · application modal", () -> alertFor(s, false)),
                menuItem("Alert · window modal", () -> alertFor(s, true)),
                menuItem("Open file", () -> { current = s; openFile(false, false); }),
                menuItem("Save file", () -> { current = s; saveFile(false); }),
                menuItem("Folder", () -> { current = s; chooseFolder(); }));
        root.setOnContextMenuRequested(e -> menu.show(root, e.getScreenX(), e.getScreenY()));
        root.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (menu.isShowing()) {
                menu.hide();
            }
        });
    }

    private static MenuItem menuItem(String text, Runnable r) {
        MenuItem item = new MenuItem(text);
        item.setOnAction(e -> r.run());
        return item;
    }

    private void alert(boolean windowModal) {
        alertFor(current, windowModal);
    }

    private void alertFor(Stage owner, boolean windowModal) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Alert");
        alert.setHeaderText("The quick brown fox jumps over the lazy dog.");
        if (windowModal) {
            alert.initModality(Modality.WINDOW_MODAL);
            alert.initOwner(owner != null && owner.isShowing() ? owner : commandStage);
        }
        style(alert);
        alert.showAndWait();
    }

    private void report(Alert.AlertType type, String text) {
        Alert alert = new Alert(type, text);
        alert.initOwner(ownerForDialogs());
        style(alert);
        alert.showAndWait();
    }

    private static void style(Alert alert) {
        if (!alert.getDialogPane().getStylesheets().contains(STYLESHEET)) {
            alert.getDialogPane().getStylesheets().add(STYLESHEET);
        }
    }

    private void openFile(boolean multiple, boolean filtered) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(multiple ? "Open files" : filtered ? "Open file (filtered)" : "Open file");
        if (filtered) {
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Text files", "*.txt", "*.md"),
                    new FileChooser.ExtensionFilter("Java files", "*.java"),
                    new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                    new FileChooser.ExtensionFilter("All files", "*.*"));
            chooser.setSelectedExtensionFilter(chooser.getExtensionFilters().getFirst());
        } else {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("All files", "*.*"));
        }
        if (multiple) {
            List<File> files = chooser.showOpenMultipleDialog(ownerForDialogs());
            report(files == null ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION,
                    files == null ? "No files selected" : "Files selected: " + files.size());
            return;
        }
        File file = chooser.showOpenDialog(ownerForDialogs());
        FileChooser.ExtensionFilter selected = chooser.getSelectedExtensionFilter();
        report(file == null ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION, file == null ? "No file selected"
                : "File selected: " + file.getAbsolutePath()
                + (filtered ? "\nFilter: " + (selected == null ? "none" : selected.getDescription()) : ""));
    }

    private void saveFile(boolean filtered) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(filtered ? "Save file (filtered)" : "Save file");
        chooser.setInitialFileName(filtered ? "output.txt" : "test.txt");
        if (filtered) {
            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Text files", "*.txt"),
                    new FileChooser.ExtensionFilter("Java files", "*.java"),
                    new FileChooser.ExtensionFilter("All files", "*.*"));
            chooser.setSelectedExtensionFilter(chooser.getExtensionFilters().getFirst());
        }
        File file = chooser.showSaveDialog(ownerForDialogs());
        FileChooser.ExtensionFilter selected = chooser.getSelectedExtensionFilter();
        report(file == null ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION, file == null ? "Save cancelled"
                : "Save target: " + file.getAbsolutePath()
                + (filtered ? "\nFilter: " + (selected == null ? "none" : selected.getDescription()) : ""));
    }

    private void chooseFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select folder");
        File dir = chooser.showDialog(ownerForDialogs());
        report(dir == null ? Alert.AlertType.WARNING : Alert.AlertType.INFORMATION,
                dir == null ? "No folder selected" : "Folder selected: " + dir.getAbsolutePath());
    }

    private void chooseIcon() {
        if (current == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose icon image");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File file = chooser.showOpenDialog(ownerForDialogs());
        if (file != null) {
            current.getIcons().setAll(new Image(file.toURI().toString()));
        }
    }

    private static final class PropertyGrid extends GridPane {
        private final List<Runnable> cleanups = new ArrayList<>();
        private int row;

        PropertyGrid() {
            setHgap(12);
            setVgap(6);
        }

        void clear() {
            cleanups.forEach(Runnable::run);
            cleanups.clear();
            getChildren().clear();
            row = 0;
        }

        private <T> void listen(ReadOnlyProperty<T> property, ChangeListener<T> listener) {
            property.addListener(listener);
            cleanups.add(() -> property.removeListener(listener));
        }

        private void key(String text) {
            Label l = new Label(text);
            l.getStyleClass().add("muted");
            l.setMinWidth(100);
            add(l, 0, row);
            GridPane.setHalignment(l, HPos.RIGHT);
        }

        void addNode(String label, Region node) {
            key(label);
            node.setMaxWidth(Double.MAX_VALUE);
            add(node, 1, row);
            GridPane.setHgrow(node, Priority.ALWAYS);
            row++;
        }

        void addLabel(String label, javafx.beans.value.ObservableValue<String> value) {
            Label l = new Label();
            l.textProperty().bind(value);
            l.getStyleClass().add("mono");
            addNode(label, l);
        }

        void addString(String label, ReadOnlyStringProperty property, Consumer<String> setter) {
            TextField field = new TextField(property.get());
            listen(property, (o, was, now) -> field.setText(now));
            field.setOnAction(e -> setter.accept(field.getText()));
            addNode(label, field);
        }

        void addBoolean(String label, ReadOnlyBooleanProperty property, Consumer<Boolean> setter) {
            CheckBox box = new CheckBox();
            box.setSelected(property.get());
            listen(property, (o, was, now) -> box.setSelected(now));
            if (setter != null) {
                box.setOnAction(e -> setter.accept(box.isSelected()));
            } else {
                box.setDisable(true);
            }
            addNode(label, box);
        }

        void addDouble(String label, ReadOnlyDoubleProperty property, DoubleConsumer setter,
                       double min, double max, double step) {
            Spinner<Double> spinner = new Spinner<>();
            spinner.setEditable(true);
            SpinnerValueFactory.DoubleSpinnerValueFactory factory =
                    new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, property.get(), step);
            spinner.setValueFactory(factory);
            AtomicBoolean updating = new AtomicBoolean();
            listen(property, (o, was, now) -> {
                if (!now.equals(spinner.getValue())) {
                    updating.set(true);
                    try {
                        factory.setValue(now.doubleValue());
                    } finally {
                        updating.set(false);
                    }
                }
            });
            spinner.valueProperty().addListener((o, was, now) -> {
                if (now != null && !now.equals(was) && !updating.get()) {
                    setter.accept(now);
                }
            });
            addNode(label, spinner);
        }
    }
}
