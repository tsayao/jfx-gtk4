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
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Pagination;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
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
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.SVGPath;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * A laboratory for accessibility: one control of each accessible role the
 * GTK 4 glass maps to an AT-SPI role, with names, descriptions and role
 * descriptions set where the control has no visible text, controls whose
 * text, value, state and children change on demand, a burst of text changes
 * (after the AccessibilityNotificationTest of OpenJFX), and an inspector that
 * shows the accessible attributes of the focused node, the text offsets under
 * the pointer and the accessible tree of the window.
 * <p>
 * Use it with a screen reader (Orca, started with Super+Alt+S in GNOME) or
 * with an accessibility inspector such as Accerciser.
 * <p>
 * Its tests (see TestKit) check the accessible attributes of the controls,
 * read the AT-SPI tree of the application from another process (with
 * python3 and the Atspi GObject introspection bindings), and move the focus
 * with the keyboard through the robot.
 */
public class AccessibilityDemo extends Application {

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
            -fx-accent: #0ea5e9;
            -fx-focus-color: #38bdf8;
            -fx-faint-focus-color: #38bdf833;
            -fx-text-fill: #e8eef7;
        }
        .label, .check-box, .radio-button, .hyperlink { -fx-text-fill: #e8eef7; }
        .hyperlink { -fx-text-fill: #7dd3fc; }
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
        .key { -fx-text-fill: #93a1b8; -fx-font-size: 12px; }
        .value { -fx-font-family: "Adwaita Mono", "Monospace"; -fx-font-size: 12px; }
        .focus-title { -fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #7dd3fc; }
        .button, .toggle-button {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.14);
            -fx-border-radius: 9;
            -fx-text-fill: #e8eef7;
            -fx-padding: 6 14 6 14;
        }
        .button:hover, .toggle-button:hover { -fx-background-color: rgba(255, 255, 255, 0.15); }
        .button:focused, .toggle-button:focused { -fx-border-color: #38bdf8; -fx-border-width: 2; }
        .toggle-button:selected { -fx-background-color: rgba(14, 165, 233, 0.3); -fx-border-color: #0ea5e9; }
        .button:disabled { -fx-opacity: 0.4; }
        .combo-box, .text-field, .password-field, .spinner, .text-area {
            -fx-background-color: rgba(255, 255, 255, 0.08);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.16);
            -fx-border-radius: 9;
        }
        .combo-box:focused, .text-field:focused, .password-field:focused, .text-area:focused, .spinner:focused {
            -fx-border-color: #38bdf8;
        }
        .text-field, .password-field, .text-area, .spinner .text-field, .combo-box .list-cell { -fx-text-fill: #e8eef7; }
        .text-area .content { -fx-background-color: transparent; }
        .spinner .text-field { -fx-background-color: transparent; -fx-border-color: transparent; }
        .menu-bar { -fx-background-color: rgba(255, 255, 255, 0.05); -fx-background-radius: 9; }
        .menu-bar .label { -fx-text-fill: #e8eef7; }
        .tool-bar { -fx-background-color: rgba(255, 255, 255, 0.04); -fx-background-radius: 9; }
        .tab-pane > .tab-header-area > .headers-region > .tab { -fx-background-color: rgba(255, 255, 255, 0.06); }
        .tab-pane > .tab-header-area > .headers-region > .tab:selected { -fx-background-color: rgba(14, 165, 233, 0.3); }
        .tab-pane > .tab-header-area > .tab-header-background { -fx-background-color: transparent; }
        .tab .tab-label { -fx-text-fill: #e8eef7; }
        .list-view, .tree-view, .table-view {
            -fx-background-color: rgba(255, 255, 255, 0.03);
            -fx-background-radius: 9;
            -fx-border-color: rgba(255, 255, 255, 0.1);
            -fx-border-radius: 9;
        }
        .list-cell, .tree-cell, .table-row-cell { -fx-background-color: transparent; -fx-text-fill: #e8eef7; }
        .list-cell:filled:selected, .tree-cell:filled:selected, .table-row-cell:filled:selected {
            -fx-background-color: rgba(14, 165, 233, 0.28);
        }
        .table-view .column-header, .table-view .column-header-background { -fx-background-color: rgba(255, 255, 255, 0.05); }
        .table-view .column-header .label { -fx-text-fill: #cbd5e1; }
        .table-cell { -fx-text-fill: #e8eef7; }
        .slider .track { -fx-background-color: rgba(255, 255, 255, 0.14); -fx-background-radius: 4; }
        .slider .thumb { -fx-background-color: white; }
        .progress-bar > .track { -fx-background-color: rgba(255, 255, 255, 0.12); -fx-background-radius: 6; }
        .progress-bar > .bar { -fx-background-color: linear-gradient(to right, #0ea5e9, #6366f1); -fx-background-radius: 6; }
        .scroll-pane, .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-bar { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: rgba(255, 255, 255, 0.25); -fx-background-radius: 6; }
        .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 0; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-shape: " "; -fx-padding: 0; }
        """;

    static final String STYLESHEET = "data:text/css;base64,"
            + Base64.getEncoder().encodeToString(CSS.getBytes(StandardCharsets.UTF_8));

    /** The attributes the inspector queries, when the role answers them. */
    private static final AccessibleAttribute[] INSPECTED = {
        AccessibleAttribute.ROLE, AccessibleAttribute.ROLE_DESCRIPTION, AccessibleAttribute.TEXT,
        AccessibleAttribute.HELP, AccessibleAttribute.VALUE, AccessibleAttribute.VALUE_STRING,
        AccessibleAttribute.MIN_VALUE, AccessibleAttribute.MAX_VALUE, AccessibleAttribute.SELECTED,
        AccessibleAttribute.INDETERMINATE, AccessibleAttribute.EXPANDED, AccessibleAttribute.DISABLED,
        AccessibleAttribute.EDITABLE, AccessibleAttribute.ITEM_COUNT, AccessibleAttribute.ROW_COUNT,
        AccessibleAttribute.COLUMN_COUNT, AccessibleAttribute.SELECTED_ITEMS, AccessibleAttribute.CARET_OFFSET,
        AccessibleAttribute.LABELED_BY, AccessibleAttribute.ACCELERATOR, AccessibleAttribute.MNEMONIC,
    };

    public record Person(String name, String role, int year) {}

    private boolean backdropSupported;
    private Parent contentRoot;
    private final Label focusTitle = new Label("Nothing focused");
    private final GridPane focusGrid = new GridPane(12, 3);
    private final TreeView<Node> tree = new TreeView<>();
    private final Label treeInfo = new Label();
    private final Label pointerText = new Label("Point at a text to query its offsets");
    private final TextArea stressArea = new TextArea();
    private Timeline stressTimeline;

    // Controls that the live changes card modifies
    private final Button statusButton = new Button("Status: idle");
    private final ProgressBar progress = new ProgressBar(0);
    private final ListView<String> list = new ListView<>(FXCollections.observableArrayList(
            "Orca", "Accerciser", "AT-SPI", "GTK 4"));
    private final TextField nameField = new TextField();
    private final CheckBox agree = new CheckBox("I agree with the terms");
    private Timeline progressTimeline;
    private int added;

    // Controls that the tests check
    final TestKit.Suite suite = new TestKit.Suite("Accessibility");
    private final Button iconButton = new Button();
    private final ToggleButton boldButton = new ToggleButton("Bold");
    private final Label nameLabel = new Label("_Name");
    private final PasswordField password = new PasswordField();
    private final TextArea bio = new TextArea("JavaFX on GTK 4.\nSecond line of the biography.");
    private final ComboBox<String> country = new ComboBox<>(FXCollections.observableArrayList(
            "Brazil", "Canada", "Germany", "Japan", "Portugal"));
    private final Spinner<Integer> age = new Spinner<>(0, 120, 30);
    private final Slider volume = new Slider(0, 100, 40);
    private final RadioButton medium = new RadioButton("Medium");
    private final Hyperlink link = new Hyperlink("openjfx.io");
    private final ImageView image = new ImageView(sampleImage());
    private MenuBar menuBar;
    private ToolBar toolBar;
    private Menu viewMenu;
    private TabPane tabs;
    private TreeView<String> desktops;
    private TableView<Person> table;
    private Pagination pages;

    @Override
    public void start(Stage stage) {
        stage.initStyle(StageStyle.EXTENDED);
        BorderPane body = new BorderPane();
        body.setTop(header());
        body.setCenter(content(stage));

        StackPane root = new StackPane(backgroundLayer(), body);
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        Scene scene = new Scene(root, Math.min(1240, screen.getWidth() * 0.92),
                Math.min(900, screen.getHeight() * 0.92), Color.TRANSPARENT);
        scene.getStylesheets().add(STYLESHEET);
        scene.getPreferences().setColorScheme(ColorScheme.DARK);
        stage.setScene(scene);
        stage.setTitle("Accessibility Lab");
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.show();
        TestKit.autorun(suite);
    }

    /**
     * The lab without its header bar, which the tests launcher shows in a tab.
     */
    Region content(Stage stage) {
        backdropSupported = Boolean.getBoolean("glass.gtk4.backdrop.active");
        VBox controls = column(formCard(), collectionsCard(), testsCard(stage));
        ScrollPane controlsScroll = new ScrollPane(controls);
        controlsScroll.setFitToWidth(true);
        controlsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        controlsScroll.setAccessibleText("Controls");
        VBox right = column(liveCard(), inspectorCard(), treeCard());
        HBox columns = new HBox(14, controlsScroll, right);
        HBox.setHgrow(controlsScroll, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        controlsScroll.setPrefWidth(1);
        right.setPrefWidth(1);
        columns.setPadding(new Insets(4, 18, 18, 18));
        columns.addEventFilter(MouseEvent.MOUSE_MOVED, this::inspectText);
        contentRoot = columns;
        TestKit.whenInScene(columns, scene -> {
            scene.focusOwnerProperty().addListener((obs, o, n) -> inspect(n));
            refreshTree();
            inspect(scene.getFocusOwner());
        });
        return columns;
    }

    private Region backgroundLayer() {
        Region fill = new Region();
        if (backdropSupported) {
            fill.setBackground(new Background(new BackgroundFill(Color.color(0.02, 0.03, 0.06, 0.22), null, null)));
        } else {
            fill.setBackground(new Background(new BackgroundFill(new RadialGradient(0, 0, 0.2, 0.1, 1.2, true,
                    CycleMethod.NO_CYCLE, new Stop(0, Color.web("#0c2a3a")), new Stop(0.6, Color.web("#0b1120")),
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
                new Stop(0, Color.web("#0ea5e9")), new Stop(1, Color.web("#6366f1"))));
        logo.setEffect(new Glow(0.6));
        Label brand = new Label("Accessibility Lab");
        brand.getStyleClass().add("brand");
        Label sub = muted("roles · names · states · live changes · inspector");
        HBox left = new HBox(10, logo, brand, sub);
        left.setAlignment(Pos.CENTER_LEFT);
        left.setPadding(new Insets(0, 0, 0, 18));

        HBox at = pill("");
        ((Label) at.getChildren().getFirst()).textProperty().bind(Bindings.when(
                Platform.accessibilityActiveProperty()).then("Assistive technology connected")
                .otherwise("No assistive technology"));
        HBox right = new HBox(10, at, pill(System.getProperty("glass.platform", "default") + " glass"));
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

    // ---------------------------------------------------------------------
    // Controls

    private Node formCard() {
        menuBar = new MenuBar(
                new Menu("_File", null, new MenuItem("_New"), new MenuItem("_Open…"), new SeparatorMenuItem(),
                        new MenuItem("_Quit")),
                viewMenu(),
                new Menu("_Help", null, new MenuItem("_About")));
        menuBar.getMenus().forEach(m -> m.setMnemonicParsing(true));

        Button icon = iconButton;
        SVGPath trash = new SVGPath();
        trash.setContent("M3 6h18M8 6V4h8v2m-9 0 1 14h8l1-14");
        trash.setStroke(Color.web("#e8eef7"));
        trash.setFill(Color.TRANSPARENT);
        trash.setStrokeWidth(2);
        icon.setGraphic(trash);
        icon.setAccessibleText("Delete");
        icon.setAccessibleHelp("Deletes the selected item. This button only has an icon.");
        icon.setTooltip(new Tooltip("Delete"));
        ToggleButton bold = boldButton;
        bold.setAccessibleHelp("Toggles bold text");
        toolBar = new ToolBar(new Button("Cut"), new Button("Copy"), new Button("Paste"), icon, bold);
        toolBar.setAccessibleText("Editing tools");

        nameLabel.setMnemonicParsing(true);
        nameLabel.setLabelFor(nameField);
        nameField.setPromptText("Your name");
        nameField.setAccessibleHelp("The name shown on your profile");
        Label passLabel = new Label("_Password");
        passLabel.setMnemonicParsing(true);
        passLabel.setLabelFor(password);
        Label bioLabel = new Label("_Biography");
        bioLabel.setMnemonicParsing(true);
        bio.setPrefRowCount(3);
        bioLabel.setLabelFor(bio);
        Label countryLabel = new Label("_Country");
        countryLabel.setMnemonicParsing(true);
        country.setValue("Brazil");
        countryLabel.setLabelFor(country);
        Label ageLabel = new Label("_Age");
        ageLabel.setMnemonicParsing(true);
        age.setEditable(true);
        ageLabel.setLabelFor(age);
        Label volumeLabel = new Label("_Volume");
        volumeLabel.setMnemonicParsing(true);
        volume.setBlockIncrement(5);
        volumeLabel.setLabelFor(volume);
        volume.setAccessibleRoleDescription("volume slider");

        GridPane grid = new GridPane(12, 8);
        grid.addRow(0, nameLabel, nameField);
        grid.addRow(1, passLabel, password);
        grid.addRow(2, bioLabel, bio);
        grid.addRow(3, countryLabel, country);
        grid.addRow(4, ageLabel, age);
        grid.addRow(5, volumeLabel, volume);
        GridPane.setHgrow(nameField, Priority.ALWAYS);

        ToggleGroup sizeGroup = new ToggleGroup();
        RadioButton small = new RadioButton("Small");
        RadioButton large = new RadioButton("Large");
        for (RadioButton r : List.of(small, medium, large)) {
            r.setToggleGroup(sizeGroup);
        }
        medium.setSelected(true);
        HBox sizes = new HBox(14, new Label("Size"), small, medium, large);
        sizes.setAlignment(Pos.CENTER_LEFT);
        sizes.setAccessibleRole(AccessibleRole.PARENT);

        CheckBox maybe = new CheckBox("Indeterminate check box");
        maybe.setAllowIndeterminate(true);
        maybe.setIndeterminate(true);
        link.setAccessibleHelp("Opens the JavaFX website");
        image.setAccessibleText("A sunset over a dark sea");
        image.setFocusTraversable(true);
        HBox misc = new HBox(14, agree, maybe, link, image);
        misc.setAlignment(Pos.CENTER_LEFT);

        return card("Forms", menuBar, toolBar, grid, sizes, misc);
    }

    private Menu viewMenu() {
        CheckMenuItem statusBar = new CheckMenuItem("Status _bar");
        statusBar.setSelected(true);
        ToggleGroup zoom = new ToggleGroup();
        RadioMenuItem z100 = new RadioMenuItem("_100%");
        RadioMenuItem z150 = new RadioMenuItem("1_50%");
        z100.setToggleGroup(zoom);
        z150.setToggleGroup(zoom);
        z100.setSelected(true);
        MenuItem fullScreen = new MenuItem("_Full screen");
        fullScreen.setAccelerator(javafx.scene.input.KeyCombination.keyCombination("F11"));
        viewMenu = new Menu("_View", null, statusBar, new SeparatorMenuItem(), z100, z150, new SeparatorMenuItem(),
                fullScreen);
        return viewMenu;
    }

    private static WritableImage sampleImage() {
        int w = 48, h = 32;
        WritableImage image = new WritableImage(w, h);
        PixelWriter pw = image.getPixelWriter();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                double dx = x - 30, dy = y - 12;
                Color sky = Color.web("#f59e0b").interpolate(Color.web("#6d28d9"), (double) y / h);
                pw.setColor(x, y, dx * dx + dy * dy < 36 ? Color.web("#fde68a") : y > 22 ? Color.web("#0f172a") : sky);
            }
        }
        return image;
    }

    private Node collectionsCard() {
        list.setPrefHeight(120);
        list.setAccessibleText("Tools");

        TreeItem<String> rootItem = new TreeItem<>("Desktop");
        TreeItem<String> gnome = new TreeItem<>("GNOME");
        gnome.getChildren().addAll(List.of(new TreeItem<>("Shell"), new TreeItem<>("Mutter")));
        TreeItem<String> kde = new TreeItem<>("KDE");
        kde.getChildren().addAll(List.of(new TreeItem<>("Plasma"), new TreeItem<>("KWin")));
        rootItem.getChildren().addAll(List.of(gnome, kde));
        rootItem.setExpanded(true);
        gnome.setExpanded(true);
        desktops = new TreeView<>(rootItem);
        desktops.setPrefHeight(150);
        desktops.setAccessibleText("Desktops");

        table = new TableView<>(FXCollections.observableArrayList(
                new Person("Ada", "Engineer", 1815), new Person("Grace", "Admiral", 1906),
                new Person("Alan", "Mathematician", 1912)));
        TableColumn<Person, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().name()));
        TableColumn<Person, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().role()));
        TableColumn<Person, String> yearCol = new TableColumn<>("Born");
        yearCol.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().year())));
        table.getColumns().addAll(List.of(nameCol, roleCol, yearCol));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(130);
        table.setAccessibleText("People");

        tabs = new TabPane(
                new Tab("List", list),
                new Tab("Tree", desktops),
                new Tab("Table", table));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setPrefHeight(200);

        pages = new Pagination(5, 0);
        pages.setPageFactory(i -> new Label("Page " + (i + 1) + " of 5"));
        pages.setPrefHeight(90);
        pages.setAccessibleText("Pages");

        progress.setMaxWidth(Double.MAX_VALUE);
        progress.setAccessibleText("Download");
        return card("Collections", tabs, new Label("Progress"), progress, pages);
    }

    // ---------------------------------------------------------------------
    // Live changes

    private Node liveCard() {
        Button rename = new Button("Rename the status button");
        rename.setOnAction(e -> statusButton.setText(statusButton.getText().endsWith("idle")
                ? "Status: busy" : "Status: idle"));
        Button disable = new Button("Disable / enable the name field");
        disable.setOnAction(e -> nameField.setDisable(!nameField.isDisable()));
        Button check = new Button("Toggle the check box");
        check.setOnAction(e -> agree.setSelected(!agree.isSelected()));
        Button add = new Button("Add a list item");
        add.setOnAction(e -> {
            list.getItems().add("Item " + (++added));
            list.getSelectionModel().selectLast();
            list.scrollTo(list.getItems().size() - 1);
        });
        Button remove = new Button("Remove a list item");
        remove.setOnAction(e -> {
            if (!list.getItems().isEmpty()) {
                list.getItems().removeLast();
            }
        });
        Button type = new Button("Set text in the name field");
        type.setOnAction(e -> nameField.setText(nameField.getText().isEmpty() ? "Ada Lovelace" : ""));
        Button run = new Button("Start / stop the progress");
        run.setOnAction(e -> toggleProgress());
        Button focus = new Button("Move focus to the name field (in 2 s)");
        focus.setOnAction(e -> new Timeline(new KeyFrame(Duration.seconds(2), f -> nameField.requestFocus())).play());

        Button stress = new Button("Burst of 1000 text changes");
        stress.setOnAction(e -> stressText());

        statusButton.setAccessibleHelp("Its text is changed by the button on its left");
        FlowPane buttons = new FlowPane(8, 8, rename, statusButton, disable, check, add, remove, type, run, focus,
                stress);
        stressArea.setPrefRowCount(2);
        stressArea.setWrapText(true);
        stressArea.setAccessibleText("Burst target");
        Label hint = muted("Each button changes another control: a screen reader announces the new name, "
                + "state, value or children, and the inspector below shows them. The burst appends to the "
                + "text area every 10 ms with a screen reader running: the application must not crash nor "
                + "stall.");
        return card("Live changes", buttons, stressArea, hint);
    }

    private void stressText() {
        if (stressTimeline != null) {
            stressTimeline.stop();
        }
        stressArea.clear();
        int[] i = {0};
        long start = System.nanoTime();
        stressTimeline = new Timeline(new KeyFrame(Duration.millis(10), e -> {
            stressArea.appendText(String.valueOf(i[0]++));
            if (i[0] == 1000) {
                stressArea.appendText(String.format(Locale.ROOT, "\nDone in %d ms",
                        (System.nanoTime() - start) / 1_000_000));
            }
        }));
        stressTimeline.setCycleCount(1000);
        stressTimeline.play();
    }

    private void toggleProgress() {
        if (progressTimeline != null) {
            progressTimeline.stop();
            progressTimeline = null;
            return;
        }
        progressTimeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
            double v = progress.getProgress() + 0.1;
            progress.setProgress(v > 1.0001 ? 0 : v);
        }));
        progressTimeline.setCycleCount(Timeline.INDEFINITE);
        progressTimeline.play();
    }

    // ---------------------------------------------------------------------
    // Inspector

    private Node inspectorCard() {
        focusTitle.getStyleClass().add("focus-title");
        focusGrid.setMinHeight(120);
        pointerText.getStyleClass().add("value");
        pointerText.setWrapText(true);
        Label hint = muted("Move the focus with Tab, Shift+Tab and the arrow keys; the inspector queries the "
                + "accessible attributes of the focused node, which are what the glass reports to AT-SPI. "
                + "Over a text, the last line shows the offset, line and line bounds under the pointer, "
                + "which screen readers use to read by character and by line.");
        return card("Focused node", focusTitle, focusGrid, pointerText, hint);
    }

    private void inspect(Node node) {
        focusGrid.getChildren().clear();
        if (node == null) {
            focusTitle.setText("Nothing focused");
            return;
        }
        focusTitle.setText(node.getClass().getSimpleName()
                + (node.getId() != null ? " #" + node.getId() : ""));
        int row = 0;
        for (AccessibleAttribute attribute : INSPECTED) {
            Object value;
            try {
                value = node.queryAccessibleAttribute(attribute);
            } catch (RuntimeException e) {
                value = "error: " + e.getMessage();
            }
            if (value == null) {
                continue;
            }
            Label k = new Label(attribute.name());
            k.getStyleClass().add("key");
            Label v = new Label(describe(value));
            v.getStyleClass().add("value");
            v.setWrapText(true);
            focusGrid.addRow(row++, k, v);
        }
        if (node.getAccessibleHelp() != null || node.getAccessibleText() != null) {
            Label k = new Label("set by the app");
            k.getStyleClass().add("key");
            Label v = new Label("text=" + node.getAccessibleText() + ", help=" + node.getAccessibleHelp());
            v.getStyleClass().add("value");
            v.setWrapText(true);
            focusGrid.addRow(row, k, v);
        }
    }

    /**
     * Queries the text attributes at the pointer of the nearest node that
     * answers OFFSET_AT_POINT.
     */
    private void inspectText(MouseEvent e) {
        Point2D point = new Point2D(e.getScreenX(), e.getScreenY());
        for (Node n = e.getPickResult().getIntersectedNode(); n != null; n = n.getParent()) {
            if (n.queryAccessibleAttribute(AccessibleAttribute.OFFSET_AT_POINT, point) instanceof Integer offset) {
                Object line = n.queryAccessibleAttribute(AccessibleAttribute.LINE_FOR_OFFSET, offset);
                Object lineStart = line == null ? null
                        : n.queryAccessibleAttribute(AccessibleAttribute.LINE_START, line);
                Object lineEnd = line == null ? null : n.queryAccessibleAttribute(AccessibleAttribute.LINE_END, line);
                Object text = n.queryAccessibleAttribute(AccessibleAttribute.TEXT);
                String ch = text instanceof String s && offset >= 0 && offset < s.length()
                        ? " '" + s.charAt(offset) + "'" : "";
                pointerText.setText(n.getClass().getSimpleName() + ": offset " + offset + ch + ", line " + line
                        + " [" + lineStart + ", " + lineEnd + ")");
                return;
            }
        }
    }

    private static String describe(Object value) {
        if (value instanceof Node n) {
            Object text = n.queryAccessibleAttribute(AccessibleAttribute.TEXT);
            return n.getClass().getSimpleName() + (text != null ? " \"" + text + "\"" : "");
        }
        if (value instanceof List<?> l) {
            return l.size() + " items";
        }
        if (value instanceof Double d) {
            return String.format(Locale.ROOT, "%.2f", d);
        }
        String s = String.valueOf(value);
        return s.length() > 120 ? s.substring(0, 120) + "…" : s;
    }

    // ---------------------------------------------------------------------
    // Tests

    /**
     * Reads the AT-SPI tree of a process, or invokes the first action of the
     * first accessible with a role and a name, the way assistive technologies
     * do. Prints one line per accessible: depth, role, name and states.
     */
    private static final String ATSPI_SCRIPT = """
            import sys, gi
            gi.require_version('Atspi', '2.0')
            from gi.repository import Atspi
            command, pid = sys.argv[1], int(sys.argv[2])
            STATES = (('checked', Atspi.StateType.CHECKED), ('focused', Atspi.StateType.FOCUSED),
                      ('enabled', Atspi.StateType.ENABLED), ('showing', Atspi.StateType.SHOWING))
            def walk(a, depth, visit):
                if visit(a, depth):
                    return True
                for i in range(min(a.get_child_count(), 400)):
                    c = a.get_child_at_index(i)
                    if c is not None and walk(c, depth + 1, visit):
                        return True
                return False
            def dump(a, depth):
                states = a.get_state_set()
                flags = ','.join(n for n, s in STATES if states.contains(s))
                print('%d\\t%s\\t%s\\t%s' % (depth, a.get_role_name(), (a.get_name() or '').replace('\\n', ' '), flags))
                return False
            def act(a, depth):
                if a.get_role_name() == sys.argv[3] and a.get_name() == sys.argv[4]:
                    action = a.get_action_iface()
                    if action is None or action.get_n_actions() == 0:
                        print('no action interface')
                    else:
                        print('done' if action.do_action(0) else 'action failed')
                    return True
                return False
            desktop = Atspi.get_desktop(0)
            for i in range(desktop.get_child_count()):
                app = desktop.get_child_at_index(i)
                if app is not None and app.get_process_id() == pid:
                    if not walk(app, 0, dump if command == 'dump' else act):
                        print('missing' if command != 'dump' else '')
                    break
            else:
                print('no application')
            """;

    private record AtspiNode(int depth, String role, String name, String states) {}

    private Node testsCard(Stage stage) {
        defineTests();
        suite.setBeforeRun(() -> {
            stage.toFront();
            stage.requestFocus();
        });
        return card("Tests", suite.view());
    }

    private void defineTests() {
        suite.auto("Roles", "Each control answers ROLE with its accessible role", () -> {
            Map<Node, AccessibleRole> roles = fx(() -> {
                Map<Node, AccessibleRole> m = new LinkedHashMap<>();
                m.put(iconButton, AccessibleRole.BUTTON);
                m.put(boldButton, AccessibleRole.TOGGLE_BUTTON);
                m.put(nameField, AccessibleRole.TEXT_FIELD);
                m.put(password, AccessibleRole.PASSWORD_FIELD);
                m.put(bio, AccessibleRole.TEXT_AREA);
                m.put(country, AccessibleRole.COMBO_BOX);
                m.put(age, AccessibleRole.SPINNER);
                m.put(volume, AccessibleRole.SLIDER);
                m.put(medium, AccessibleRole.RADIO_BUTTON);
                m.put(agree, AccessibleRole.CHECK_BOX);
                m.put(link, AccessibleRole.HYPERLINK);
                m.put(image, AccessibleRole.IMAGE_VIEW);
                m.put(menuBar, AccessibleRole.MENU_BAR);
                m.put(toolBar, AccessibleRole.TOOL_BAR);
                m.put(tabs, AccessibleRole.TAB_PANE);
                m.put(list, AccessibleRole.LIST_VIEW);
                m.put(desktops, AccessibleRole.TREE_VIEW);
                m.put(table, AccessibleRole.TABLE_VIEW);
                m.put(progress, AccessibleRole.PROGRESS_INDICATOR);
                m.put(pages, AccessibleRole.PAGINATION);
                return m;
            });
            for (Map.Entry<Node, AccessibleRole> e : roles.entrySet()) {
                Object role = fx(() -> e.getKey().queryAccessibleAttribute(AccessibleAttribute.ROLE));
                TestKit.check(role == e.getValue(), e.getKey().getClass().getSimpleName() + " answers " + role
                        + ", expected " + e.getValue());
            }
            return roles.size() + " roles";
        });
        suite.auto("Names and labels", "Accessible text, labels, help and role descriptions", () -> {
            fx(() -> {
                TestKit.check("Delete".equals(iconButton.queryAccessibleAttribute(AccessibleAttribute.TEXT)),
                        "the icon button is named " + iconButton.queryAccessibleAttribute(AccessibleAttribute.TEXT));
                TestKit.check(nameField.queryAccessibleAttribute(AccessibleAttribute.LABELED_BY) == nameLabel,
                        "the name field is labeled by " + nameField.queryAccessibleAttribute(
                                AccessibleAttribute.LABELED_BY));
                TestKit.check("volume slider".equals(volume.queryAccessibleAttribute(
                        AccessibleAttribute.ROLE_DESCRIPTION)), "the slider is described as "
                        + volume.queryAccessibleAttribute(AccessibleAttribute.ROLE_DESCRIPTION));
                TestKit.check("A sunset over a dark sea".equals(image.queryAccessibleAttribute(
                        AccessibleAttribute.TEXT)), "the image is named "
                        + image.queryAccessibleAttribute(AccessibleAttribute.TEXT));
                TestKit.check("The name shown on your profile".equals(nameField.queryAccessibleAttribute(
                        AccessibleAttribute.HELP)), "the help of the name field is "
                        + nameField.queryAccessibleAttribute(AccessibleAttribute.HELP));
            });
            return "text, LABELED_BY, ROLE_DESCRIPTION and HELP";
        });
        suite.auto("AT-SPI tree", "Reads the tree of this process with Atspi, as a screen reader does", () -> {
            List<AtspiNode> nodes = atspiTree();
            List<String> missing = new ArrayList<>();
            String[][] expected = {
                {"button", "Delete"}, {"toggle button", "Bold"}, {"text", "Name"}, {"text", "Password"},
                {"text", "Biography"}, {"combo box", "Country"}, {"spin button", "Age"}, {"slider", "Volume"},
                {"radio button", "Medium"}, {"check box", "I agree with the terms"}, {"link", "openjfx.io"},
                {"image", "A sunset over a dark sea"}, {"tool bar", "Editing tools"}, {"menu", "View"},
                {"list", "Tools"}, {"list item", "Orca"}, {"page tab", "Table"}, {"progress bar", "Download"},
            };
            for (String[] e : expected) {
                if (find(nodes, e[0], e[1]) == null) {
                    missing.add(e[0] + " \"" + e[1] + "\"");
                }
            }
            TestKit.check(missing.isEmpty(), "missing in AT-SPI: " + String.join(", ", missing));
            AtspiNode radio = find(nodes, "radio button", "Medium");
            TestKit.check(radio.states().contains("checked"), "the radio button Medium is not checked in AT-SPI");
            return nodes.size() + " accessibles, " + expected.length + " checked";
        });
        suite.auto("AT-SPI name change", "Renames the status button and reads the new name with Atspi", () -> {
            String old = fx(() -> statusButton.getText());
            String renamed = "Status: renamed " + System.nanoTime() % 1000;
            fx(() -> statusButton.setText(renamed));
            try {
                awaitAtspi("the new name", () -> find(atspiTree(), "button", renamed) != null);
            } finally {
                fx(() -> statusButton.setText(old));
            }
            return "\"" + renamed + "\"";
        });
        suite.auto("AT-SPI state change", "Checks the check box and reads its state with Atspi", () -> {
            boolean old = fx(() -> agree.isSelected());
            fx(() -> agree.setSelected(!old));
            try {
                awaitAtspi("the new state", () -> {
                    AtspiNode n = find(atspiTree(), "check box", "I agree with the terms");
                    return n != null && n.states().contains("checked") == !old;
                });
            } finally {
                fx(() -> agree.setSelected(old));
            }
            return old ? "unchecked" : "checked";
        });
        suite.auto("AT-SPI action", "Clicks the rename button through its AT-SPI action", () -> {
            String old = fx(() -> statusButton.getText());
            String result = atspi("action", "button", "Rename the status button").trim();
            if (result.equals("no action interface")) {
                // GTK offers the Action interface of AT-SPI for its own widgets only
                TestKit.skip("GTK exposes no AT-SPI actions for the accessibles of the glass");
            }
            TestKit.check(result.equals("done"), "the action answered " + result);
            await("the renamed status button", () -> !statusButton.getText().equals(old));
            String now = fx(() -> statusButton.getText());
            fx(() -> statusButton.setText(old));
            return "\"" + old + "\" → \"" + now + "\"";
        });
        suite.auto("Text offsets", "OFFSET_AT_POINT, LINE_FOR_OFFSET and LINE_START on the biography", () -> {
            TestKit.reveal(bio);
            int secondLine = fx(() -> bio.getText().indexOf('\n') + 1);
            Object[] answers = fx(() -> {
                javafx.scene.text.Text text = (javafx.scene.text.Text) bio.lookup(".text");
                javafx.geometry.Bounds b = text.localToScreen(text.getLayoutBounds());
                double lineHeight = b.getHeight() / 2;
                Point2D p = new Point2D(b.getMinX() + 1, b.getMinY() + lineHeight * 1.5);
                Object offset = bio.queryAccessibleAttribute(AccessibleAttribute.OFFSET_AT_POINT, p);
                Object line = offset == null ? null : bio.queryAccessibleAttribute(AccessibleAttribute.LINE_FOR_OFFSET,
                        offset);
                Object start = line == null ? null : bio.queryAccessibleAttribute(AccessibleAttribute.LINE_START,
                        line);
                return new Object[] {offset, line, start};
            });
            TestKit.check(answers[0] instanceof Integer o && Math.abs(o - secondLine) <= 1,
                    "the offset at the start of the second line is " + answers[0] + ", expected " + secondLine);
            TestKit.check(Integer.valueOf(1).equals(answers[1]), "the line of that offset is " + answers[1]);
            TestKit.check(Integer.valueOf(secondLine).equals(answers[2]), "the second line starts at " + answers[2]);
            return "offset " + answers[0] + ", line " + answers[1] + ", line start " + answers[2];
        });
        suite.auto("Burst of text changes", "Appends to a text area every 10 ms, 1000 times", () -> {
            fx(this::stressText);
            await("the end of the burst", 30_000, () -> stressArea.getText().contains("Done in"));
            String text = fx(() -> stressArea.getText());
            return text.substring(text.indexOf("Done in"));
        });
        suite.robot("Tab moves the focus", "Tab and Shift+Tab from the name field", () -> {
            TestKit.click(nameField);
            await("the name field focused", nameField::isFocused);
            TestKit.shortcut(KeyCode.TAB);
            await("the password field focused", password::isFocused);
            TestKit.shortcut(KeyCode.TAB, KeyCode.SHIFT);
            await("the name field focused again", nameField::isFocused);
            return "name → password → name";
        });
        suite.robot("Mnemonics", "Alt+P and Alt+B focus the labeled fields", () -> {
            TestKit.click(nameField);
            await("the name field focused", nameField::isFocused);
            TestKit.shortcut(KeyCode.P, KeyCode.ALT);
            await("the password field focused", password::isFocused);
            TestKit.shortcut(KeyCode.B, KeyCode.ALT);
            await("the biography focused", bio::isFocused);
            return "Alt+P → password, Alt+B → biography";
        });
        suite.robot("Menu mnemonic", "Alt+V opens the View menu, Escape closes it", () -> {
            TestKit.click(nameField);
            TestKit.shortcut(KeyCode.V, KeyCode.ALT);
            await("the View menu", () -> viewMenu.isShowing());
            TestKit.shortcut(KeyCode.ESCAPE);
            await("the View menu closed", () -> !viewMenu.isShowing());
            return "shown and closed";
        });
        suite.robot("Offsets under the pointer", "Points at the biography and checks the inspector", () -> {
            TestKit.reveal(bio);
            fx(() -> pointerText.setText(""));
            Point2D p = fx(() -> {
                javafx.scene.text.Text text = (javafx.scene.text.Text) bio.lookup(".text");
                javafx.geometry.Bounds b = text.localToScreen(text.getLayoutBounds());
                return new Point2D(b.getMinX() + 2, b.getMinY() + b.getHeight() / 4);
            });
            TestKit.move(p);
            TestKit.move(p.add(1, 0));
            // The text node of the skin answers, inside the text area
            await("the inspector line", () -> pointerText.getText().contains(": offset"));
            String text = fx(() -> pointerText.getText());
            TestKit.check(text.contains("line 0 "), "the inspector shows " + text);
            return text;
        });
    }

    private static AtspiNode find(List<AtspiNode> nodes, String role, String name) {
        for (AtspiNode n : nodes) {
            if (n.role().equals(role) && n.name().equals(name)) {
                return n;
            }
        }
        return null;
    }

    private static List<AtspiNode> atspiTree() throws Exception {
        List<AtspiNode> nodes = new ArrayList<>();
        for (String line : atspi("dump").split("\n")) {
            String[] f = line.split("\t", -1);
            if (f.length == 4) {
                nodes.add(new AtspiNode(Integer.parseInt(f[0]), f[1], f[2], f[3]));
            } else if (line.equals("no application")) {
                throw new AssertionError("this process is not on the AT-SPI bus");
            }
        }
        return nodes;
    }

    private static Path script;

    /**
     * Runs the AT-SPI script with a command, skipping the test when python3
     * or its Atspi bindings are missing.
     */
    private static String atspi(String... args) throws Exception {
        if (script == null) {
            script = Files.createTempFile("jfx-atspi", ".py");
            script.toFile().deleteOnExit();
            Files.writeString(script, ATSPI_SCRIPT);
        }
        List<String> command = new ArrayList<>(List.of("python3", "-I", script.toString(), args[0],
                String.valueOf(ProcessHandle.current().pid())));
        command.addAll(Arrays.asList(args).subList(1, args.length));
        Process p;
        try {
            p = new ProcessBuilder(command).start();
        } catch (IOException e) {
            TestKit.skip("python3 is not installed");
            return null;
        }
        p.getOutputStream().close();
        CompletableFuture<String> err = CompletableFuture.supplyAsync(() -> {
            try {
                return new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return e.toString();
            }
        });
        String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!p.waitFor(30, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new AssertionError("the AT-SPI script did not finish");
        }
        if (p.exitValue() != 0) {
            String error = err.join();
            if (error.contains("ModuleNotFoundError") || error.contains("Namespace Atspi not available")) {
                TestKit.skip("python3 has no Atspi bindings (python3-gobject and at-spi2-core)");
            }
            throw new AssertionError("the AT-SPI script failed: " + error.strip().lines()
                    .reduce((a, b) -> b).orElse(""));
        }
        return out;
    }

    @FunctionalInterface
    private interface AtspiCondition {
        boolean holds() throws Exception;
    }

    private static void awaitAtspi(String what, AtspiCondition condition) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (System.nanoTime() < deadline) {
            if (condition.holds()) {
                return;
            }
            TestKit.pause(200);
        }
        throw new AssertionError("Timed out waiting in AT-SPI for " + what);
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
    // Accessible tree

    private Node treeCard() {
        tree.setPrefHeight(220);
        tree.setShowRoot(true);
        tree.setCellFactory(tv -> new javafx.scene.control.TreeCell<>() {
            @Override
            protected void updateItem(Node node, boolean empty) {
                super.updateItem(node, empty);
                if (empty || node == null) {
                    setText(null);
                    return;
                }
                Object role = node.queryAccessibleAttribute(AccessibleAttribute.ROLE);
                Object text = node.queryAccessibleAttribute(AccessibleAttribute.TEXT);
                setText(role + (text != null && !String.valueOf(text).isBlank() ? "  \"" + text + "\"" : "")
                        + (node.isDisabled() ? "  (disabled)" : ""));
            }
        });
        tree.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null && n.getValue() != null) {
                inspect(n.getValue());
            }
        });
        tree.setAccessibleText("Accessible tree of this window");
        VBox.setVgrow(tree, Priority.ALWAYS);
        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> refreshTree());
        treeInfo.getStyleClass().add("muted");
        HBox tools = new HBox(10, refresh, treeInfo);
        tools.setAlignment(Pos.CENTER_LEFT);
        VBox box = card("Accessible tree", tree, tools);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }

    /**
     * Builds the tree of the nodes with an accessible role, skipping the
     * plain nodes and parents in between, close to the tree that assistive
     * technologies see.
     */
    private void refreshTree() {
        TreeItem<Node> root = new TreeItem<>(contentRoot);
        int[] count = {0};
        addAccessibleChildren(root, contentRoot, count);
        root.setExpanded(true);
        tree.setRoot(root);
        treeInfo.setText(count[0] + " accessible nodes");
    }

    private void addAccessibleChildren(TreeItem<Node> item, Parent parent, int[] count) {
        if (parent == tree) {
            return;
        }
        for (Node child : parent.getChildrenUnmodifiable()) {
            if (!child.isVisible()) {
                continue;
            }
            AccessibleRole role = child.getAccessibleRole();
            boolean plain = role == AccessibleRole.NODE || role == AccessibleRole.PARENT;
            TreeItem<Node> target = item;
            if (!plain) {
                target = new TreeItem<>(child);
                target.setExpanded(item.getParent() == null || count[0] < 40);
                item.getChildren().add(target);
                count[0]++;
            }
            if (child instanceof Parent p) {
                addAccessibleChildren(target, p, count);
            }
        }
    }
}
