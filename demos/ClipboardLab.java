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
import javafx.scene.input.KeyCode;
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
 * <p>
 * Its tests (see TestKit) run that check, copy and paste in the text controls
 * with the robot, and exchange text and images with wl-copy and wl-paste of
 * wl-clipboard when they are installed.
 */
public class ClipboardLab {

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
        .status.ok { -fx-background-color: rgba(34, 197, 94, 0.25); -fx-text-fill: #86efac; }
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

    static final String STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));

    private static final String READER = "--reader";
    private static final String REPORT = "CLIPBOARD ";
    private static final String REPORT_END = "CLIPBOARD-END";
    private static final String REPORT_WINDOW = "CLIPBOARD-WINDOW ";
    private static final long READER_TIMEOUT_S = 30;
    private static final int LOG_SIZE = 300;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    /** Text from the Basic Multilingual Plane and the supplementary planes, with combining marks. */
    private static final String UNICODE = """
            Latin: café, résumé, façade, naïve, Ångström
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
    // Marks the copies of the tests, to find them in the list of wl-paste
    private static final DataFormat OWNER = format("application/x-jfx-clipboard-demo-owner");

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
    private final TextField textField = new TextField("Select, Ctrl+C, Ctrl+X, Ctrl+V");
    private final TextArea textArea = new TextArea(UNICODE);
    private Set<DataFormat> lastTypes = Set.of();
    final TestKit.Suite suite = new TestKit.Suite("Clipboard");
    private TestKit.Test crossTest;

    /**
     * Whether the application arguments ask for the reader of the
     * cross-process check.
     */
    static boolean isReader(List<String> args) {
        return args.contains(READER);
    }

    /**
     * The lab without its header bar, which the tests launcher shows in a tab.
     */
    Region content(Stage stage) {
        backdropSupported = Boolean.getBoolean("glass.gtk4.backdrop.active");
        defineTests();
        suite.setBeforeRun(() -> {
            stage.toFront();
            stage.requestFocus();
        });
        VBox left = column(copyCard(), textCard());
        VBox middle = column(viewerCard());
        VBox right = column(testsCard(), checksCard(), logCard());
        HBox columns = new HBox(14);
        for (VBox c : List.of(left, middle, right)) {
            HBox.setHgrow(c, Priority.ALWAYS);
            c.setPrefWidth(1);
            columns.getChildren().add(c);
        }
        columns.setPadding(new Insets(4, 18, 18, 18));
        // Short of room, the cards are cut at the bottom, the header stays
        columns.setMinHeight(0);

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
        return columns;
    }

    private static DataFormat format(String mime) {
        // A DataFormat can be created once per mime type
        DataFormat f = DataFormat.lookupMimeType(mime);
        return f != null ? f : new DataFormat(mime);
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
            sb.append(String.format(Locale.ROOT, "%06d The quick brown fox jumps over the lazy dog. Café 😀%n",
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
            for (String name : List.of("first file.txt", "second-café.txt")) {
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
        TextField field = textField;
        TextArea area = textArea;
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
        crossProcess.disableProperty().bind(TestKit.busyProperty());
        crossProcess.setOnAction(e -> suite.runAsync(List.of(crossTest)));
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
        Label hint = muted("Copies Everything, starts a second instance of the tests that reads the clipboard "
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

    /**
     * Copies Everything, starts the reader and compares what it read, on the
     * test thread.
     */
    private String checkInAnotherProcess() throws Exception {
        List<String> command = readerCommand();
        if (command == null) {
            TestKit.skip("Cannot find the command line of this process");
        }
        claim(this::fillEverything);
        // While this process owns the clipboard, it reads back from the JavaFX cache: the expected values
        Map<String, String> expected = fx(() -> {
            readClipboard();
            checks.values().forEach(l -> setStatus(l, "running", "WAITING", ""));
            summary.setText("Waiting for the reader…");
            log("Starting the reader: " + String.join(" ", command));
            return report(Clipboard.getSystemClipboard());
        });
        Map<String, String> actual = new TreeMap<>();
        ProcessBuilder builder = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT);
        if ("Gtk4".equalsIgnoreCase(System.getProperty("glass.platform"))) {
            // AWT sets GDK_BACKEND=x11 in this process when Swing starts, as in the tests
            // launcher, and the GTK 4 glass of the reader needs Wayland
            builder.environment().put("GDK_BACKEND", "wayland");
        }
        Process process = builder.start();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals(REPORT_END)) {
                    break;
                }
                if (line.startsWith(REPORT_WINDOW) && TestKit.robotInput()) {
                    String[] b = line.substring(REPORT_WINDOW.length()).split(" ");
                    TestKit.pause(400);
                    TestKit.click(new javafx.geometry.Point2D(Double.parseDouble(b[0]) + Double.parseDouble(b[2]) / 2,
                            Double.parseDouble(b[1]) + Double.parseDouble(b[3]) / 2));
                }
                if (line.startsWith(REPORT)) {
                    String[] kv = line.substring(REPORT.length()).split(" ", 2);
                    actual.put(kv[0], kv.length > 1 ? kv[1] : "");
                }
            }
        }
        if (!process.waitFor(READER_TIMEOUT_S, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            fx(() -> summary.setText("The reader did not finish"));
            throw new AssertionError("The reader did not finish");
        }
        List<String> failed = fx(() -> showCrossProcess(expected, actual));
        TestKit.check(failed.isEmpty(), "read differently by the reader: " + String.join(", ", failed)
                + " (it read " + (actual.isEmpty() ? "nothing" : actual.keySet()) + ")");
        return expected.size() + " formats read the same by the reader";
    }

    private List<String> showCrossProcess(Map<String, String> expected, Map<String, String> actual) {
        List<String> failed = new ArrayList<>();
        for (Map.Entry<String, Label[]> e : checks.entrySet()) {
            String want = expected.get(e.getKey());
            String got = actual.get(e.getKey());
            boolean ok = want != null && want.equals(got);
            if (!ok) {
                failed.add(label(e.getKey()));
            }
            setStatus(e.getValue(), ok ? "ok" : "fail", ok ? "OK" : "FAIL",
                    ok ? got : "expected " + want + ", read " + got);
            log((ok ? "OK   " : "FAIL ") + label(e.getKey()) + (ok ? "" : ": expected " + want + ", read " + got));
        }
        int passed = checks.size() - failed.size();
        summary.setText(passed + " of " + checks.size() + " formats match");
        summary.setTextFill(Color.web(failed.isEmpty() ? "#86efac" : "#fda4af"));
        return failed;
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
        // Not the options that run the tests
        Arrays.stream(info.arguments().get()).filter(a -> !a.startsWith("-Dtests.")).forEach(command::add);
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
    // Tests

    private Node testsCard() {
        return TestKit.grow(card("Tests", suite.view()));
    }

    private void defineTests() {
        crossTest = suite.auto("Cross-process", "Copies Everything and reads it in a second process",
                this::checkInAnotherProcess);
        suite.robot("Ctrl+C and Ctrl+V", "Copies the text field with the keyboard and pastes it in the text area",
                () -> {
                    String text = "Robot clipboard " + System.nanoTime() % 100000;
                    fx(() -> textField.setText(text));
                    TestKit.click(textField);
                    await("the text field focused", textField::isFocused);
                    TestKit.shortcut(KeyCode.A, KeyCode.CONTROL);
                    TestKit.shortcut(KeyCode.C, KeyCode.CONTROL);
                    await("the text on the clipboard", () -> text.equals(Clipboard.getSystemClipboard().getString()));
                    fx(() -> textArea.clear());
                    TestKit.click(textArea);
                    await("the text area focused", textArea::isFocused);
                    TestKit.shortcut(KeyCode.V, KeyCode.CONTROL);
                    await("the pasted text", () -> textArea.getText().equals(text));
                    fx(() -> textArea.setText(UNICODE));
                    return "\"" + text + "\"";
                });
        suite.auto("Paste in another application", "Copies Everything and reads it with wl-paste", () -> {
            requireWlClipboard();
            claim(this::fillEverything);
            List<String> types = List.of(new String(run(null, "wl-paste", "--list-types"), StandardCharsets.UTF_8)
                    .split("\n"));
            for (String want : List.of("text/plain;charset=utf-8", "text/html", "text/rtf", "text/uri-list",
                    "image/png", mime(CUSTOM_TEXT), mime(JSON))) {
                TestKit.check(types.contains(want), "wl-paste does not list " + want + ": " + types);
            }
            String text = new String(run(null, "wl-paste", "--no-newline", "--type", "text/plain;charset=utf-8"),
                    StandardCharsets.UTF_8);
            TestKit.check(text.equals(UNICODE), "wl-paste read the text as " + digest(text) + ", expected "
                    + digest(UNICODE));
            Image image = new Image(new java.io.ByteArrayInputStream(run(null, "wl-paste", "--type", "image/png")));
            TestKit.check(!image.isError(), "wl-paste read an image that does not decode");
            TestKit.check(imageDigest(image).equals(imageDigest(stripes())), "wl-paste read the image as "
                    + imageDigest(image) + ", expected " + imageDigest(stripes()));
            String uris = new String(run(null, "wl-paste", "--no-newline", "--type", "text/uri-list"),
                    StandardCharsets.UTF_8);
            TestKit.check(uris.contains("https://openjfx.io/") && uris.contains("file://"),
                    "wl-paste read the URIs as " + uris);
            return types.size() + " types, text, image and URIs match";
        });
        suite.auto("Large text in another application", "Copies 1 MiB of text and reads it with wl-paste", () -> {
            requireWlClipboard();
            String large = largeText();
            claim(c -> c.putString(large));
            String text = new String(run(null, "wl-paste", "--no-newline", "--type", "text/plain;charset=utf-8"),
                    StandardCharsets.UTF_8);
            TestKit.check(text.equals(large), "wl-paste read " + digest(text) + ", expected " + digest(large));
            return digest(text);
        });
        suite.auto("Clear", "Clears the clipboard, which wl-paste must see empty", () -> {
            requireWlClipboard();
            claim(c -> c.putString("Cleared next"));
            fx(() -> Clipboard.getSystemClipboard().clear());
            Process p = new ProcessBuilder("wl-paste", "--list-types").redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            p.waitFor(10, TimeUnit.SECONDS);
            TestKit.check(p.exitValue() != 0, "wl-paste still lists " + out);
            fx(this::readClipboard);
            return out;
        });
        suite.auto("Copy from another application", "Copies text and then an image with wl-copy and reads them",
                () -> {
                    requireWlClipboard();
                    String text = "From wl-copy: Café 😀 " + System.nanoTime() % 100000;
                    Process textCopy = wlCopy(text.getBytes(StandardCharsets.UTF_8), "text/plain;charset=utf-8");
                    try {
                        await("the text of wl-copy", 5000,
                                () -> text.equals(Clipboard.getSystemClipboard().getString()));
                    } finally {
                        textCopy.destroy();
                    }
                    Process imageCopy = wlCopy(pngOf(stripes()), "image/png");
                    try {
                        Set<String> before = tempFiles();
                        await("the image of wl-copy", 5000, () -> Clipboard.getSystemClipboard().hasImage()
                                && !Clipboard.getSystemClipboard().hasString());
                        Image image = fx(() -> Clipboard.getSystemClipboard().getImage());
                        Set<String> leftovers = tempFiles();
                        leftovers.removeAll(before);
                        TestKit.check(image != null, "the image could not be read");
                        TestKit.check(imageDigest(image).equals(imageDigest(stripes())), "the image reads as "
                                + imageDigest(image) + ", expected " + imageDigest(stripes()));
                        TestKit.check(leftovers.isEmpty(), "reading the image left temporary files: " + leftovers);
                        fx(this::readClipboard);
                        return "text and " + imageDigest(image);
                    } finally {
                        // The selection goes away with wl-copy, so that the next copies of this window count
                        imageCopy.destroy();
                    }
                });
    }

    /**
     * Copies content and makes sure that the compositor made it the
     * selection, which wl-paste shows. The compositor keeps the selection of
     * another client that is newer than the last input to this window, so
     * with the robot this clicks the window first, and without it the test is
     * skipped.
     */
    private void claim(Consumer<ClipboardContent> filler) throws Exception {
        Runnable copy = () -> {
            ClipboardContent c = new ClipboardContent();
            filler.accept(c);
            c.put(OWNER, "owner");
            Clipboard.getSystemClipboard().setContent(c);
        };
        awaitFocus();
        fx(copy);
        if (!hasWlClipboard() || ownsSelection()) {
            return;
        }
        if (!TestKit.robotInput()) {
            TestKit.skip("Another application owns the clipboard since the last input to this window, so the "
                    + "compositor ignores the copy: run with the robot, or click the window and run again");
        }
        TestKit.click(viewerTitle);
        awaitFocus();
        fx(copy);
        TestKit.check(ownsSelection(), "the compositor ignores the copy of this window, even after a click");
    }

    private static boolean ownsSelection() throws IOException, InterruptedException {
        Process p = new ProcessBuilder("wl-paste", "--list-types").redirectErrorStream(true).start();
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        p.waitFor(10, TimeUnit.SECONDS);
        return out.lines().anyMatch(mime(OWNER)::equals);
    }

    /**
     * Waits for the focus to come back after a helper window took it: the
     * compositor ignores the clipboard of a window without the focus.
     */
    private void awaitFocus() {
        await("the focus of the window", 5000, () -> textField.getScene() != null
                && textField.getScene().getWindow().isFocused());
    }

    private static void requireWlClipboard() {
        if (!hasWlClipboard()) {
            TestKit.skip("wl-clipboard is not installed (wl-copy and wl-paste)");
        }
    }

    private static boolean hasWlClipboard() {
        for (String dir : System.getenv().getOrDefault("PATH", "").split(File.pathSeparator)) {
            if (Files.isExecutable(Path.of(dir, "wl-paste")) && Files.isExecutable(Path.of(dir, "wl-copy"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Runs a command with the given input and returns its output, failing
     * when it does not exit with 0.
     */
    private static byte[] run(byte[] input, String... command) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        if (input != null) {
            try (var out = p.getOutputStream()) {
                out.write(input);
            }
        } else {
            p.getOutputStream().close();
        }
        byte[] output = p.getInputStream().readAllBytes();
        if (!p.waitFor(10, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new AssertionError(String.join(" ", command) + " did not finish");
        }
        TestKit.check(p.exitValue() == 0, String.join(" ", command) + " exited with " + p.exitValue());
        return output;
    }

    /**
     * Starts wl-copy in the foreground, serving the data until it is
     * destroyed.
     */
    private static Process wlCopy(byte[] data, String mime) throws IOException {
        Process p = new ProcessBuilder("wl-copy", "--foreground", "--type", mime)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        try (var out = p.getOutputStream()) {
            out.write(data);
        }
        return p;
    }

    private static byte[] pngOf(Image image) {
        // A PNG of an opaque image, without java.desktop: stored (uncompressed) deflate blocks
        int w = (int) image.getWidth(), h = (int) image.getHeight();
        java.io.ByteArrayOutputStream raw = new java.io.ByteArrayOutputStream();
        PixelReader pr = image.getPixelReader();
        for (int y = 0; y < h; y++) {
            raw.write(0);
            for (int x = 0; x < w; x++) {
                int argb = pr.getArgb(x, y);
                raw.write(argb >> 16);
                raw.write(argb >> 8);
                raw.write(argb);
            }
        }
        java.util.zip.Deflater deflater = new java.util.zip.Deflater();
        deflater.setInput(raw.toByteArray());
        deflater.finish();
        java.io.ByteArrayOutputStream z = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        while (!deflater.finished()) {
            z.write(buf, 0, deflater.deflate(buf));
        }
        deflater.end();
        ByteBuffer ihdr = ByteBuffer.allocate(13).putInt(w).putInt(h).put((byte) 8).put((byte) 2)
                .put((byte) 0).put((byte) 0).put((byte) 0);
        java.io.ByteArrayOutputStream png = new java.io.ByteArrayOutputStream();
        png.writeBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1a, '\n'});
        chunk(png, "IHDR", ihdr.array());
        chunk(png, "IDAT", z.toByteArray());
        chunk(png, "IEND", new byte[0]);
        return png.toByteArray();
    }

    private static void chunk(java.io.ByteArrayOutputStream out, String type, byte[] data) {
        byte[] t = type.getBytes(StandardCharsets.US_ASCII);
        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(t);
        crc.update(data);
        out.writeBytes(ByteBuffer.allocate(4).putInt(data.length).array());
        out.writeBytes(t);
        out.writeBytes(data);
        out.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
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

    // ---------------------------------------------------------------------
    // Reader process

    void startReader(Stage stage) {
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
        int[] attempts = {0};
        Runnable[] readRef = {null};
        Runnable read = () -> {
            if (done[0]) {
                return;
            }
            // The selection reaches a window some time after its focus: wait for it
            if (Clipboard.getSystemClipboard().getContentTypes().isEmpty() && attempts[0]++ < 20) {
                new Timeline(new KeyFrame(Duration.millis(250), e -> readRef[0].run())).play();
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
        readRef[0] = read;
        stage.focusedProperty().addListener((obs, o, focused) -> {
            if (focused) {
                // Wayland sends the selection right after the focus
                new Timeline(new KeyFrame(Duration.millis(200), e -> read.run())).play();
            }
        });
        stage.show();
        // Where the window is, so that the robot of the test can click it: a
        // new window of another application may not get the focus
        new Timeline(new KeyFrame(Duration.millis(800), e -> {
            System.out.println(String.format(Locale.ROOT, "%s%.0f %.0f %.0f %.0f", REPORT_WINDOW, stage.getX(),
                    stage.getY(), stage.getWidth(), stage.getHeight()));
            System.out.flush();
        })).play();
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
