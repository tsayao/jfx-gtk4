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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
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
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A laboratory for drag and drop, after the DnD manual tests of OpenJFX:
 * <ul>
 * <li>a matrix of sources and destinations with each set of transfer modes,
 *     with the modifiers that choose the mode (DndBasic);</li>
 * <li>sources of text, HTML, images, URLs, a custom format and two files whose
 *     contents the destination verifies (DndTestMultiple), dragged to this or
 *     another window or to other applications;</li>
 * <li>drag images from a snapshot or an opaque RGB image
 *     (DndTestDragViewRawImage), with ordinary and extreme offsets (the DnD
 *     page of Monkey Tester);</li>
 * <li>a full press-drag-release gesture, and lists that reorder their items
 *     and move them between each other.</li>
 * </ul>
 */
public class DragDropDemo extends Application {

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
            -fx-accent: #f59e0b;
            -fx-focus-color: #fbbf24;
            -fx-faint-focus-color: #fbbf2422;
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
        .tile {
            -fx-background-radius: 10;
            -fx-border-radius: 10;
            -fx-padding: 7 10 7 10;
            -fx-spacing: 2;
        }
        .tile .title { -fx-font-weight: bold; -fx-font-size: 12px; }
        .tile .line { -fx-font-size: 11px; -fx-text-fill: #cbd5e1; }
        .tile.source {
            -fx-background-color: rgba(34, 197, 94, 0.18);
            -fx-border-color: rgba(34, 197, 94, 0.5);
            -fx-cursor: open-hand;
        }
        .tile.target {
            -fx-background-color: rgba(14, 165, 233, 0.16);
            -fx-border-color: rgba(14, 165, 233, 0.5);
        }
        .tile.target.over { -fx-background-color: rgba(14, 165, 233, 0.34); -fx-border-color: #38bdf8; }
        .tile.target.dropped { -fx-border-color: #fbbf24; -fx-border-width: 2; }
        .chip {
            -fx-background-color: linear-gradient(to bottom right, rgba(245, 158, 11, 0.28), rgba(217, 70, 239, 0.22));
            -fx-background-radius: 12;
            -fx-border-color: rgba(255, 255, 255, 0.22);
            -fx-border-radius: 12;
            -fx-padding: 8 12 8 12;
            -fx-cursor: open-hand;
        }
        .chip:hover { -fx-border-color: #fbbf24; }
        .chip .title { -fx-font-weight: bold; }
        .chip .detail { -fx-font-size: 11px; -fx-text-fill: #cbd5e1; }
        .drop-zone {
            -fx-background-color: rgba(255, 255, 255, 0.04);
            -fx-background-radius: 14;
            -fx-border-color: rgba(255, 255, 255, 0.28);
            -fx-border-style: segments(8, 6);
            -fx-border-width: 2;
            -fx-border-radius: 14;
            -fx-padding: 14;
        }
        .drop-zone.accepting { -fx-background-color: rgba(34, 197, 94, 0.14); -fx-border-color: #22c55e; }
        .drop-zone.rejecting { -fx-background-color: rgba(244, 63, 94, 0.12); -fx-border-color: #f43f5e; }
        .big { -fx-font-size: 17px; -fx-font-weight: bold; }
        .verdict { -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 3 10 3 10; }
        .verdict.pass { -fx-background-color: rgba(34, 197, 94, 0.25); -fx-text-fill: #86efac; }
        .verdict.fail { -fx-background-color: rgba(244, 63, 94, 0.25); -fx-text-fill: #fda4af; }
        .full-target {
            -fx-background-color: rgba(255, 255, 255, 0.05);
            -fx-background-radius: 10;
            -fx-border-color: rgba(255, 255, 255, 0.2);
            -fx-border-radius: 10;
            -fx-padding: 8 12 8 12;
        }
        .full-target.over { -fx-background-color: rgba(217, 70, 239, 0.3); -fx-border-color: #e879f9; }
        .check-box > .box {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 5;
            -fx-border-color: rgba(255, 255, 255, 0.22);
            -fx-border-radius: 5;
        }
        .check-box:selected > .box { -fx-background-color: #f59e0b; -fx-border-color: transparent; }
        .check-box:selected > .box > .mark { -fx-background-color: white; }
        .button {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 9;
            -fx-text-fill: #e8eef7;
            -fx-padding: 6 14 6 14;
        }
        .button:hover { -fx-background-color: rgba(255, 255, 255, 0.15); }
        .combo-box {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.16);
            -fx-border-radius: 9;
        }
        .combo-box .list-cell { -fx-text-fill: #e8eef7; }
        .combo-box-popup .list-view {
            -fx-background-color: rgba(18, 22, 34, 0.96);
            -fx-background-radius: 10;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 10;
        }
        .list-view { -fx-background-color: rgba(255, 255, 255, 0.03); -fx-background-radius: 10; -fx-border-color: transparent; }
        .list-view .list-cell { -fx-background-color: transparent; -fx-text-fill: #e8eef7; -fx-padding: 5 8 5 8; }
        .list-view .list-cell:filled:hover { -fx-background-color: rgba(255, 255, 255, 0.06); -fx-background-radius: 9; }
        .list-view .list-cell:filled:selected { -fx-background-color: rgba(245, 158, 11, 0.22); -fx-background-radius: 9; }
        .list-view .list-cell.drop-before { -fx-border-color: #fbbf24 transparent transparent transparent; -fx-border-width: 2 0 0 0; }
        .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-bar { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: rgba(255, 255, 255, 0.25); -fx-background-radius: 6; }
        .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 0; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-shape: " "; -fx-padding: 0; }
        """;

    private static final String STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));

    private static final DataFormat CUSTOM = new DataFormat("application/x-jfx-dragdrop-demo");
    private static final DataFormat LIST_ITEM = new DataFormat("application/x-jfx-dragdrop-demo-item");
    private static final int LOG_SIZE = 300;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final TransferMode[] COPY = {TransferMode.COPY};
    private static final TransferMode[] MOVE = {TransferMode.MOVE};
    private static final TransferMode[] LINK = {TransferMode.LINK};
    private static final TransferMode[][] MODE_SETS = {
        TransferMode.ANY, TransferMode.COPY_OR_MOVE, COPY, MOVE, LINK
    };

    /** The contents of the files this application dragged, shared by its windows to verify drops. */
    private static final Map<Path, String> draggedFiles = new HashMap<>();
    private static int windows;

    private enum Accept {
        ANY("Any mode", TransferMode.ANY),
        COPY_OR_MOVE("Copy or move", TransferMode.COPY_OR_MOVE),
        COPY("Copy only", DragDropDemo.COPY),
        MOVE("Move only", DragDropDemo.MOVE),
        LINK("Link only", DragDropDemo.LINK),
        NONE("Reject everything", new TransferMode[0]);

        final String label;
        final TransferMode[] modes;

        Accept(String label, TransferMode[] modes) {
            this.label = label;
            this.modes = modes;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private enum DragView {
        SNAPSHOT("Snapshot of the source"),
        STRIPES("Opaque RGB stripes"),
        NONE("None (platform default)");

        final String label;

        DragView(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private static final Map<String, Double> OFFSETS = new java.util.LinkedHashMap<>();
    static {
        OFFSETS.put("At the pointer", null);
        OFFSETS.put("0", 0.0);
        OFFSETS.put("10", 10.0);
        OFFSETS.put("33.3", 33.3);
        OFFSETS.put("100", 100.0);
        OFFSETS.put("NaN", Double.NaN);
        OFFSETS.put("Double.MAX_VALUE", Double.MAX_VALUE);
        OFFSETS.put("Double.MIN_VALUE", Double.MIN_VALUE);
        OFFSETS.put("Infinity", Double.POSITIVE_INFINITY);
    }

    private boolean backdropSupported;
    private final ObservableList<String> log = FXCollections.observableArrayList();
    private final ListView<String> logView = new ListView<>(log);
    private final CheckBox logOver = new CheckBox("DRAG_OVER");
    private final CheckBox logMouse = new CheckBox("Mouse events of the sources");
    private final ComboBox<DragView> dragViewBox = new ComboBox<>(FXCollections.observableArrayList(DragView.values()));
    private final ComboBox<String> offsetBox = new ComboBox<>(FXCollections.observableArrayList(OFFSETS.keySet()));
    private final ComboBox<Accept> acceptBox = new ComboBox<>(FXCollections.observableArrayList(Accept.values()));
    private final Label zoneTitle = new Label("Drop here");
    private final Label zoneDetail = new Label();
    private final VBox received = new VBox(8);
    private final List<Label> sourceResults = new ArrayList<>();
    private final List<Label> targetResults = new ArrayList<>();
    private Node lastDropTarget;
    private TransferMode lastOverMode;
    private int counter;

    @Override
    public void start(Stage stage) {
        backdropSupported = Boolean.getBoolean("glass.gtk4.backdrop.active");
        stage.initStyle(StageStyle.EXTENDED);

        BorderPane body = new BorderPane();
        body.setTop(header());
        VBox left = column(modesCard(), fullDragCard());
        VBox middle = column(formatsCard(), reorderCard());
        VBox right = column(dropCard(), logCard());
        HBox columns = new HBox(14);
        for (VBox c : List.of(left, middle, right)) {
            HBox.setHgrow(c, Priority.ALWAYS);
            c.setPrefWidth(1);
            columns.getChildren().add(c);
        }
        columns.setPadding(new Insets(4, 18, 18, 18));
        ScrollPane scroll = new ScrollPane(columns);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        body.setCenter(scroll);

        StackPane root = new StackPane(backgroundLayer(), body);
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, Math.min(1400, screen.getWidth() * 0.94), Math.min(900, screen.getHeight() * 0.92),
                Color.TRANSPARENT);
        scene.getStylesheets().add(STYLESHEET);
        scene.getPreferences().setColorScheme(ColorScheme.DARK);
        stage.setScene(scene);
        stage.setTitle(windows == 0 ? "Drag & Drop Lab" : "Drag & Drop Lab " + (windows + 1));
        windows++;
        stage.setMinWidth(1000);
        stage.setMinHeight(620);
        stage.show();
        log("Ready: drag between the tiles, to the drop target, to another window or to another application");
    }

    private Region backgroundLayer() {
        Region fill = new Region();
        if (backdropSupported) {
            fill.setBackground(new Background(new BackgroundFill(Color.color(0.02, 0.03, 0.06, 0.22), null, null)));
        } else {
            fill.setBackground(new Background(new BackgroundFill(new RadialGradient(0, 0, 0.2, 0.1, 1.2, true,
                    CycleMethod.NO_CYCLE, new Stop(0, Color.web("#2a1f14")), new Stop(0.6, Color.web("#0b1120")),
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
                new Stop(0, Color.web("#f59e0b")), new Stop(1, Color.web("#d946ef"))));
        logo.setEffect(new Glow(0.6));
        Label brand = new Label("Drag & Drop Lab");
        brand.getStyleClass().add("brand");
        Label sub = muted("transfer modes · formats · drag images · other applications");
        HBox left = new HBox(10, logo, brand, sub);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 18));

        Button another = new Button("New window");
        another.setOnAction(e -> new DragDropDemo().start(new Stage()));
        HBox right = new HBox(10, another, pill(System.getProperty("glass.platform", "default") + " glass"));
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setPadding(new Insets(0, 10, 0, 0));

        HeaderBar.setDragType(left, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar header = new HeaderBar(left, null, right);
        header.setBackground(null);
        header.setMinHeight(52);
        HeaderBar.setAlignment(left, Pos.CENTER_LEFT);
        return header;
    }

    private static HBox pill(String text) {
        HBox box = new HBox(new Label(text));
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
        l.setWrapText(true);
        return l;
    }

    private static Label styled(String text, String style) {
        Label l = new Label(text);
        l.getStyleClass().add(style);
        return l;
    }

    // ---------------------------------------------------------------------
    // Transfer modes: every source against every destination

    private Node modesCard() {
        GridPane grid = new GridPane(8, 8);
        grid.add(styled("SOURCES", "section"), 0, 0);
        grid.add(styled("DESTINATIONS", "section"), 1, 0);
        for (int i = 0; i < MODE_SETS.length; i++) {
            Node source = modeSource(MODE_SETS[i]);
            Node target = modeTarget(MODE_SETS[i]);
            GridPane.setHgrow(source, Priority.ALWAYS);
            GridPane.setHgrow(target, Priority.ALWAYS);
            grid.add(source, 0, i + 1);
            grid.add(target, 1, i + 1);
        }
        Label hint = muted("""
                Drag from a green source to a blue destination; an action is possible when both list it. \
                Drag first without modifiers, then holding Ctrl (COPY), Shift (MOVE) and Ctrl+Shift (LINK) \
                until the drop. A drop must show "Done, accepted MODE" in the source and "Dropped here, \
                proposed MODE" in the destination, with a MODE of both; other combinations must be rejected. \
                Other applications can be the source or the destination as well.""");
        return card("Transfer modes", hint, grid);
    }

    private Node modeSource(TransferMode[] modes) {
        Label result = styled("", "line");
        sourceResults.add(result);
        VBox tile = new VBox(styled(modesText(modes), "title"), result);
        tile.getStyleClass().addAll("tile", "source");
        tile.setMaxWidth(Double.MAX_VALUE);
        tile.setOnDragDetected(e -> {
            Dragboard db = tile.startDragAndDrop(modes);
            ClipboardContent content = new ClipboardContent();
            content.putString(UUID.randomUUID().toString());
            db.setContent(content);
            sourceResults.forEach(l -> l.setText(""));
            targetResults.forEach(l -> l.setText(""));
            result.setText("Dragging");
            log("DRAG_DETECTED source " + modesText(modes));
            e.consume();
        });
        tile.setOnDragDone(e -> {
            String text = "Done, " + (e.isAccepted() ? "accepted" : "rejected")
                    + (e.getTransferMode() != null ? " " + e.getTransferMode() : "");
            result.setText(text);
            log("DRAG_DONE source " + modesText(modes) + ": " + text);
            e.consume();
        });
        logMouseEvents(tile, "source " + modesText(modes));
        return tile;
    }

    private Node modeTarget(TransferMode[] modes) {
        Label available = styled("", "line");
        Label result = styled("", "line");
        targetResults.add(result);
        VBox tile = new VBox(styled(modesText(modes), "title"), available, result);
        tile.getStyleClass().addAll("tile", "target");
        tile.setMaxWidth(Double.MAX_VALUE);
        tile.setOnDragEntered(e -> {
            tile.getStyleClass().add("over");
            e.consume();
        });
        tile.setOnDragOver(e -> {
            e.acceptTransferModes(modes);
            targetResults.forEach(l -> l.setText(""));
            if (e.getGestureSource() == null) {
                sourceResults.forEach(l -> l.setText(""));
            }
            result.setText(e.getTransferMode() != null ? "Proposed " + e.getTransferMode() : "");
            // The modes of the source, which the modifiers may filter
            available.setText("Source offers " + modesText(e.getDragboard().getTransferModes()));
            e.consume();
        });
        tile.setOnDragDropped(e -> {
            lastDropTarget = tile;
            String text = "Dropped here" + (e.getTransferMode() != null ? ", proposed " + e.getTransferMode() : "");
            result.setText(text);
            log("DRAG_DROPPED destination " + modesText(modes) + ": " + text + " from " + source(e));
            e.setDropCompleted(true);
            e.consume();
        });
        tile.setOnDragExited(e -> {
            tile.getStyleClass().remove("over");
            available.setText("");
            if (tile != lastDropTarget) {
                result.setText("");
            }
            e.consume();
        });
        return tile;
    }

    private static String modesText(TransferMode[] modes) {
        return modesText(Set.of(modes));
    }

    private static String modesText(Set<TransferMode> modes) {
        String text = Arrays.stream(TransferMode.values())
                .filter(modes::contains)
                .map(Enum::name)
                .collect(Collectors.joining(" "));
        return text.isEmpty() ? "nothing" : text;
    }

    // ---------------------------------------------------------------------
    // Data formats and drag images

    private Node formatsCard() {
        dragViewBox.setValue(DragView.SNAPSHOT);
        offsetBox.setValue("At the pointer");
        GridPane options = new GridPane(10, 6);
        options.addRow(0, new Label("Drag image"), dragViewBox);
        options.addRow(1, new Label("Image offset"), offsetBox);

        FlowPane chips = new FlowPane(8, 8,
                chip("Text", "text/plain", c -> c.putString("Hello from JavaFX at " + now())),
                chip("HTML", "text/html + text/plain", c -> {
                    c.putHtml("<b>Bold</b> and <i>italic</i> from <a href=\"https://openjfx.io\">JavaFX</a>");
                    c.putString("Bold and italic from JavaFX");
                }),
                chip("Image", "image/png", c -> c.putImage(sampleImage())),
                chip("RGB stripes", "image/png, the drag image too", c -> c.putImage(stripes(240, 240))),
                chip("Two files", "text/uri-list, verified on drop", c -> c.putFiles(sampleFiles())),
                chip("URL", "text/uri-list + text/plain", c -> {
                    c.putUrl("https://openjdk.org/projects/openjfx/");
                    c.putString("https://openjdk.org/projects/openjfx/");
                }),
                chip("Custom", CUSTOM.getIdentifiers().iterator().next(),
                        c -> c.put(CUSTOM, "Custom payload #" + (++counter))),
                chip("Everything", "all of the above", c -> {
                    c.putString("Everything from JavaFX");
                    c.putHtml("<p>Everything from <b>JavaFX</b></p>");
                    c.putImage(sampleImage());
                    c.putFiles(sampleFiles());
                    c.put(CUSTOM, "Custom payload #" + (++counter));
                }));

        Label hint = muted("These sources offer every mode. Drop them on the drop target, on another window, "
                + "on a file manager (Image, Two files) or on a text editor (Text, HTML, URL). The colors of "
                + "the RGB stripes drag image must match the source, red, green and blue, even if its size "
                + "differs. Extreme offsets must not break the drag.");
        return card("Data formats", options, chips, hint);
    }

    private Node chip(String title, String detail, Consumer<ClipboardContent> filler) {
        VBox chip = new VBox(2, styled(title, "title"), styled(detail, "detail"));
        chip.getStyleClass().add("chip");
        chip.setOnDragDetected(e -> {
            Dragboard db = chip.startDragAndDrop(TransferMode.ANY);
            ClipboardContent content = new ClipboardContent();
            filler.accept(content);
            db.setContent(content);
            setDragView(db, chip, e, title.equals("RGB stripes"));
            log("DRAG_DETECTED " + title + ": formats " + formats(content.keySet()) + ", drag image "
                    + dragViewBox.getValue() + ", offset " + offsetBox.getValue());
            e.consume();
        });
        chip.setOnDragDone(e -> {
            log("DRAG_DONE " + title + ": " + (e.getTransferMode() == null ? "not accepted" : e.getTransferMode()));
            e.consume();
        });
        logMouseEvents(chip, "chip " + title);
        return chip;
    }

    private void setDragView(Dragboard db, Node source, MouseEvent e, boolean stripes) {
        DragView view = stripes ? DragView.STRIPES : dragViewBox.getValue();
        Image image = switch (view) {
            case SNAPSHOT -> {
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                yield source.snapshot(params, null);
            }
            case STRIPES -> stripes(240, 240);
            case NONE -> null;
        };
        if (image == null) {
            return;
        }
        Double offset = OFFSETS.get(offsetBox.getValue());
        if (offset == null) {
            boolean snapshot = view == DragView.SNAPSHOT;
            db.setDragView(image, snapshot ? e.getX() : image.getWidth() / 2, snapshot ? e.getY() : image.getHeight() / 2);
        } else {
            db.setDragView(image);
            db.setDragViewOffsetX(offset);
            db.setDragViewOffsetY(offset);
        }
    }

    private static Image sampleImage() {
        int w = 180, h = 120;
        WritableImage image = new WritableImage(w, h);
        PixelWriter pw = image.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double dx = x - w * 0.62, dy = y - h * 0.45;
                boolean sun = dx * dx + dy * dy < 26 * 26;
                Color sky = Color.web("#f59e0b").interpolate(Color.web("#6d28d9"), (double) y / h);
                pw.setColor(x, y, sun ? Color.web("#fde68a") : y > h * 0.72 ? Color.web("#0f172a") : sky);
            }
        }
        return image;
    }

    /**
     * Opaque vertical stripes of pure red, green and blue.
     */
    private static Image stripes(int w, int h) {
        WritableImage image = new WritableImage(w, h);
        PixelWriter pw = image.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pw.setArgb(x, y, x < w * 0.33 ? 0xffff0000 : x < w * 0.66 ? 0xff00ff00 : 0xff0000ff);
            }
        }
        return image;
    }

    private static List<File> sampleFiles() {
        try {
            Path dir = Path.of(System.getProperty("java.io.tmpdir"), "jfx-dragdrop-demo");
            Files.createDirectories(dir);
            List<File> files = new ArrayList<>();
            for (int i = 1; i <= 2; i++) {
                Path file = dir.resolve("dnd-test-file-" + i + ".txt");
                String text = "test_string_" + i + " " + UUID.randomUUID();
                Files.writeString(file, text);
                synchronized (draggedFiles) {
                    draggedFiles.put(file.toAbsolutePath(), text);
                }
                files.add(file.toFile());
            }
            return files;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------------------------------------------------------------------
    // Drop target

    private Node dropCard() {
        acceptBox.setValue(Accept.ANY);
        HBox options = new HBox(10, new Label("Accepts"), acceptBox);
        options.setAlignment(Pos.CENTER_LEFT);

        zoneTitle.getStyleClass().add("big");
        zoneDetail.getStyleClass().add("muted");
        zoneDetail.setWrapText(true);
        received.getChildren().add(muted("Nothing dropped yet. Drops from other applications (files from a "
                + "file manager, text or images from a browser) land here as well."));
        VBox zone = new VBox(8, zoneTitle, zoneDetail, received);
        zone.getStyleClass().add("drop-zone");
        zone.setMinHeight(230);
        VBox.setVgrow(zone, Priority.ALWAYS);

        zone.setOnDragEntered(e -> {
            Dragboard db = e.getDragboard();
            boolean accepts = acceptBox.getValue() != Accept.NONE;
            zone.getStyleClass().add(accepts ? "accepting" : "rejecting");
            zoneTitle.setText(accepts ? "Release to drop" : "This target rejects drops");
            zoneDetail.setText("From " + source(e) + ", offers " + modesText(db.getTransferModes()) + "\n"
                    + formats(db.getContentTypes()));
            log("DRAG_ENTERED from " + source(e) + ", offers " + modesText(db.getTransferModes())
                    + ", formats " + formats(db.getContentTypes()));
            lastOverMode = null;
            e.consume();
        });
        zone.setOnDragOver(e -> {
            if (!e.getDragboard().getContentTypes().isEmpty()) {
                e.acceptTransferModes(acceptBox.getValue().modes);
            }
            TransferMode mode = e.getTransferMode();
            if (logOver.isSelected()) {
                log(String.format(Locale.ROOT, "DRAG_OVER at %.1f, %.1f (screen %.1f, %.1f): %s",
                        e.getX(), e.getY(), e.getScreenX(), e.getScreenY(), mode));
            } else if (mode != lastOverMode) {
                log("DRAG_OVER: proposed " + mode);
            }
            lastOverMode = mode;
            e.consume();
        });
        zone.setOnDragExited(e -> {
            zone.getStyleClass().removeAll("accepting", "rejecting");
            zoneTitle.setText("Drop here");
            log("DRAG_EXITED");
            e.consume();
        });
        zone.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            log("DRAG_DROPPED as " + e.getTransferMode() + " from " + source(e));
            boolean ok = showReceived(db, e.getTransferMode(), source(e));
            e.setDropCompleted(ok);
            e.consume();
        });

        Button clear = new Button("Clear");
        clear.setOnAction(e -> {
            received.getChildren().setAll(muted("Cleared"));
            zoneDetail.setText("");
        });
        VBox box = card("Drop target", options, zone, clear);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    /**
     * Shows the dropped content, and verifies the files this application
     * dragged.
     *
     * @return false when a dragged file does not hold what was written to it
     */
    private boolean showReceived(Dragboard db, TransferMode mode, String source) {
        received.getChildren().clear();
        Label head = new Label("Received " + mode + " from " + source + " at " + now());
        head.setStyle("-fx-font-weight: bold;");
        received.getChildren().add(head);
        boolean ok = true;
        if (db.hasString()) {
            received.getChildren().add(entry("Text", db.getString()));
        }
        if (db.hasHtml()) {
            received.getChildren().add(entry("HTML", db.getHtml()));
        }
        if (db.hasUrl()) {
            received.getChildren().add(entry("URL", db.getUrl()));
        }
        if (db.hasFiles()) {
            List<File> files = db.getFiles();
            received.getChildren().add(entry("Files", files.stream()
                    .map(f -> f.getAbsolutePath() + (f.exists() ? " (" + f.length() + " bytes)" : " (missing)"))
                    .collect(Collectors.joining("\n"))));
            String failure = verifyFiles(files);
            if (failure != null) {
                ok = !failure.startsWith("FAILED");
                Label verdict = new Label(failure);
                verdict.getStyleClass().addAll("verdict", ok ? "pass" : "fail");
                received.getChildren().add(verdict);
                log("Files: " + failure);
            }
        }
        if (db.hasContent(CUSTOM)) {
            received.getChildren().add(entry("Custom", String.valueOf(db.getContent(CUSTOM))));
        }
        if (db.hasImage()) {
            Image image = db.getImage();
            if (image != null) {
                ImageView view = new ImageView(image);
                view.setPreserveRatio(true);
                view.setFitWidth(Math.min(220, image.getWidth()));
                received.getChildren().addAll(entry("Image", String.format(Locale.ROOT, "%.0f × %.0f",
                        image.getWidth(), image.getHeight())), view);
            } else {
                received.getChildren().add(entry("Image", "advertised, but could not be read"));
            }
        }
        if (received.getChildren().size() == 1) {
            received.getChildren().add(entry("Formats", formats(db.getContentTypes())));
        }
        return ok;
    }

    /**
     * Checks the contents of the dropped files that this application wrote.
     *
     * @return the verdict, or null when none of the files was dragged from here
     */
    private static String verifyFiles(List<File> files) {
        int checked = 0;
        for (File f : files) {
            String expected;
            synchronized (draggedFiles) {
                expected = draggedFiles.get(f.toPath().toAbsolutePath());
            }
            if (expected == null) {
                continue;
            }
            checked++;
            try {
                String actual = Files.readString(f.toPath());
                if (!actual.equals(expected)) {
                    return "FAILED: the contents of " + f.getName() + " differ";
                }
            } catch (IOException e) {
                return "FAILED: " + f.getName() + ": " + e.getMessage();
            }
        }
        return checked == 0 ? null : "SUCCESS: " + checked + " of " + files.size() + " files verified";
    }

    private static Node entry(String key, String value) {
        Label k = styled(key, "section");
        k.setMinWidth(54);
        String shown = value == null ? "null" : value.length() > 400 ? value.substring(0, 400) + "…" : value;
        Label v = styled(shown, "mono");
        v.setWrapText(true);
        HBox box = new HBox(10, k, v);
        box.setAlignment(Pos.TOP_LEFT);
        return box;
    }

    private static String source(DragEvent e) {
        Object source = e.getGestureSource();
        if (source == null) {
            return "another application";
        }
        if (source instanceof Node n && n.getScene() != null && e.getSource() instanceof Node t
                && t.getScene() != null && n.getScene().getWindow() != t.getScene().getWindow()) {
            return "another JavaFX window";
        }
        return "this window";
    }

    private static String formats(Set<DataFormat> formats) {
        return formats.stream()
                .map(f -> f.getIdentifiers().iterator().next())
                .sorted()
                .collect(Collectors.joining(", ", "[", "]"));
    }

    // ---------------------------------------------------------------------
    // Full press-drag-release

    private Node fullDragCard() {
        Label source = new Label("Press here and drag");
        source.getStyleClass().addAll("tile", "source");
        source.setOnDragDetected(e -> {
            source.startFullDrag();
            log("startFullDrag()");
            e.consume();
        });
        FlowPane targets = new FlowPane(8, 8);
        for (String name : List.of("One", "Two", "Three")) {
            Label target = new Label(name);
            target.getStyleClass().add("full-target");
            target.setOnMouseDragEntered(e -> target.getStyleClass().add("over"));
            target.setOnMouseDragExited(e -> target.getStyleClass().remove("over"));
            target.setOnMouseDragReleased(e -> {
                target.getStyleClass().remove("over");
                log(String.format(Locale.ROOT, "MOUSE_DRAG_RELEASED on %s at %.1f, %.1f from %s", name,
                        e.getX(), e.getY(), e.getGestureSource() == source ? "the source" : e.getGestureSource()));
            });
            target.addEventHandler(MouseDragEvent.MOUSE_DRAG_ENTERED, e -> log("MOUSE_DRAG_ENTERED " + name));
            targets.getChildren().add(target);
        }
        HBox row = new HBox(12, source, targets);
        row.setAlignment(Pos.CENTER_LEFT);
        Label hint = muted("A full press-drag-release gesture stays inside JavaFX: the targets get "
                + "MOUSE_DRAG events and no platform drag starts.");
        return card("Full drag", row, hint);
    }

    // ---------------------------------------------------------------------
    // Reorderable lists

    private Node reorderCard() {
        ListView<String> todo = reorderList(FXCollections.observableArrayList(
                "Wayland surfaces", "Vulkan swap chain", "Input methods", "Drag and drop", "Touch gestures"));
        ListView<String> done = reorderList(FXCollections.observableArrayList(
                "Clipboard", "File dialogs", "Backdrop blur"));
        VBox a = new VBox(6, styled("To do", "title"), todo);
        VBox b = new VBox(6, styled("Done", "title"), done);
        HBox.setHgrow(a, Priority.ALWAYS);
        HBox.setHgrow(b, Priority.ALWAYS);
        HBox lists = new HBox(10, a, b);
        Label hint = muted("Drag items to reorder them, to move them to the other list or to another "
                + "window, or drop them on the drop target as text.");
        return card("Reorder", lists, hint);
    }

    private ListView<String> reorderList(ObservableList<String> items) {
        ListView<String> list = new ListView<>(items);
        list.setPrefHeight(170);
        list.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item);
                }
            };
            cell.setOnDragDetected(e -> {
                if (cell.isEmpty()) {
                    return;
                }
                Dragboard db = cell.startDragAndDrop(TransferMode.COPY_OR_MOVE);
                ClipboardContent content = new ClipboardContent();
                content.put(LIST_ITEM, cell.getItem());
                content.putString(cell.getItem());
                db.setContent(content);
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                db.setDragView(cell.snapshot(params, null), e.getX(), e.getY());
                e.consume();
            });
            cell.setOnDragOver(e -> {
                if (e.getDragboard().hasContent(LIST_ITEM)) {
                    e.acceptTransferModes(TransferMode.MOVE);
                    if (!cell.getStyleClass().contains("drop-before")) {
                        cell.getStyleClass().add("drop-before");
                    }
                }
                e.consume();
            });
            cell.setOnDragExited(e -> cell.getStyleClass().remove("drop-before"));
            cell.setOnDragDropped(e -> {
                cell.getStyleClass().remove("drop-before");
                int index = cell.isEmpty() ? items.size() : cell.getIndex();
                insert(e, list, items, (String) e.getDragboard().getContent(LIST_ITEM), index);
            });
            cell.setOnDragDone(e -> {
                if (e.getTransferMode() == TransferMode.MOVE && !isInside(e.getGestureTarget(), list)) {
                    items.remove(e.getDragboard().getString());
                    log("Moved \"" + e.getDragboard().getString() + "\" out of the list");
                }
                e.consume();
            });
            return cell;
        });
        // Drops below the last cell
        list.setOnDragOver(e -> {
            if (e.getDragboard().hasContent(LIST_ITEM)) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });
        list.setOnDragDropped(e -> insert(e, list, items, (String) e.getDragboard().getContent(LIST_ITEM),
                items.size()));
        return list;
    }

    private void insert(DragEvent e, ListView<String> list, ObservableList<String> items, String item, int index) {
        if (item == null) {
            e.setDropCompleted(false);
            e.consume();
            return;
        }
        int old = items.indexOf(item);
        boolean local = old >= 0;
        if (local) {
            items.remove(old);
            if (old < index) {
                index--;
            }
        }
        index = Math.max(0, Math.min(index, items.size()));
        items.add(index, item);
        list.getSelectionModel().select(index);
        log((local ? "Reordered \"" : "Received \"") + item + "\" at position " + (index + 1));
        e.setDropCompleted(true);
        e.consume();
    }

    private static boolean isInside(Object target, Node container) {
        for (Node n = target instanceof Node t ? t : null; n != null; n = n.getParent()) {
            if (n == container) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------
    // Log

    private Node logCard() {
        logView.setPrefHeight(220);
        logView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty ? null : s);
                setWrapText(true);
                setPrefWidth(0);
                getStyleClass().add("mono");
                setStyle("-fx-padding: 1 8 1 8; -fx-font-size: 11px;");
            }
        });
        VBox.setVgrow(logView, Priority.ALWAYS);
        Button clear = new Button("Clear");
        clear.setOnAction(e -> log.clear());
        HBox tools = new HBox(12, clear, new Label("Log"), logOver, logMouse);
        tools.setAlignment(Pos.CENTER_LEFT);
        VBox box = card("Event log", logView, tools);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private void logMouseEvents(Node node, String name) {
        node.addEventHandler(MouseEvent.ANY, e -> {
            if (logMouse.isSelected() && e.getEventType() != MouseEvent.MOUSE_MOVED) {
                log(String.format(Locale.ROOT, "%s on %s at %.1f, %.1f (screen %.1f, %.1f)%s", e.getEventType(),
                        name, e.getX(), e.getY(), e.getScreenX(), e.getScreenY(),
                        e.getEventType() == MouseEvent.MOUSE_CLICKED ? " clicks=" + e.getClickCount() : ""));
            }
        });
    }

    private void log(String text) {
        log.add(now() + "  " + text);
        if (log.size() > LOG_SIZE) {
            log.remove(0, log.size() - LOG_SIZE);
        }
        logView.scrollTo(log.size() - 1);
        System.out.println(text);
    }

    private static String now() {
        return LocalTime.now().format(CLOCK);
    }
}
