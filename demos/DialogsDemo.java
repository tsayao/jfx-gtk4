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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.Glow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
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
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
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
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * A laboratory for the common dialogs: the file and folder choosers, which the
 * GTK 4 glass shows through the file chooser portal of the desktop (or a
 * GtkFileDialog with -Dglass.gtk4.portal=false), and the dialogs of JavaFX
 * controls: alerts, a text input dialog and a choice dialog. Its tests (see
 * TestKit) answer the dialogs with the keyboard of the robot: in a file
 * chooser, Ctrl+L and a path, Enter to accept and Escape to cancel.
 */
public class DialogsDemo extends Application {

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
            -fx-accent: #84cc16;
            -fx-focus-color: #a3e635;
            -fx-faint-focus-color: #a3e63522;
            -fx-text-fill: #e8eef7;
        }
        .label { -fx-text-fill: #e8eef7; }
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
        .button {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 9;
            -fx-text-fill: #e8eef7;
            -fx-padding: 6 14 6 14;
        }
        .button:hover { -fx-background-color: rgba(255, 255, 255, 0.15); }
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

    private static final long DIALOG_TIMEOUT_S = 20;
    private static final int LOG_SIZE = 300;
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    final TestKit.Suite suite = new TestKit.Suite("Dialogs");
    private boolean backdropSupported;
    private Stage stage;
    private final ObservableList<String> log = FXCollections.observableArrayList();
    private final ListView<String> logView = new ListView<>(log);
    private final Label result = new Label("No dialog yet");
    private final Button ownerTarget = new Button("Owner button");
    private int ownerClicks;
    private Path dir;

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.EXTENDED);
        BorderPane body = new BorderPane();
        body.setTop(header());
        body.setCenter(content(stage));
        StackPane root = new StackPane(backgroundLayer(), body);
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, Math.min(1200, screen.getWidth() * 0.9), Math.min(820, screen.getHeight() * 0.9),
                Color.TRANSPARENT);
        scene.getStylesheets().add(STYLESHEET);
        scene.getPreferences().setColorScheme(ColorScheme.DARK);
        stage.setScene(scene);
        stage.setTitle("Dialogs Lab");
        stage.show();
        TestKit.autorun(suite);
    }

    /**
     * The lab without its header bar, which the tests launcher shows in a tab.
     */
    Region content(Stage stage) {
        this.stage = stage;
        backdropSupported = Boolean.getBoolean("glass.gtk4.backdrop.active");
        defineTests();
        suite.setBeforeRun(() -> {
            stage.toFront();
            stage.requestFocus();
        });
        VBox left = column(openCard(), logCard());
        VBox right = column(card("Tests", suite.view()));
        HBox columns = new HBox(14, left, right);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setPrefWidth(1);
        right.setPrefWidth(1.3);
        columns.setPadding(new Insets(4, 18, 18, 18));
        javafx.scene.control.ScrollPane scroll = new javafx.scene.control.ScrollPane(columns);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
        return scroll;
    }

    private Region backgroundLayer() {
        Region fill = new Region();
        if (backdropSupported) {
            fill.setBackground(new Background(new BackgroundFill(Color.color(0.02, 0.03, 0.06, 0.22), null, null)));
        } else {
            fill.setBackground(new Background(new BackgroundFill(new RadialGradient(0, 0, 0.2, 0.1, 1.2, true,
                    CycleMethod.NO_CYCLE, new Stop(0, Color.web("#1a2a0c")), new Stop(0.6, Color.web("#0b1120")),
                    new Stop(1, Color.web("#050811"))), null, null)));
        }
        return fill;
    }

    private Node header() {
        Circle logo = new Circle(10, new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#84cc16")), new Stop(1, Color.web("#14b8a6"))));
        logo.setEffect(new Glow(0.6));
        Label brand = new Label("Dialogs Lab");
        brand.getStyleClass().add("brand");
        Label sub = new Label("open · save · folders · filters · alerts · input");
        sub.getStyleClass().add("muted");
        HBox left = new HBox(10, logo, brand, sub);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 18));
        HBox portal = new HBox(new Label("false".equals(System.getProperty("glass.gtk4.portal"))
                ? "GtkFileDialog" : "File chooser portal"));
        portal.getStyleClass().add("pill");
        HBox right = new HBox(10, portal);
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setFillHeight(false);
        right.setPadding(new Insets(0, 10, 0, 0));
        HeaderBar.setDragType(left, HeaderDragType.DRAGGABLE_SUBTREE);
        HeaderBar header = new HeaderBar(left, null, right);
        header.setBackground(null);
        header.setMinHeight(52);
        HeaderBar.setAlignment(left, Pos.CENTER_LEFT);
        return header;
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

    // ---------------------------------------------------------------------
    // Dialogs to open by hand

    private Node openCard() {
        Button open = new Button("Open file");
        open.setOnAction(e -> show("Open", fileChooser().showOpenDialog(stage)));
        Button several = new Button("Open files");
        several.setOnAction(e -> show("Open files", fileChooser().showOpenMultipleDialog(stage)));
        Button save = new Button("Save file");
        save.setOnAction(e -> {
            FileChooser chooser = fileChooser();
            chooser.setInitialFileName("untitled.txt");
            show("Save", chooser.showSaveDialog(stage));
        });
        Button folder = new Button("Choose folder");
        folder.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Choose a folder");
            chooser.setInitialDirectory(testDir().toFile());
            show("Folder", chooser.showDialog(stage));
        });
        Button alert = new Button("Alert");
        alert.setOnAction(e -> show("Alert", new Alert(Alert.AlertType.CONFIRMATION, "Are you sure?")
                .showAndWait().orElse(null)));
        Button input = new Button("Text input");
        input.setOnAction(e -> show("Text input", new TextInputDialog("JavaFX").showAndWait().orElse(null)));
        Button choice = new Button("Choice");
        choice.setOnAction(e -> show("Choice", new ChoiceDialog<>("GTK 4", "GTK 3", "GTK 4", "Wayland")
                .showAndWait().orElse(null)));
        ownerTarget.setOnAction(e -> {
            ownerClicks++;
            ownerTarget.setText("Owner button " + ownerClicks + "×");
        });
        result.getStyleClass().add("mono");
        result.setWrapText(true);
        Label hint = new Label("The file choosers start in a folder of test files. The owner button must not "
                + "react while a dialog is open.");
        hint.getStyleClass().add("muted");
        hint.setWrapText(true);
        return card("Open", new FlowPane(8, 8, open, several, save, folder, alert, input, choice), ownerTarget,
                result, hint);
    }

    private FileChooser fileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose a test file");
        chooser.setInitialDirectory(testDir().toFile());
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("All files", "*"),
                new FileChooser.ExtensionFilter("Text", "*.txt"),
                new FileChooser.ExtensionFilter("Images", "*.png"));
        return chooser;
    }

    private void show(String what, Object value) {
        String text = what + ": " + (value == null ? "cancelled" : value);
        result.setText(text);
        log(text);
    }

    /**
     * A folder of test files, made again for each run.
     */
    private Path testDir() {
        try {
            if (dir == null) {
                dir = Files.createTempDirectory("jfx-dialogs");
                dir.toFile().deleteOnExit();
            }
            for (String name : List.of("alpha.txt", "beta.txt", "gamma.png")) {
                Path f = dir.resolve(name);
                if (!Files.exists(f)) {
                    Files.writeString(f, name);
                    f.toFile().deleteOnExit();
                }
            }
            Path sub = dir.resolve("inner");
            Files.createDirectories(sub);
            sub.toFile().deleteOnExit();
            return dir;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // ---------------------------------------------------------------------
    // Log

    private Node logCard() {
        logView.setPrefHeight(200);
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
        VBox box = card("Log", logView);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    private void log(String text) {
        log.add(LocalTime.now().format(CLOCK) + "  " + text);
        if (log.size() > LOG_SIZE) {
            log.remove(0, log.size() - LOG_SIZE);
        }
        logView.scrollTo(log.size() - 1);
        System.out.println(text);
    }

    // ---------------------------------------------------------------------
    // Tests

    private void defineTests() {
        suite.robot("Open a file", "Opens alpha.txt by typing its path", () -> {
            Path file = testDir().resolve("alpha.txt");
            File chosen = choose(() -> fileChooser().showOpenDialog(stage), () -> typePath(file));
            check(file.toFile().equals(chosen), "the dialog returned " + chosen);
            return chosen.getName();
        });
        suite.robot("Cancel", "Opens the dialog and cancels it with Escape", () -> {
            File chosen = choose(() -> fileChooser().showOpenDialog(stage),
                    () -> TestKit.shortcut(KeyCode.ESCAPE));
            check(chosen == null, "the dialog returned " + chosen);
            return "null";
        });
        suite.robot("Open several files", "Selects every file of a folder with Ctrl+A", () -> {
            // A folder of files only: Enter on a selection with a folder opens the folder
            Path multi = testDir().resolve("inner");
            for (String name : List.of("one.txt", "two.txt")) {
                Path f = multi.resolve(name);
                Files.writeString(f, name);
                f.toFile().deleteOnExit();
            }
            List<File> chosen = choose(() -> {
                FileChooser chooser = fileChooser();
                chooser.setInitialDirectory(multi.toFile());
                return chooser.showOpenMultipleDialog(stage);
            }, () -> {
                // The list of files has the focus once the dialog shows the folder
                TestKit.shortcut(KeyCode.A, KeyCode.CONTROL);
                TestKit.pause(300);
                TestKit.shortcut(KeyCode.ENTER);
            });
            check(chosen != null && chosen.size() == 2, "the dialog returned " + chosen);
            check(chosen.contains(multi.resolve("one.txt").toFile())
                    && chosen.contains(multi.resolve("two.txt").toFile()), "the files are " + chosen);
            return chosen.size() + " files";
        });
        suite.robot("Save with the initial name", "Accepts the initial file name with Enter", () -> {
            File chosen = choose(() -> {
                FileChooser chooser = fileChooser();
                chooser.setInitialFileName("delta.txt");
                return chooser.showSaveDialog(stage);
            }, () -> TestKit.shortcut(KeyCode.ENTER));
            check(testDir().resolve("delta.txt").toFile().equals(chosen), "the dialog returned " + chosen);
            return chosen.getName();
        });
        suite.robot("Save with a typed name", "Replaces the initial name with epsilon.txt", () -> {
            File chosen = choose(() -> {
                FileChooser chooser = fileChooser();
                chooser.setInitialFileName("untitled.txt");
                return chooser.showSaveDialog(stage);
            }, () -> {
                TestKit.shortcut(KeyCode.A, KeyCode.CONTROL);
                TestKit.type("epsilon.txt");
                TestKit.pause(200);
                TestKit.shortcut(KeyCode.ENTER);
            });
            check(testDir().resolve("epsilon.txt").toFile().equals(chosen), "the dialog returned " + chosen);
            return chosen.getName();
        });
        suite.robot("Choose a folder", "Types the path of the inner folder and selects it", () -> {
            Path inner = testDir().resolve("inner");
            File chosen = choose(() -> {
                DirectoryChooser chooser = new DirectoryChooser();
                chooser.setInitialDirectory(testDir().toFile());
                return chooser.showDialog(stage);
            }, () -> typePath(inner));
            check(chosen != null && (chosen.equals(inner.toFile()) || chosen.equals(testDir().toFile())),
                    "the dialog returned " + chosen);
            return chosen.getName();
        });
        suite.robot("Extension filter", "Opens with the Text, Images and All files filters, then reads the choice",
                () -> {
                    FileChooser[] holder = {null};
                    Path file = testDir().resolve("gamma.png");
                    File chosen = choose(() -> {
                        holder[0] = fileChooser();
                        return holder[0].showOpenDialog(stage);
                    }, () -> typePath(file));
                    check(file.toFile().equals(chosen), "the dialog returned " + chosen);
                    FileChooser.ExtensionFilter filter = fx(() -> holder[0].getSelectedExtensionFilter());
                    check(filter != null, "no selected extension filter");
                    return chosen.getName() + " with the filter " + filter.getDescription();
                });
        suite.robot("Owner blocked", "Clicks the owner while a file chooser is open", () -> {
            int before = fx(() -> ownerClicks);
            File chosen = choose(() -> fileChooser().showOpenDialog(stage), () -> {
                TestKit.click(ownerTarget);
                TestKit.pause(400);
                check(fx(() -> ownerClicks) == before, "the owner got the click");
                TestKit.shortcut(KeyCode.ESCAPE);
            });
            check(chosen == null, "the dialog returned " + chosen);
            return "the click did not reach the owner";
        });

        suite.robot("Alert", "A confirmation alert answers OK with Enter and CANCEL with Escape", () -> {
            Object ok = dialog(() -> new Alert(Alert.AlertType.CONFIRMATION, "Press Enter"),
                    () -> TestKit.shortcut(KeyCode.ENTER));
            check(ok == ButtonType.OK, "Enter answered " + ok);
            Object cancel = dialog(() -> new Alert(Alert.AlertType.CONFIRMATION, "Press Escape"),
                    () -> TestKit.shortcut(KeyCode.ESCAPE));
            check(cancel == ButtonType.CANCEL, "Escape answered " + cancel);
            return "OK and CANCEL";
        });
        suite.robot("Alert types", "Shows an information, a warning and an error alert, closed with Enter", () -> {
            StringBuilder sb = new StringBuilder();
            for (Alert.AlertType type : List.of(Alert.AlertType.INFORMATION, Alert.AlertType.WARNING,
                    Alert.AlertType.ERROR)) {
                Object answer = dialog(() -> new Alert(type, type + " alert"), () -> TestKit.shortcut(KeyCode.ENTER));
                check(answer == ButtonType.OK, type + " answered " + answer);
                sb.append(type).append(' ');
            }
            return sb.toString().trim();
        });
        suite.robot("Text input", "Types a value in a TextInputDialog", () -> {
            Object answer = dialog(() -> {
                TextInputDialog d = new TextInputDialog();
                d.setHeaderText("Type a value");
                return d;
            }, () -> {
                TestKit.type("Robot 42");
                TestKit.pause(150);
                TestKit.shortcut(KeyCode.ENTER);
            });
            check("Robot 42".equals(answer), "the dialog answered " + answer);
            return "\"" + answer + "\"";
        });
        suite.robot("Choice", "Picks the next choice of a ChoiceDialog with the keyboard", () -> {
            Object answer = dialog(() -> new ChoiceDialog<>("GTK 3", "GTK 3", "GTK 4", "Wayland"), () -> {
                // The choice box has the focus: Down selects the next item
                TestKit.shortcut(KeyCode.DOWN);
                TestKit.pause(150);
                TestKit.shortcut(KeyCode.ENTER);
            });
            check("GTK 4".equals(answer), "the dialog answered " + answer);
            return "\"" + answer + "\"";
        });
    }

    /**
     * Shows a file or folder chooser from the FX thread, without waiting for
     * it there, runs the answer of the robot once the dialog took the focus,
     * and returns what the chooser returned.
     */
    private <T> T choose(Supplier<T> chooser, Runnable answer) throws Exception {
        CompletableFuture<T> future = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                T value = chooser.get();
                show("Chooser", value);
                future.complete(value);
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });
        // The chooser of the portal is a window of another process
        TestKit.await("the dialog", (DIALOG_TIMEOUT_S / 2) * 1000, () -> !stage.isFocused());
        TestKit.pause(1200);
        answer.run();
        try {
            return future.get(3, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            // The first Enter of a typed path may only accept its completion
            TestKit.shortcut(KeyCode.ENTER);
        }
        try {
            return future.get(DIALOG_TIMEOUT_S, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            // Close it, so that the next tests can run
            for (int i = 0; i < 5 && !future.isDone(); i++) {
                TestKit.shortcut(KeyCode.ESCAPE);
                TestKit.pause(1000);
            }
            throw new AssertionError("the dialog did not close");
        }
    }

    /**
     * Shows a JavaFX dialog owned by the lab and returns its result after the
     * answer of the robot.
     */
    private Object dialog(Supplier<Dialog<?>> factory, Runnable answer) throws Exception {
        CompletableFuture<Object> future = new CompletableFuture<>();
        Dialog<?> d = fx(() -> {
            Dialog<?> dialog = factory.get();
            dialog.initOwner(stage);
            dialog.setOnHidden(e -> future.complete(Optional.ofNullable(dialog.getResult()).orElse(null)));
            dialog.show();
            return dialog;
        });
        TestKit.await("the dialog focused", () -> d.getDialogPane().getScene() != null
                && d.getDialogPane().getScene().getWindow().isFocused());
        TestKit.pause(300);
        answer.run();
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            fx(d::close);
            throw new AssertionError("the dialog did not close");
        }
    }

    /**
     * Opens the location entry with Ctrl+L, types a path and accepts it.
     */
    private static void typePath(Path path) {
        TestKit.shortcut(KeyCode.L, KeyCode.CONTROL);
        TestKit.pause(400);
        TestKit.type(path.toString());
        TestKit.pause(400);
        TestKit.shortcut(KeyCode.ENTER);
    }

    private static <T> T fx(Supplier<T> s) {
        return TestKit.fx(s);
    }

    private static void fx(Runnable r) {
        TestKit.fx(r);
    }

    private static void check(boolean condition, String message) {
        TestKit.check(condition, message);
    }
}
