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

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.ColorScheme;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.effect.Glow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
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
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * A laboratory for the system clipboard, after the clipboard manual tests of
 * OpenJFX (ClipboardExtImageTest and the clipboard page of Monkey Tester):
 * copies text from every Unicode plane, HTML, RTF, URLs, files, images and
 * custom formats (strings, bytes and serialized objects), shows what the
 * clipboard holds, including content copied by other applications, and checks
 * that reading an image leaves no temporary files behind.
 * <p>
 * While this application owns the clipboard, JavaFX answers reads from its own
 * cache, so the cross-process check starts a second instance of the demo that
 * reads the clipboard through the platform and reports what it got. Wayland
 * gives the clipboard only to the focused window, so click the reader window
 * if it does not get the focus by itself.
 */
public class ClipboardDemo extends Application {

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
            -fx-accent: #14b8a6;
            -fx-focus-color: #2dd4bf;
            -fx-faint-focus-color: #2dd4bf22;
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
        .big { -fx-font-size: 17px; -fx-font-weight: bold; }
        .pill {
            -fx-background-color: rgba(255, 255, 255, 0.07);
            -fx-background-radius: 999;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 999;
            -fx-padding: 5 12 5 12;
            -fx-alignment: center;
        }
        .pill .label { -fx-font-size: 12px; }
        .key { -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #93a1b8; }
        .detail { -fx-font-size: 11px; -fx-text-fill: #93a1b8; }
        .status { -fx-background-radius: 999; -fx-padding: 1 9 1 9; -fx-font-size: 10px; -fx-font-weight: bold; }
        .status.idle { -fx-background-color: rgba(255, 255, 255, 0.07); -fx-text-fill: #93a1b8; }
        .status.running { -fx-background-color: rgba(20, 184, 166, 0.35); -fx-text-fill: #99f6e4; }
        .status.pass { -fx-background-color: rgba(34, 197, 94, 0.25); -fx-text-fill: #86efac; }
        .status.fail { -fx-background-color: rgba(244, 63, 94, 0.25); -fx-text-fill: #fda4af; }
        .check-box > .box {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 5;
            -fx-border-color: rgba(255, 255, 255, 0.22);
            -fx-border-radius: 5;
        }
        .check-box:selected > .box { -fx-background-color: #14b8a6; -fx-border-color: transparent; }
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
        .button:disabled { -fx-opacity: 0.4; }
        .button.accent {
            -fx-background-color: linear-gradient(to right, #14b8a6, #6366f1);
            -fx-border-color: transparent;
            -fx-text-fill: white;
            -fx-font-weight: bold;
        }
        .button.copy { -fx-min-width: 120; -fx-alignment: center-left; }
        .text-field, .text-area {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.16);
            -fx-border-radius: 9;
            -fx-text-fill: #e8eef7;
        }
        .text-area .content { -fx-background-color: transparent; }
        .list-view { -fx-background-color: transparent; -fx-border-color: transparent; }
        .list-view .list-cell { -fx-background-color: transparent; -fx-text-fill: #e8eef7; }
        .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-bar { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: rgba(255, 255, 255, 0.25); -fx-background-radius: 6; }
        .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 0; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-shape: " "; -fx-padding: 0; }
        """;

    private static final String STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));

    private static final String READER = "--reader";
    private static final String REPORT = "CLIPBOARD ";
    private static final String REPORT_END = "CLIPBOARD-END";
    private static final long READER_TIMEOUT_S = 60;
    private static final int LOG_SIZE = 300;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /** Text from the Basic Multilingual Plane and the supplementary planes, with combining marks. */
    private static final String UNICODE = """
            Latin: Olá, coração, façade, naïve, Ångström
            Greek: Καλημέρα · Cyrillic: Здравствуйте · Hebrew: שלום · Arabic: مرحبا
            CJK: 日本語 中文 한국어 · Thai: สวัสดี · Devanagari: नमस्ते
            Supplementary: 😀 🚀 🧪 𝄞 𝔘𝔫𝔦𝔠𝔬𝔡𝔢 𠜎 𓀀
            Combining: é (e + U+0301), flags 🇧🇷 🇯🇵, family 👨‍👩‍👧
            Controls: tab[\t] and a trailing newline
            """;

    private static final DataFormat CUSTOM_TEXT = format("application/x-jfx-clipboard-demo");
    private static final DataFormat CUSTOM_BYTES = format("application/octet-stream");
    private static final DataFormat CUSTOM_OBJECT = format("application/x-jfx-clipboard-demo-object");
    private static final DataFormat JSON = format("application/json");

    public record Sample(String name, int[] values, double ratio) implements Serializable {
        @Override
        public boolean equals(Object o) {
            return o instanceof Sample s && s.name.equals(name) && Arrays.equals(s.values, values) && s.ratio == ratio;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, Arrays.hashCode(values), ratio);
        }

        @Override
        public String toString() {
            return "Sample[" + name + ", " + Arrays.toString(values) + ", " + ratio + "]";
        }
    }

    private boolean backdropSupported;
    private final ObservableList<String> log = FXCollections.observableArrayList();
    private final ListView<String> logView = new ListView<>(log);
    private final VBox viewer = new VBox(8);
    private final Label viewerTitle = new Label("Clipboard");
    private final CheckBox watch = new CheckBox("Watch for changes");
    private final Label typesPill = new Label("–");
    private final Map<String, Label[]> checks = new LinkedHashMap<>();
    private final Label summary = new Label("Not run");
    private final Button crossProcess = new Button("Check in another process");
    private Set<DataFormat> lastTypes = Set.of();

    @Override
    public void start(Stage stage) {
        if (getParameters().getUnnamed().contains(READER)) {
            startReader(stage);
            return;
        }
        backdropSupported = Boolean.getBoolean("glass.gtk4.backdrop.active");
        stage.initStyle(StageStyle.EXTENDED);

        BorderPane body = new BorderPane();
        body.setTop(header());
        VBox left = column(copyCard(), textCard());
        VBox middle = column(viewerCard());
        VBox right = column(checksCard(), logCard());
        HBox columns = new HBox(14);
        for (VBox c : List.of(left, middle, right)) {
            HBox.setHgrow(c, Priority.ALWAYS);
            c.setPrefWidth(1);
            columns.getChildren().add(c);
        }
        columns.setPadding(new Insets(4, 18, 18, 18));
        body.setCenter(columns);

        StackPane root = new StackPane(backgroundLayer(), body);
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, Math.min(1380, screen.getWidth() * 0.94), Math.min(880, screen.getHeight() * 0.92),
                Color.TRANSPARENT);
        scene.getStylesheets().add(STYLESHEET);
        scene.getPreferences().setColorScheme(ColorScheme.DARK);
        stage.setScene(scene);
        stage.setTitle("Clipboard Lab");
        stage.setMinWidth(1000);
        stage.setMinHeight(620);
        stage.show();

        Timeline poll = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (watch.isSelected() && stage.isFocused()) {
                Set<DataFormat> types = Clipboard.getSystemClipboard().getContentTypes();
                if (!types.equals(lastTypes)) {
                    log("The clipboard changed");
                    readClipboard();
                }
            }
        }));
        poll.setCycleCount(Timeline.INDEFINITE);
        poll.play();
        stage.focusedProperty().addListener((obs, o, focused) -> {
            if (focused && watch.isSelected()) {
                readClipboard();
            }
        });
        readClipboard();
    }

    private static DataFormat format(String mime) {
        // A DataFormat can be created once per mime type
        DataFormat f = DataFormat.lookupMimeType(mime);
        return f != null ? f : new DataFormat(mime);
    }

    private Region backgroundLayer() {
        Region fill = new Region();
        if (backdropSupported) {
            fill.setBackground(new Background(new BackgroundFill(Color.color(0.02, 0.03, 0.06, 0.22), null, null)));
        } else {
            fill.setBackground(new Background(new BackgroundFill(new RadialGradient(0, 0, 0.2, 0.1, 1.2, true,
                    CycleMethod.NO_CYCLE, new Stop(0, Color.web("#0d2a28")), new Stop(0.6, Color.web("#0b1120")),
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
                new Stop(0, Color.web("#14b8a6")), new Stop(1, Color.web("#6366f1"))));
        logo.setEffect(new Glow(0.6));
        Label brand = new Label("Clipboard Lab");
        brand.getStyleClass().add("brand");
        Label sub = muted("text · HTML · RTF · URLs · files · images · custom formats");
        HBox left = new HBox(10, logo, brand, sub);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 18));

        HBox right = new HBox(10, pill(typesPill), pill(new Label(System.getProperty("glass.platform", "default")
                + " glass")));
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setPadding(new Insets(0, 10, 0, 0));

        HeaderBar.setDragType(left, HeaderDragType.DRAGGABLE_SUBTREE);
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
        l.setWrapText(true);
        return l;
    }

    // ---------------------------------------------------------------------
    // Copy

    private Node copyCard() {
        GridPane grid = new GridPane(10, 6);
        int row = 0;
        row = copyRow(grid, row, "Unicode text", "Every Unicode plane, emoji sequences, a tab and a newline",
                c -> c.putString(UNICODE));
        row = copyRow(grid, row, "Large text", "1 MiB of text", c -> c.putString(largeText()));
        row = copyRow(grid, row, "HTML", "text/html with a plain text alternative", c -> {
            c.putHtml("<h3>JavaFX</h3><p><b>Bold</b>, <i>italic</i> and a <a href=\"https://openjfx.io\">link</a></p>");
            c.putString("JavaFX\nBold, italic and a link");
        });
        row = copyRow(grid, row, "RTF", "text/rtf with a plain text alternative", c -> {
            c.putRtf("{\\rtf1\\ansi{\\fonttbl\\f0\\fswiss Helvetica;}\\f0\\pard This is {\\b bold} RTF.\\par}");
            c.putString("This is bold RTF.");
        });
        row = copyRow(grid, row, "URL", "text/uri-list and text", c -> {
            c.putUrl("https://openjdk.org/projects/openjfx/");
            c.putString("https://openjdk.org/projects/openjfx/");
        });
        row = copyRow(grid, row, "Files", "Two files, to paste in a file manager", c -> c.putFiles(sampleFiles()));
        row = copyRow(grid, row, "Image", "A 240 × 160 PNG with transparency", c -> c.putImage(sampleImage()));
        row = copyRow(grid, row, "RGB stripes", "Opaque red, green and blue stripes", c -> c.putImage(stripes()));
        row = copyRow(grid, row, "Custom text", "application/x-jfx-clipboard-demo",
                c -> c.put(CUSTOM_TEXT, "Custom text at " + now()));
        row = copyRow(grid, row, "Bytes", "application/octet-stream from a ByteBuffer",
                c -> c.put(CUSTOM_BYTES, ByteBuffer.wrap(new byte[] {'B', 'U', 'F', 0x09, 0x00, (byte) 0xff})));
        row = copyRow(grid, row, "JSON", "application/json",
                c -> c.put(JSON, "{\"toolkit\": \"JavaFX\", \"glass\": \"GTK 4\", \"planes\": \"😀𝄞\"}"));
        row = copyRow(grid, row, "Object", "A Serializable record in a custom format",
                c -> c.put(CUSTOM_OBJECT, new Sample("sample", new int[] {1, 2, 3}, 0.5)));
        row = copyRow(grid, row, "Everything", "All of the formats at once", this::fillEverything);

        Button clear = new Button("Clear the clipboard");
        clear.setOnAction(e -> {
            Clipboard.getSystemClipboard().clear();
            log("Cleared the clipboard");
            readClipboard();
        });
        ScrollPane scroll = new ScrollPane(grid);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox box = card("Copy", scroll, clear);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private int copyRow(GridPane grid, int row, String name, String detail, Consumer<ClipboardContent> filler) {
        Button button = new Button(name);
        button.getStyleClass().add("copy");
        button.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            filler.accept(content);
            boolean set = Clipboard.getSystemClipboard().setContent(content);
            log("Copied " + name + " as " + formats(content.keySet()) + (set ? "" : " (setContent returned false)"));
            readClipboard();
        });
        Label d = new Label(detail);
        d.getStyleClass().add("detail");
        d.setWrapText(true);
        GridPane.setHgrow(d, Priority.ALWAYS);
        grid.addRow(row, button, d);
        return row + 1;
    }

    private void fillEverything(ClipboardContent c) {
        c.putString(UNICODE);
        c.putHtml("<p>Everything from <b>JavaFX</b> 😀</p>");
        c.putRtf("{\\rtf1\\ansi Everything from {\\b JavaFX}.\\par}");
        c.putUrl("https://openjfx.io/");
        c.putFiles(sampleFiles());
        c.putImage(stripes());
        c.put(CUSTOM_TEXT, "Custom text");
        c.put(CUSTOM_BYTES, ByteBuffer.wrap(new byte[] {1, 2, 3, 0, (byte) 0xfe}));
        c.put(JSON, "{\"everything\": true}");
        c.put(CUSTOM_OBJECT, new Sample("everything", new int[] {4, 5}, 1.5));
    }

    private static String largeText() {
        StringBuilder sb = new StringBuilder(1 << 20);
        int line = 0;
        while (sb.length() < (1 << 20) - 80) {
            sb.append(String.format(Locale.ROOT, "%06d The quick brown fox jumps over the lazy dog. Ação 😀%n",
                    line++));
        }
        return sb.toString();
    }

    private static Image sampleImage() {
        int w = 240, h = 160;
        WritableImage image = new WritableImage(w, h);
        PixelWriter pw = image.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double dx = x - w / 2.0, dy = y - h / 2.0;
                double d = Math.sqrt(dx * dx + dy * dy) / (h / 2.0);
                Color c = Color.web("#14b8a6").interpolate(Color.web("#6366f1"), (double) x / w);
                pw.setColor(x, y, c.deriveColor(0, 1, 1, Math.max(0, 1 - d * d)));
            }
        }
        return image;
    }

    private static Image stripes() {
        int w = 240, h = 120;
        WritableImage image = new WritableImage(w, h);
        PixelWriter pw = image.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pw.setArgb(x, y, x < w / 3 ? 0xffff0000 : x < 2 * w / 3 ? 0xff00ff00 : 0xff0000ff);
            }
        }
        return image;
    }

    private static List<File> sampleFiles() {
        try {
            Path dir = Path.of(System.getProperty("java.io.tmpdir"), "jfx-clipboard-demo");
            Files.createDirectories(dir);
            List<File> files = new ArrayList<>();
            for (String name : List.of("first file.txt", "segundo-ação.txt")) {
                Path file = dir.resolve(name);
                Files.writeString(file, "Copied from the JavaFX Clipboard Lab: " + name + "\n");
                files.add(file.toFile());
            }
            return files;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------------------------------------------------------------------
    // Text controls

    private Node textCard() {
        TextField field = new TextField("Select, Ctrl+C, Ctrl+X, Ctrl+V");
        TextArea area = new TextArea(UNICODE);
        area.setPrefRowCount(4);
        Label hint = muted("The text controls copy and paste through the same clipboard: paste here what "
                + "other applications copied, and copy from here to them.");
        return card("Text controls", field, area, hint);
    }

    // ---------------------------------------------------------------------
    // Viewer

    private Node viewerCard() {
        viewerTitle.getStyleClass().add("big");
        Button read = new Button("Read the clipboard");
        read.getStyleClass().add("accent");
        read.setOnAction(e -> readClipboard());
        watch.setSelected(true);
        HBox tools = new HBox(12, read, watch);
        tools.setAlignment(Pos.CENTER_LEFT);
        ScrollPane scroll = new ScrollPane(viewer);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        Label hint = muted("Copy something in another application (text from a browser or editor, an image, "
                + "files from a file manager) and come back: the viewer reads the clipboard again when the "
                + "window gets the focus. Reading an image must leave no temporary files behind.");
        VBox box = card("What the clipboard holds", viewerTitle, tools, scroll, hint);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private void readClipboard() {
        Clipboard cb = Clipboard.getSystemClipboard();
        Set<DataFormat> types = cb.getContentTypes();
        lastTypes = types;
        typesPill.setText(types.size() + (types.size() == 1 ? " format" : " formats"));
        viewer.getChildren().clear();
        viewerTitle.setText(types.isEmpty() ? "The clipboard is empty" : "Read at " + now());
        if (types.isEmpty()) {
            return;
        }
        viewer.getChildren().add(entry("Formats", formats(types)));
        if (cb.hasString()) {
            viewer.getChildren().add(entry("Text", cb.getString()));
        }
        if (cb.hasHtml()) {
            viewer.getChildren().add(entry("HTML", cb.getHtml()));
        }
        if (cb.hasRtf()) {
            viewer.getChildren().add(entry("RTF", cb.getRtf()));
        }
        if (cb.hasUrl()) {
            viewer.getChildren().add(entry("URL", cb.getUrl()));
        }
        if (cb.hasFiles()) {
            viewer.getChildren().add(entry("Files", cb.getFiles().stream()
                    .map(f -> f.getAbsolutePath() + (f.exists() ? " (" + f.length() + " bytes)" : " (missing)"))
                    .collect(Collectors.joining("\n"))));
        }
        for (DataFormat f : types) {
            if (f == DataFormat.PLAIN_TEXT || f == DataFormat.HTML || f == DataFormat.RTF || f == DataFormat.URL
                    || f == DataFormat.FILES || f == DataFormat.IMAGE) {
                continue;
            }
            viewer.getChildren().add(entry(f.getIdentifiers().iterator().next(), describe(cb.getContent(f))));
        }
        if (cb.hasImage()) {
            Set<String> before = tempFiles();
            long t0 = System.nanoTime();
            Image image = cb.getImage();
            long ms = (System.nanoTime() - t0) / 1_000_000;
            Set<String> leftovers = tempFiles();
            leftovers.removeAll(before);
            if (image == null) {
                viewer.getChildren().add(entry("Image", "advertised, but could not be read"));
            } else {
                ImageView view = new ImageView(image);
                view.setPreserveRatio(true);
                view.setFitWidth(Math.min(320, image.getWidth()));
                viewer.getChildren().addAll(entry("Image", String.format(Locale.ROOT,
                        "%.0f × %.0f, read in %d ms%s", image.getWidth(), image.getHeight(), ms,
                        leftovers.isEmpty() ? ", no temporary files left" : "")), view);
            }
            if (!leftovers.isEmpty()) {
                viewer.getChildren().add(entry("FAILED", "Reading the image left temporary files: " + leftovers));
                log("Reading the image left temporary files: " + leftovers);
            }
        }
    }

    private static Set<String> tempFiles() {
        String[] names = new File(System.getProperty("java.io.tmpdir")).list();
        return names == null ? new java.util.HashSet<>() : new java.util.HashSet<>(Arrays.asList(names));
    }

    private static String describe(Object value) {
        if (value instanceof ByteBuffer b) {
            ByteBuffer copy = b.duplicate().rewind();
            byte[] bytes = new byte[Math.min(copy.remaining(), 64)];
            copy.get(bytes);
            return copy.capacity() + " bytes: " + HexFormat.ofDelimiter(" ").formatHex(bytes)
                    + (copy.capacity() > 64 ? " …" : "");
        }
        if (value instanceof byte[] bytes) {
            return describe(ByteBuffer.wrap(bytes));
        }
        return value == null ? "null" : value.getClass().getSimpleName() + ": " + value;
    }

    private static Node entry(String key, String value) {
        Label k = new Label(key);
        k.getStyleClass().add("key");
        String shown = value == null ? "null" : value.length() > 1200
                ? value.substring(0, 1200) + "… (" + value.length() + " characters)" : value;
        Label v = new Label(shown);
        v.getStyleClass().add("mono");
        v.setWrapText(true);
        return new VBox(2, k, v);
    }

    private static String formats(Set<DataFormat> formats) {
        return formats.stream()
                .map(f -> f.getIdentifiers().iterator().next())
                .sorted()
                .collect(Collectors.joining(", ", "[", "]"));
    }

    // ---------------------------------------------------------------------
    // Cross-process check

    private Node checksCard() {
        crossProcess.getStyleClass().add("accent");
        crossProcess.setOnAction(e -> checkInAnotherProcess());
        summary.setWrapText(true);
        HBox top = new HBox(12, crossProcess, summary);
        top.setAlignment(Pos.CENTER_LEFT);

        GridPane grid = new GridPane(10, 5);
        int row = 0;
        for (String key : List.of("text", "html", "rtf", "url", "files", "image", mime(CUSTOM_TEXT),
                mime(CUSTOM_BYTES), mime(JSON), mime(CUSTOM_OBJECT))) {
            Label status = new Label("IDLE");
            status.getStyleClass().addAll("status", "idle");
            status.setMinWidth(Region.USE_PREF_SIZE);
            Label name = new Label(label(key));
            name.setMinWidth(Region.USE_PREF_SIZE);
            Label detail = new Label();
            detail.getStyleClass().add("detail");
            detail.setWrapText(true);
            GridPane.setHgrow(detail, Priority.ALWAYS);
            grid.addRow(row++, status, name, detail);
            checks.put(key, new Label[] {status, detail});
        }
        Label hint = muted("Copies Everything, starts a second instance of this demo that reads the clipboard "
                + "through the platform and compares what it read with what was copied. Click the reader "
                + "window if it does not get the focus.");
        return card("Cross-process check", top, grid, hint);
    }

    private static String label(String key) {
        return switch (key) {
            case "text" -> "Text";
            case "html" -> "HTML";
            case "rtf" -> "RTF";
            case "url" -> "URL";
            case "files" -> "Files";
            case "image" -> "Image";
            default -> key;
        };
    }

    private void checkInAnotherProcess() {
        List<String> command = readerCommand();
        if (command == null) {
            summary.setText("Cannot find the command line of this process");
            return;
        }
        ClipboardContent content = new ClipboardContent();
        fillEverything(content);
        Clipboard.getSystemClipboard().setContent(content);
        readClipboard();
        // While this process owns the clipboard, it reads back from the JavaFX cache: the expected values
        Map<String, String> expected = report(Clipboard.getSystemClipboard());

        checks.values().forEach(l -> setStatus(l, "running", "WAITING", ""));
        summary.setText("Waiting for the reader…");
        crossProcess.setDisable(true);
        log("Starting the reader: " + String.join(" ", command));
        Thread.ofPlatform().daemon().name("clipboard-reader").start(() -> {
            Map<String, String> actual = new TreeMap<>();
            String error = null;
            try {
                Process process = new ProcessBuilder(command)
                        .redirectError(ProcessBuilder.Redirect.INHERIT)
                        .start();
                try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(),
                        StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (line.equals(REPORT_END)) {
                            break;
                        }
                        if (line.startsWith(REPORT)) {
                            String[] kv = line.substring(REPORT.length()).split(" ", 2);
                            actual.put(kv[0], kv.length > 1 ? kv[1] : "");
                        }
                    }
                }
                if (!process.waitFor(READER_TIMEOUT_S, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    error = "The reader did not finish";
                }
            } catch (IOException | InterruptedException ex) {
                error = ex.toString();
            }
            String failure = error;
            Platform.runLater(() -> showCrossProcess(expected, actual, failure));
        });
    }

    private void showCrossProcess(Map<String, String> expected, Map<String, String> actual, String error) {
        crossProcess.setDisable(false);
        int passed = 0;
        for (Map.Entry<String, Label[]> e : checks.entrySet()) {
            String want = expected.get(e.getKey());
            String got = actual.get(e.getKey());
            boolean ok = want != null && want.equals(got);
            if (ok) {
                passed++;
            }
            setStatus(e.getValue(), ok ? "pass" : "fail", ok ? "PASS" : "FAIL",
                    ok ? got : "expected " + want + ", read " + got);
            log((ok ? "PASS " : "FAIL ") + label(e.getKey()) + (ok ? "" : ": expected " + want + ", read " + got));
        }
        summary.setText(error != null ? error : passed + " of " + checks.size() + " formats match");
        summary.setTextFill(Color.web(error == null && passed == checks.size() ? "#86efac" : "#fda4af"));
    }

    private static void setStatus(Label[] labels, String style, String text, String detail) {
        labels[0].getStyleClass().removeAll("idle", "running", "pass", "fail");
        labels[0].getStyleClass().add(style);
        labels[0].setText(text);
        labels[1].setText(detail);
    }

    /**
     * The command line of this process, with the reader flag as an
     * application argument.
     */
    private static List<String> readerCommand() {
        ProcessHandle.Info info = ProcessHandle.current().info();
        if (info.command().isEmpty() || info.arguments().isEmpty()) {
            return null;
        }
        List<String> command = new ArrayList<>();
        command.add(info.command().get());
        command.addAll(Arrays.asList(info.arguments().get()));
        command.add(READER);
        return command;
    }

    /**
     * A one-line digest of each format, which both processes compute the same
     * way.
     */
    private static Map<String, String> report(Clipboard cb) {
        Map<String, String> report = new TreeMap<>();
        if (cb.hasString()) {
            report.put("text", digest(cb.getString()));
        }
        if (cb.hasHtml()) {
            report.put("html", digest(cb.getHtml()));
        }
        if (cb.hasRtf()) {
            report.put("rtf", digest(cb.getRtf()));
        }
        if (cb.hasUrl()) {
            report.put("url", cb.getUrl());
        }
        if (cb.hasFiles()) {
            report.put("files", cb.getFiles().stream().map(File::getAbsolutePath).sorted()
                    .collect(Collectors.joining("|")));
        }
        if (cb.hasImage()) {
            Image image = cb.getImage();
            report.put("image", image == null ? "unreadable" : imageDigest(image));
        }
        for (DataFormat f : List.of(CUSTOM_TEXT, CUSTOM_BYTES, JSON, CUSTOM_OBJECT)) {
            if (cb.hasContent(f)) {
                Object value = cb.getContent(f);
                report.put(mime(f), value instanceof String s ? digest(s) : describe(value));
            }
        }
        return report;
    }

    private static String mime(DataFormat f) {
        return f.getIdentifiers().iterator().next();
    }

    private static String digest(String s) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return s.length() + " chars, sha256 " + HexFormat.of().formatHex(hash, 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * The size and the colors at the center of the three stripes.
     */
    private static String imageDigest(Image image) {
        PixelReader pr = image.getPixelReader();
        int w = (int) image.getWidth(), h = (int) image.getHeight();
        StringBuilder sb = new StringBuilder(w + "x" + h);
        for (int i = 0; i < 3; i++) {
            sb.append(String.format(" %08x", pr.getArgb(w * (2 * i + 1) / 6, h / 2)));
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // Reader process

    private void startReader(Stage stage) {
        Label title = new Label("Clipboard reader");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        Label state = new Label("Click this window to read the clipboard");
        VBox box = new VBox(10, title, state);
        box.setPadding(new Insets(24));
        box.setAlignment(Pos.CENTER);
        Scene scene = new Scene(box, 420, 140);
        scene.getPreferences().setColorScheme(ColorScheme.DARK);
        stage.setScene(scene);
        stage.setTitle("Clipboard reader");
        boolean[] done = {false};
        Runnable read = () -> {
            if (done[0]) {
                return;
            }
            done[0] = true;
            Map<String, String> report = report(Clipboard.getSystemClipboard());
            report.forEach((k, v) -> System.out.println(REPORT + k + " " + v));
            System.out.println(REPORT_END);
            System.out.flush();
            state.setText("Read " + report.size() + " formats, closing");
            new Timeline(new KeyFrame(Duration.seconds(1), e -> Platform.exit())).play();
        };
        stage.focusedProperty().addListener((obs, o, focused) -> {
            if (focused) {
                // Wayland sends the selection right after the focus
                new Timeline(new KeyFrame(Duration.millis(200), e -> read.run())).play();
            }
        });
        stage.show();
        new Timeline(new KeyFrame(Duration.seconds(READER_TIMEOUT_S), e -> Platform.exit())).play();
    }

    // ---------------------------------------------------------------------
    // Log

    private Node logCard() {
        logView.setPrefHeight(160);
        logView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty ? null : s);
                getStyleClass().add("mono");
                setStyle("-fx-padding: 1 6 1 6; -fx-font-size: 11px;");
            }
        });
        VBox.setVgrow(logView, Priority.ALWAYS);
        Button clear = new Button("Clear");
        clear.setOnAction(e -> log.clear());
        VBox box = card("Log", logView, clear);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
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
