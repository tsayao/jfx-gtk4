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
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/** A colorful animated desktop background, to make a blurred backdrop visible. */
public class Wallpaper extends Application {
    public static void main(String[] a) { launch(a); }

    public void start(Stage stage) {
        Pane blobs = new Pane();
        String[] colors = {"#f43f5e", "#38bdf8", "#a855f7", "#22c55e", "#f59e0b", "#06b6d4"};
        for (int i = 0; i < colors.length; i++) {
            Circle c = new Circle(260, new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
                    new Stop(0, Color.web(colors[i])), new Stop(1, Color.web(colors[i], 0))));
            c.setCenterX(200 + i * 210);
            c.setCenterY(200 + (i % 2) * 400);
            TranslateTransition t = new TranslateTransition(Duration.seconds(6 + i), c);
            t.setByX((i % 2 == 0 ? 1 : -1) * 380);
            t.setByY((i % 3 == 0 ? 1 : -1) * 220);
            t.setAutoReverse(true);
            t.setCycleCount(Animation.INDEFINITE);
            t.play();
            blobs.getChildren().add(c);
        }
        VBox words = new VBox(-10);
        words.setAlignment(Pos.CENTER);
        for (String w : new String[] {"WAYLAND", "GTK 4", "VULKAN", "JAVAFX"}) {
            Text t = new Text(w);
            t.setFont(Font.font("Adwaita Sans", FontWeight.BLACK, 150));
            t.setFill(Color.web("#ffffff", 0.85));
            words.getChildren().add(t);
        }
        StackPane root = new StackPane(blobs, words);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f172a, #1e1b4b, #0c4a6e);");
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setScene(new Scene(root, 1400, 900));
        stage.setMaximized(true);
        stage.setTitle("Wallpaper");
        stage.show();
    }
}
