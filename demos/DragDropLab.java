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

import javafx.application.ColorScheme;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.DragEvent;
import javafx.scene.input.KeyCode;
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
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
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
 *     and move them between each other;</li>
 * <li>a context menu shown when a drag ends, whose items must take clicks
 *     once the drag has given back the pointer;</li>
 * <li>colors dragged as a serialized double[] between swatches and circles
 *     that are sources and destinations at once, a move leaving the source
 *     gray (DragDropColor of the DragDrop toy), and RTF and custom bytes
 *     (DragDropWithControls of the same toy).</li>
 * </ul>
 * Its tests (see TestKit) drag with the robot between the tiles, from the
 * format sources to the drop target, between the lists and between the
 * colors.
 */
public class DragDropLab {

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

    static final String STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));

    private static final DataFormat CUSTOM = new DataFormat("application/x-jfx-dragdrop-demo");
    private static final DataFormat LIST_ITEM = new DataFormat("application/x-jfx-dragdrop-demo-item");
    private static final DataFormat BYTES = new DataFormat("application/x-jfx-dragdrop-demo-bytes");
    // Color is not Serializable, so the colors travel as red, green, blue and opacity
    private static final DataFormat COLOR = new DataFormat("application/x-jfx-dragdrop-demo-color");
    private static final String RTF = "{\\rtf1\\ansi\\uc1{\\colortbl;\\red255\\green0\\blue0;}"
            + "\\uc1\\b\\i FRED\\par rtf\\par text}";
    private static final Color EMPTY = Color.web("#475569");
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
        COPY("Copy only", DragDropLab.COPY),
        MOVE("Move only", DragDropLab.MOVE),
        LINK("Link only", DragDropLab.LINK),
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
    {
        // -Ddragdrop.logMouse=true logs them from the start, to debug the tests
        logMouse.setSelected(Boolean.getBoolean("dragdrop.logMouse"));
    }
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

    // What the tests check, written by the event handlers
    final TestKit.Suite suite = new TestKit.Suite("Drag and drop");
    private final List<Node> sourceTiles = new ArrayList<>();
    private final List<Node> targetTiles = new ArrayList<>();
    private final Map<String, Node> chips = new HashMap<>();
    private final Map<String, String> chipDone = new HashMap<>();
    private final List<Label> fullTargets = new ArrayList<>();
    private Label fullSource;
    private String fullDragResult;
    private Node menuSource;
    private Node menuTarget;
    private ContextMenu dragEndMenu;
    private String menuPicked;
    private ListView<String> todoList;
    private ListView<String> doneList;
    private int drops;
    private String droppedText;
    private Image droppedImage;
    private String droppedFiles;
    private Object droppedCustom;
    private String droppedRtf;
    private byte[] droppedBytes;
    private final List<Shape> swatches = new ArrayList<>();
    private final List<Circle> colorCircles = new ArrayList<>();
    private Label colorText;
    private TransferMode colorDone;

    /**
     * The lab without its header bar, which the tests launcher shows in a tab.
     */
    Region content(Stage stage) {
        backdropSupported = Boolean.getBoolean("glass.gtk4.backdrop.active");
        VBox left = column(modesCard(), fullDragCard(), dragEndMenuCard(), testsCard(stage));
        VBox middle = column(formatsCard(), colorsCard(), reorderCard());
        VBox right = column(dropCard(), logCard());
        HBox columns = new HBox(14);
        for (VBox c : List.of(left, middle, right)) {
            HBox.setHgrow(c, Priority.ALWAYS);
            c.setPrefWidth(1);
            columns.getChildren().add(c);
        }
        columns.setPadding(new Insets(4, 18, 18, 18));
        // Short of room, the cards are cut at the bottom, the header stays
        columns.setMinHeight(0);
        return columns;
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
        sourceTiles.add(tile);
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
        targetTiles.add(tile);
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
        dragViewBox.setValue(DragView.valueOf(System.getProperty("dragdrop.dragView", "SNAPSHOT")));
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
                chip("Image", "image/png", sampleImage, c -> c.putImage(sampleImage)),
                chip("RGB stripes", "image/png, the drag image too", stripesImage,
                        c -> c.putImage(stripesImage)),
                chip("Two files", "text/uri-list, verified on drop", c -> c.putFiles(sampleFiles())),
                chip("URL", "text/uri-list + text/plain", c -> {
                    c.putUrl("https://openjdk.org/projects/openjfx/");
                    c.putString("https://openjdk.org/projects/openjfx/");
                }),
                chip("Custom", CUSTOM.getIdentifiers().iterator().next(),
                        c -> c.put(CUSTOM, "Custom payload #" + (++counter))),
                chip("RTF", "text/rtf + text/plain", c -> {
                    c.putRtf(RTF);
                    c.putString("FRED rtf text");
                }),
                chip("Bytes", "four bytes, serialized", c -> c.put(BYTES, new byte[] {1, 2, 3, 4})),
                chip("Everything", "all of the above", c -> {
                    c.putString("Everything from JavaFX");
                    c.putHtml("<p>Everything from <b>JavaFX</b></p>");
                    c.putImage(sampleImage);
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
        return chip(title, detail, null, filler);
    }

    // A chip with an image shows it, so the dropped image can be compared with the source
    private Node chip(String title, String detail, Image preview, Consumer<ClipboardContent> filler) {
        VBox chip = new VBox(2, styled(title, "title"), styled(detail, "detail"));
        if (preview != null) {
            ImageView thumbnail = new ImageView(preview);
            thumbnail.setPreserveRatio(true);
            thumbnail.setFitHeight(64);
            VBox.setMargin(thumbnail, new Insets(4, 0, 0, 0));
            chip.getChildren().add(thumbnail);
        }
        chip.getStyleClass().add("chip");
        chips.put(title, chip);
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
            chipDone.put(title, e.getTransferMode() == null ? "not accepted" : e.getTransferMode().name());
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
            case STRIPES -> stripesImage;
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

    private final Image sampleImage = sampleImage();
    private final Image stripesImage = stripes(240, 240);

    /**
     * A sunset over mountains and a lake, with a translucent corner so the
     * alpha channel is transferred as well.
     */
    private static Image sampleImage() {
        int w = 240, h = 160;
        Color skyTop = Color.web("#312e81"), skyBottom = Color.web("#f97316");
        Color sun = Color.web("#fde68a"), far = Color.web("#7c3aed"), near = Color.web("#1e1b4b");
        Color water = Color.web("#0f172a");
        WritableImage image = new WritableImage(w, h);
        PixelWriter pw = image.getPixelWriter();
        double horizon = h * 0.68;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // Below the horizon the scene is mirrored and darkened
                boolean below = y > horizon;
                double sy = below ? 2 * horizon - y : y;
                double dx = x - w * 0.6, dy = sy - h * 0.42;
                double farRidge = h * 0.5 + 10 * Math.sin(x * 0.045) + 6 * Math.sin(x * 0.11 + 1);
                double nearRidge = h * 0.6 + 8 * Math.sin(x * 0.07 + 2) + 4 * Math.sin(x * 0.19);
                Color c;
                if (sy > nearRidge) {
                    c = near;
                } else if (sy > farRidge) {
                    c = far;
                } else if (dx * dx + dy * dy < 22 * 22) {
                    c = sun;
                } else {
                    double glow = Math.max(0, 1 - Math.sqrt(dx * dx + dy * dy) / 90);
                    c = skyTop.interpolate(skyBottom, sy / horizon).interpolate(sun, glow * 0.5);
                }
                if (below) {
                    double ripple = 0.5 + 0.1 * Math.sin(y * 1.3 + x * 0.05);
                    c = c.interpolate(water, ripple);
                }
                double corner = Math.hypot(x, y);
                double alpha = corner < 40 ? 0.35 + 0.65 * corner / 40 : 1;
                pw.setColor(x, y, c.deriveColor(0, 1, 1, alpha));
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
        drops++;
        droppedText = db.hasString() ? db.getString() : null;
        droppedImage = db.hasImage() ? db.getImage() : null;
        droppedFiles = db.hasFiles() ? verifyFiles(db.getFiles()) : null;
        droppedCustom = db.hasContent(CUSTOM) ? db.getContent(CUSTOM) : null;
        droppedRtf = db.hasRtf() ? db.getRtf() : null;
        droppedBytes = deserialize(db.getContent(BYTES), byte[].class);
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
        if (db.hasRtf()) {
            received.getChildren().add(entry("RTF", db.getRtf()));
        }
        if (droppedBytes != null) {
            received.getChildren().add(entry("Bytes", Arrays.toString(droppedBytes)));
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

    /**
     * Dropped content of a serializable type. JavaFX deserializes only strings,
     * unless -Djavafx.dnd.deserializeObjectGraph=true, and otherwise hands over
     * the serialized bytes, which are read here allowing just that type.
     */
    private static <T> T deserialize(Object content, Class<T> type) {
        if (type.isInstance(content)) {
            return type.cast(content);
        }
        if (!(content instanceof ByteBuffer buffer)) {
            return null;
        }
        ByteBuffer copy = buffer.duplicate().rewind();
        byte[] bytes = new byte[copy.remaining()];
        copy.get(bytes);
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            in.setObjectInputFilter(info -> info.serialClass() == null ? ObjectInputFilter.Status.UNDECIDED
                    : info.serialClass() == type ? ObjectInputFilter.Status.ALLOWED
                    : ObjectInputFilter.Status.REJECTED);
            Object value = in.readObject();
            return type.isInstance(value) ? type.cast(value) : null;
        } catch (IOException | ClassNotFoundException e) {
            return null;
        }
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
        fullSource = source;
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
            fullTargets.add(target);
            target.setOnMouseDragReleased(e -> {
                target.getStyleClass().remove("over");
                fullDragResult = name + (e.getGestureSource() == source ? " from the source" : "");
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
    // Colors, after DragDropColor of the DragDrop toy

    private Node colorsCard() {
        HBox sources = new HBox(8);
        for (Color c : List.of(Color.web("#ef4444"), Color.web("#22c55e"), Color.web("#3b82f6"))) {
            Rectangle swatch = new Rectangle(34, 34, c);
            swatch.setArcWidth(10);
            swatch.setArcHeight(10);
            colorSource(swatch, TransferMode.COPY);
            swatches.add(swatch);
            sources.getChildren().add(swatch);
        }
        HBox circles = new HBox(12);
        for (int i = 0; i < 2; i++) {
            Circle circle = new Circle(30, EMPTY);
            circle.setStroke(Color.rgb(255, 255, 255, 0.3));
            colorSource(circle, TransferMode.COPY_OR_MOVE);
            colorTarget(circle);
            colorCircles.add(circle);
            circles.getChildren().add(circle);
        }
        colorText = new Label("Text drop target");
        colorText.getStyleClass().add("full-target");
        colorText.setOnDragOver(e -> {
            if (e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            e.consume();
        });
        colorText.setOnDragDropped(e -> {
            colorText.setText(e.getDragboard().getString());
            e.setDropCompleted(e.getDragboard().hasString());
            e.consume();
        });
        HBox row = new HBox(16, sources, circles, colorText);
        row.setAlignment(Pos.CENTER_LEFT);
        Button clear = new Button("Clear");
        clear.setOnAction(e -> {
            colorCircles.forEach(c -> c.setFill(EMPTY));
            colorText.setText("Text drop target");
        });
        Label hint = muted("The swatches copy their color; the circles copy or move it (Shift), and a moved "
                + "color leaves its circle gray. The color travels as a serialized double[], and as text "
                + "for the text target and other applications.");
        return card("Colors", row, hint, clear);
    }

    private void colorSource(Shape shape, TransferMode... modes) {
        shape.setOnDragDetected(e -> {
            if (!(shape.getFill() instanceof Color c) || c.equals(EMPTY)) {
                return;
            }
            Dragboard db = shape.startDragAndDrop(modes);
            ClipboardContent content = new ClipboardContent();
            content.putString(c.toString());
            content.put(COLOR, new double[] {c.getRed(), c.getGreen(), c.getBlue(), c.getOpacity()});
            db.setContent(content);
            log("DRAG_DETECTED color " + c);
            e.consume();
        });
        shape.setOnDragDone(e -> {
            colorDone = e.getTransferMode();
            if (e.getTransferMode() == TransferMode.MOVE) {
                shape.setFill(EMPTY);
            }
            log("DRAG_DONE color: " + (e.getTransferMode() == null ? "not accepted" : e.getTransferMode()));
            e.consume();
        });
        logMouseEvents(shape, "color");
    }

    private void colorTarget(Circle circle) {
        circle.setOnDragOver(e -> {
            if (e.getGestureSource() != circle && e.getDragboard().hasContent(COLOR)) {
                e.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            e.consume();
        });
        circle.setOnDragDropped(e -> {
            boolean ok = false;
            double[] a = deserialize(e.getDragboard().getContent(COLOR), double[].class);
            if (a != null && a.length == 4) {
                circle.setFill(new Color(a[0], a[1], a[2], a[3]));
                ok = true;
            }
            e.setDropCompleted(ok);
            e.consume();
        });
    }

    // ---------------------------------------------------------------------
    // A context menu when a drag ends

    private Node dragEndMenuCard() {
        Label result = styled("", "line");
        dragEndMenu = new ContextMenu();
        for (String name : List.of("Copy here", "Move here", "Cancel")) {
            MenuItem item = new MenuItem(name);
            item.setId(name.replace(" ", "-").toLowerCase(Locale.ROOT));
            item.setOnAction(e -> {
                menuPicked = name;
                result.setText("Picked \"" + name + "\"");
                log("Menu item \"" + name + "\" after the drag");
            });
            dragEndMenu.getItems().add(item);
        }
        VBox source = new VBox(styled("Drag me", "title"), result);
        source.getStyleClass().addAll("tile", "source");
        menuSource = source;
        Label target = new Label("Drop here");
        target.getStyleClass().addAll("tile", "target");
        menuTarget = target;
        source.setOnDragDetected(e -> {
            Dragboard db = source.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            content.putString("A menu opens when this drag ends");
            db.setContent(content);
            result.setText("Dragging");
            e.consume();
        });
        target.setOnDragOver(e -> {
            if (e.getGestureSource() == source) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        target.setOnDragDropped(e -> {
            e.setDropCompleted(true);
            e.consume();
        });
        // Shown from DRAG_DONE, while the platform may still be ending the drag
        source.setOnDragDone(e -> {
            result.setText("Done, " + (e.isAccepted() ? "accepted" : "rejected") + ": pick an item");
            log("DRAG_DONE menu source: showing the context menu");
            dragEndMenu.show(target, Side.BOTTOM, 0, 4);
            e.consume();
        });
        HBox row = new HBox(12, source, target);
        row.setAlignment(Pos.CENTER_LEFT);
        Label hint = muted("When the drag ends, the source shows a context menu under the destination. Its "
                + "items must highlight under the pointer and take clicks, as the drag no longer holds the "
                + "pointer.");
        return card("Menu after a drag", row, hint);
    }

    // ---------------------------------------------------------------------
    // Reorderable lists

    private Node reorderCard() {
        ListView<String> todo = reorderList(FXCollections.observableArrayList(
                "Wayland surfaces", "Vulkan swap chain", "Input methods", "Drag and drop", "Touch gestures"));
        ListView<String> done = reorderList(FXCollections.observableArrayList(
                "Clipboard", "File dialogs", "Backdrop blur"));
        todoList = todo;
        doneList = done;
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
        list.setMinHeight(60);
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
    // Tests

    private Node testsCard(Stage stage) {
        defineTests();
        suite.setBeforeRun(() -> {
            stage.toFront();
            stage.requestFocus();
        });
        suite.setAfterRobotFailure(this::resetOptions);
        return TestKit.grow(card("Tests", suite.view()));
    }

    private void resetOptions() {
        acceptBox.setValue(Accept.ANY);
        dragViewBox.setValue(DragView.SNAPSHOT);
        offsetBox.setValue("At the pointer");
    }

    private void defineTests() {
        suite.robot("Copy", "Drags the COPY source to the COPY destination", () -> modes(2, 2, "COPY"));
        suite.robot("Move", "Drags the MOVE source to the MOVE destination", () -> modes(3, 3, "MOVE"));
        suite.robot("Link", "Drags the LINK source to the LINK destination", () -> {
            if ("Gtk4".equalsIgnoreCase(System.getProperty("glass.platform"))) {
                // Wayland drags copy, move or ask; GTK offers LINK as COPY
                TestKit.skip("Wayland has no link action for drag and drop");
            }
            return modes(4, 4, "LINK");
        });
        suite.robot("Incompatible modes", "Drags the COPY source to the MOVE destination, which must reject it",
                () -> modes(2, 3, null));
        suite.robot("Ctrl chooses COPY", "Drags ANY to ANY holding Ctrl", () -> modes(0, 0, "COPY", KeyCode.CONTROL));
        suite.robot("Shift chooses MOVE", "Drags ANY to ANY holding Shift", () -> modes(0, 0, "MOVE", KeyCode.SHIFT));
        suite.robot("Text", "Drops the Text source on the drop target", () -> {
            dropChip("Text");
            String text = fx(() -> droppedText);
            check(text != null && text.startsWith("Hello from JavaFX"), "the dropped text is " + text);
            return "\"" + text + "\"";
        });
        suite.robot("Image", "Drops the Image source and compares its pixels", () -> {
            dropChip("Image");
            Image image = fx(() -> droppedImage);
            check(image != null, "no image was dropped");
            check(image.getWidth() == sampleImage.getWidth() && image.getHeight() == sampleImage.getHeight(),
                    "the image is " + image.getWidth() + " × " + image.getHeight());
            for (int[] p : new int[][] {{120, 40}, {150, 70}, {60, 140}, {5, 5}}) {
                int want = sampleImage.getPixelReader().getArgb(p[0], p[1]);
                int got = image.getPixelReader().getArgb(p[0], p[1]);
                check(closeArgb(want, got), String.format(Locale.ROOT, "pixel %d, %d is %08x, expected %08x",
                        p[0], p[1], got, want));
            }
            return String.format(Locale.ROOT, "%.0f × %.0f, pixels and alpha match", image.getWidth(),
                    image.getHeight());
        });
        suite.robot("Files", "Drops the Two files source and verifies the contents of the files", () -> {
            dropChip("Two files");
            String verdict = fx(() -> droppedFiles);
            check(verdict != null && verdict.startsWith("SUCCESS"), "the files verdict is " + verdict);
            return verdict;
        });
        suite.robot("Custom format", "Drops the Custom source, a DataFormat of the application", () -> {
            dropChip("Custom");
            Object value = fx(() -> droppedCustom);
            String want = "Custom payload #" + fx(() -> counter);
            check(want.equals(value), "the custom content is " + value + ", expected " + want);
            return String.valueOf(value);
        });
        suite.robot("RTF", "Drops the RTF source, text/rtf, on the drop target", () -> {
            dropChip("RTF");
            String rtf = fx(() -> droppedRtf);
            check(RTF.equals(rtf), "the dropped RTF is " + rtf);
            return rtf.length() + " characters of RTF";
        });
        suite.robot("Custom bytes", "Drops the Bytes source, a byte[] serialized by JavaFX", () -> {
            dropChip("Bytes");
            byte[] bytes = fx(() -> droppedBytes);
            check(Arrays.equals(bytes, new byte[] {1, 2, 3, 4}), "the dropped bytes are " + Arrays.toString(bytes));
            return Arrays.toString(bytes);
        });
        suite.robot("Color copy", "Drags the red swatch to the first circle, a serialized double[] (DragDropColor)",
                () -> {
            fx(() -> colorCircles.forEach(c -> c.setFill(EMPTY)));
            Color red = fx(() -> (Color) swatches.getFirst().getFill());
            dragColor(swatches.getFirst(), colorCircles.getFirst());
            check(fx(() -> red.equals(colorCircles.getFirst().getFill())), "the circle is "
                    + fx(() -> colorCircles.getFirst().getFill()));
            check(fx(() -> colorDone) == TransferMode.COPY, "the drag ended with " + fx(() -> colorDone));
            return "the circle is " + red;
        });
        suite.robot("Color move", "Drags the first circle to the second holding Shift: the color moves and the "
                + "first turns gray (DragDropColor)", () -> {
            Color green = fx(() -> (Color) swatches.get(1).getFill());
            fx(() -> {
                colorCircles.getFirst().setFill(green);
                colorCircles.get(1).setFill(EMPTY);
            });
            dragColor(colorCircles.getFirst(), colorCircles.get(1), KeyCode.SHIFT);
            await("the first circle gray", () -> EMPTY.equals(colorCircles.getFirst().getFill()));
            check(fx(() -> green.equals(colorCircles.get(1).getFill())), "the second circle is "
                    + fx(() -> colorCircles.get(1).getFill()));
            return "moved " + green;
        });
        suite.robot("Color as text", "Drags the blue swatch to the text target, which takes the string",
                () -> {
            Color blue = fx(() -> (Color) swatches.get(2).getFill());
            dragColor(swatches.get(2), colorText);
            await("the text", () -> colorText.getText().equals(blue.toString()));
            return "\"" + blue + "\"";
        });
        suite.robot("Rejected by the target", "Drops Text on the target that rejects everything", () -> {
            fx(() -> acceptBox.setValue(Accept.NONE));
            int before = fx(() -> drops);
            fx(() -> chipDone.remove("Text"));
            TestKit.drag(chips.get("Text"), received.getParent());
            await("the end of the drag", () -> chipDone.containsKey("Text"));
            fx(() -> acceptBox.setValue(Accept.ANY));
            check("not accepted".equals(fx(() -> chipDone.get("Text"))), "the drag ended with "
                    + fx(() -> chipDone.get("Text")));
            check(fx(() -> drops) == before, "the target received the drop");
            return "not accepted";
        });
        suite.robot("Extreme image offsets", "Drags with the drag image at NaN and Double.MAX_VALUE offsets", () -> {
            try {
                for (String offset : List.of("NaN", "Double.MAX_VALUE")) {
                    fx(() -> offsetBox.setValue(offset));
                    dropChip("Text");
                }
            } finally {
                fx(() -> offsetBox.setValue("At the pointer"));
            }
            return "both drops arrived";
        });
        suite.robot("Full drag", "A press-drag-release from the full drag source to the target Two", () -> {
            fx(() -> fullDragResult = null);
            TestKit.drag(fullSource, fullTargets.get(1));
            await("MOUSE_DRAG_RELEASED", () -> fullDragResult != null);
            String result = fx(() -> fullDragResult);
            check(result.equals("Two from the source"), "released on " + result);
            return "released on " + result;
        });
        suite.robot("Menu after a drag", "Drops on the destination, the source shows a context menu on DRAG_DONE, "
                + "and a click picks its second item", () -> {
            fx(() -> {
                menuPicked = null;
                dragEndMenu.hide();
            });
            try {
                TestKit.drag(menuSource, menuTarget);
                await("the context menu", 5000, () -> dragEndMenu.isShowing());
                TestKit.click(awaitMenuItem(dragEndMenu, "#move-here"));
                await("the item action", () -> menuPicked != null);
                check("Move here".equals(fx(() -> menuPicked)), "picked " + fx(() -> menuPicked));
                await("the menu hidden", () -> !dragEndMenu.isShowing());
            } catch (AssertionError e) {
                fx(() -> dragEndMenu.hide());
                throw e;
            }
            return "picked \"" + fx(() -> menuPicked) + "\"";
        });
        suite.robot("Move between lists", "Drags the first item of To do below the items of Done", () -> {
            String item = fx(() -> todoList.getItems().getFirst());
            Node from = fx(() -> todoList.lookupAll(".list-cell").stream()
                    .filter(n -> n instanceof ListCell<?> c && item.equals(c.getItem())).findFirst().orElseThrow());
            TestKit.reveal(doneList);
            javafx.geometry.Point2D to = fx(() -> {
                javafx.geometry.Bounds b = TestKit.screenBounds(doneList);
                return new javafx.geometry.Point2D(b.getCenterX(), b.getMaxY() - 12);
            });
            TestKit.reveal(from);
            TestKit.drag(fx(() -> TestKit.center(from)), to);
            await("the item in Done", () -> doneList.getItems().contains(item));
            await("the item out of To do", () -> !todoList.getItems().contains(item));
            fx(() -> {
                doneList.getItems().remove(item);
                todoList.getItems().addFirst(item);
            });
            return "\"" + item + "\" moved";
        });
    }

    /**
     * Drags a mode source to a mode destination and checks the action of
     * both, or that the drop is rejected when the action is null.
     */
    private String modes(int source, int target, String action, KeyCode... modifiers) {
        Label done = sourceResults.get(source);
        Label dropped = targetResults.get(target);
        fx(() -> done.setText(""));
        TestKit.drag(sourceTiles.get(source), targetTiles.get(target), modifiers);
        await("the end of the drag", () -> done.getText().startsWith("Done"));
        String doneText = fx(() -> done.getText());
        String droppedText = fx(() -> dropped.getText());
        if (action == null) {
            check(doneText.startsWith("Done, rejected"), "the source shows \"" + doneText + "\"");
            check(!droppedText.startsWith("Dropped"), "the destination shows \"" + droppedText + "\"");
        } else {
            check(doneText.equals("Done, accepted " + action), "the source shows \"" + doneText + "\"");
            check(droppedText.equals("Dropped here, proposed " + action),
                    "the destination shows \"" + droppedText + "\"");
        }
        return doneText;
    }

    /**
     * Drags a format source to the drop target and waits for its drop.
     */
    private void dropChip(String title) {
        int before = fx(() -> drops);
        fx(() -> chipDone.remove(title));
        TestKit.drag(chips.get(title), received.getParent());
        await("the drop of " + title, 5000, () -> drops > before && chipDone.containsKey(title));
        String done = fx(() -> chipDone.get(title));
        check(!"not accepted".equals(done), "the drag of " + title + " was not accepted");
    }

    /**
     * Waits for a laid out node of a showing context menu.
     */
    private static Node awaitMenuItem(ContextMenu menu, String selector) {
        Node[] found = {null};
        await("the node " + selector, () -> {
            Node n = menu.getScene() == null ? null : menu.getScene().getRoot().lookup(selector);
            found[0] = n != null && n.localToScreen(n.getLayoutBounds()) != null ? n : null;
            return found[0] != null;
        });
        TestKit.pause(150);
        return found[0];
    }

    /**
     * Drags a color source to a destination and waits for the end of the drag.
     */
    private void dragColor(Node from, Node to, KeyCode... modifiers) {
        fx(() -> colorDone = null);
        boolean[] done = {false};
        javafx.event.EventHandler<DragEvent> handler = e -> done[0] = true;
        fx(() -> from.addEventHandler(DragEvent.DRAG_DONE, handler));
        try {
            TestKit.drag(from, to, modifiers);
            await("the end of the drag", 5000, () -> done[0]);
        } finally {
            fx(() -> from.removeEventHandler(DragEvent.DRAG_DONE, handler));
        }
    }

    private static boolean closeArgb(int a, int b) {
        for (int shift = 0; shift < 32; shift += 8) {
            if (Math.abs(((a >> shift) & 0xff) - ((b >> shift) & 0xff)) > 3) {
                return false;
            }
        }
        return true;
    }

    private static void fx(Runnable r) {
        TestKit.fx(r);
    }

    private static <T> T fx(java.util.function.Supplier<T> s) {
        return TestKit.fx(s);
    }

    private static void await(String what, java.util.function.BooleanSupplier condition) {
        TestKit.await(what, condition);
    }

    private static void await(String what, long ms, java.util.function.BooleanSupplier condition) {
        TestKit.await(what, ms, condition);
    }

    private static void check(boolean condition, String message) {
        TestKit.check(condition, message);
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
