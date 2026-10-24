/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

import com.almasb.fxgl.app.ApplicationMode;
import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.component.Component;
import com.almasb.fxgl.particle.ParticleComponent;
import com.almasb.fxgl.particle.ParticleEmitter;
import com.almasb.fxgl.particle.ParticleEmitters;
import javafx.geometry.Point2D;
import javafx.scene.effect.BlendMode;
import javafx.scene.effect.Bloom;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

import java.util.Map;

import static com.almasb.fxgl.dsl.FXGL.*;

/**
 * A neon Breakout made with FXGL (https://github.com/AlmasB/FXGL), the game
 * engine for JavaFX: its entities, collisions, input, game variables,
 * particles, animations and menus all run on the GTK 4 glass. The ./gradlew
 * fxgl task downloads FXGL, which launcher.sh then puts on the class path.
 * <p>
 * The mouse or A/D and the arrows move the paddle, the click or Space
 * launches the ball and Escape opens the game menu. -Dgames.autoplay=true
 * skips the main menu and plays by itself.
 */
public class GamesDemo extends GameApplication {

    enum Type { PADDLE, BALL, BRICK }

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 800;
    private static final int COLUMNS = 12;
    private static final int ROWS = 7;
    private static final double BRICK_W = 92;
    private static final double BRICK_H = 28;
    private static final double GAP = 8;
    private static final double TOP = 110;
    private static final double PADDLE_W = 150;
    private static final double PADDLE_Y = HEIGHT - 70;
    private static final double BALL_R = 9;
    private static final Color[] ROW_COLORS = {
        Color.web("#ff4d8d"), Color.web("#ff7a45"), Color.web("#ffc53d"), Color.web("#73d13d"),
        Color.web("#36cfc9"), Color.web("#4096ff"), Color.web("#9254de"),
    };

    private static final boolean AUTOPLAY = Boolean.getBoolean("games.autoplay");

    private Entity paddle;
    private Entity ball;

    public static void main(String[] args) {
        launch(GamesDemo.class, args);
    }

    @Override
    protected void initSettings(GameSettings settings) {
        settings.setTitle("Neon Breakout");
        settings.setVersion("1.0");
        settings.setApplicationMode(ApplicationMode.RELEASE);
        settings.setWidth(WIDTH);
        settings.setHeight(HEIGHT);
        settings.setMainMenuEnabled(!AUTOPLAY);
        settings.setGameMenuEnabled(true);
        settings.setManualResizeEnabled(true);
        settings.setPreserveResizeRatio(true);
    }

    @Override
    protected void initGameVars(Map<String, Object> vars) {
        vars.put("score", 0);
        vars.put("lives", 3);
        vars.put("level", 1);
    }

    @Override
    protected void initInput() {
        onKey(KeyCode.A, "Left", () -> movePaddle(-14));
        onKey(KeyCode.LEFT, "Left (arrow)", () -> movePaddle(-14));
        onKey(KeyCode.D, "Right", () -> movePaddle(14));
        onKey(KeyCode.RIGHT, "Right (arrow)", () -> movePaddle(14));
        onKeyDown(KeyCode.SPACE, "Launch", this::launchBall);
        onBtnDown(MouseButton.PRIMARY, "Launch (mouse)", this::launchBall);
    }

    @Override
    protected void initGame() {
        entityBuilder()
                .at(0, 0)
                .zIndex(-10)
                .view(new Rectangle(WIDTH, HEIGHT, new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                        new Stop(0, Color.web("#0b1026")), new Stop(1, Color.web("#1a0b2e")))))
                .buildAndAttach();
        for (int i = 0; i < 90; i++) {
            Circle star = new Circle(random(0.6, 1.8), Color.color(1, 1, 1, random(0.2, 0.8)));
            entityBuilder().at(random(0, WIDTH), random(0, HEIGHT)).zIndex(-9).view(star).buildAndAttach();
            animationBuilder()
                    .duration(Duration.seconds(random(1.0, 3.0)))
                    .repeatInfinitely()
                    .autoReverse(true)
                    .fadeOut(star)
                    .buildAndPlay();
        }

        Rectangle paddleView = new Rectangle(PADDLE_W, 16, Color.web("#e6f4ff"));
        paddleView.setArcWidth(16);
        paddleView.setArcHeight(16);
        paddleView.setEffect(new DropShadow(24, Color.web("#40a9ff")));
        paddle = entityBuilder()
                .type(Type.PADDLE)
                .at((WIDTH - PADDLE_W) / 2, PADDLE_Y)
                .viewWithBBox(paddleView)
                .collidable()
                .buildAndAttach();

        Circle ballView = new Circle(BALL_R, BALL_R, BALL_R, Color.WHITE);
        ballView.setEffect(new DropShadow(20, Color.web("#ffd666")));
        ball = entityBuilder()
                .type(Type.BALL)
                .viewWithBBox(ballView)
                .collidable()
                .with(new BallComponent())
                .buildAndAttach();

        buildLevel();
    }

    @Override
    protected void initPhysics() {
        onCollisionBegin(Type.BALL, Type.PADDLE, (b, p) -> {
            // The farther from the center, the wider the angle
            double offset = (b.getCenter().getX() - p.getCenter().getX()) / (PADDLE_W / 2);
            ball().bounceOffPaddle(offset);
            pulse(p);
        });
        onCollisionBegin(Type.BALL, Type.BRICK, (b, brick) -> {
            ball().bounceOffBrick(brick);
            breakBrick(brick);
        });
    }

    @Override
    protected void initUI() {
        Text score = getUIFactoryService().newText("", Color.WHITE, 26);
        score.textProperty().bind(getip("score").asString("Score  %d"));
        Text lives = getUIFactoryService().newText("", Color.web("#ff85c0"), 26);
        lives.textProperty().bind(getip("lives").asString("Lives  %d"));
        Text level = getUIFactoryService().newText("", Color.web("#87e8de"), 26);
        level.textProperty().bind(getip("level").asString("Level  %d"));
        VBox hud = new VBox(4, score, lives, level);
        addUINode(hud, 24, 16);

        Text title = getUIFactoryService().newText("NEON BREAKOUT", Color.WHITE, 40);
        title.setEffect(new Bloom(0.2));
        addUINode(title, WIDTH - 380, 54);
        Text hint = getUIFactoryService().newText("Click or Space to launch", Color.color(1, 1, 1, 0.55), 18);
        addUINode(hint, WIDTH - 300, 84);
    }

    @Override
    protected void onUpdate(double tpf) {
        if (AUTOPLAY) {
            // The paddle follows the ball, a little off center to vary the angles
            launchBall();
            double offset = Math.sin(getGameTimer().getNow() * 0.7) * PADDLE_W * 0.3;
            paddle.setX(clamp(ball.getCenter().getX() - PADDLE_W / 2 + offset));
            return;
        }
        // The paddle follows the mouse while it is over the game
        double mouseX = getInput().getMouseXWorld();
        if (mouseX > 0 && mouseX < WIDTH && Math.abs(mouseX - paddle.getCenter().getX()) > 1) {
            paddle.setX(clamp(mouseX - PADDLE_W / 2));
        }
    }

    private void movePaddle(double dx) {
        paddle.setX(clamp(paddle.getX() + dx));
    }

    private static double clamp(double x) {
        return Math.max(0, Math.min(WIDTH - PADDLE_W, x));
    }

    private BallComponent ball() {
        return ball.getComponent(BallComponent.class);
    }

    private void launchBall() {
        ball().launch(geti("level"));
    }

    private void buildLevel() {
        double left = (WIDTH - COLUMNS * BRICK_W - (COLUMNS - 1) * GAP) / 2;
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                Color color = ROW_COLORS[row];
                Rectangle view = new Rectangle(BRICK_W, BRICK_H, new LinearGradient(0, 0, 0, 1, true,
                        CycleMethod.NO_CYCLE, new Stop(0, color.brighter()), new Stop(1, color.darker())));
                view.setArcWidth(8);
                view.setArcHeight(8);
                view.setEffect(new DropShadow(12, color));
                Entity brick = entityBuilder()
                        .type(Type.BRICK)
                        .at(left + col * (BRICK_W + GAP), TOP + row * (BRICK_H + GAP))
                        .viewWithBBox(view)
                        .collidable()
                        .with("color", color)
                        .with("points", (ROWS - row) * 10)
                        .buildAndAttach();
                // The bricks drop in, row after row
                animationBuilder()
                        .delay(Duration.millis(row * 70 + col * 15))
                        .duration(Duration.millis(450))
                        .translate(brick)
                        .from(new Point2D(brick.getX(), brick.getY() - 400))
                        .to(brick.getPosition())
                        .buildAndPlay();
            }
        }
    }

    private void breakBrick(Entity brick) {
        Color color = brick.getObject("color");
        inc("score", brick.getInt("points"));
        explode(brick.getCenter(), color);
        brick.removeFromWorld();
        if (byType(Type.BRICK).isEmpty()) {
            inc("level", 1);
            ball().reset();
            getNotificationService().pushNotification("Level " + geti("level"));
            runOnce(this::buildLevel, Duration.seconds(0.6));
        }
    }

    private static void explode(Point2D at, Color color) {
        ParticleEmitter emitter = ParticleEmitters.newExplosionEmitter(60);
        emitter.setStartColor(color);
        emitter.setEndColor(Color.TRANSPARENT);
        emitter.setBlendMode(BlendMode.ADD);
        emitter.setSize(2, 6);
        emitter.setMaxEmissions(1);
        Entity sparks = entityBuilder().at(at).with(new ParticleComponent(emitter)).buildAndAttach();
        runOnce(sparks::removeFromWorld, Duration.seconds(1.5));
    }

    private static void pulse(Entity e) {
        animationBuilder()
                .duration(Duration.millis(120))
                .autoReverse(true)
                .repeat(2)
                .scale(e)
                .from(new Point2D(1, 1))
                .to(new Point2D(1.08, 1.3))
                .buildAndPlay();
    }

    private void loseLife() {
        inc("lives", -1);
        explode(ball.getCenter(), Color.web("#ff4d4f"));
        if (geti("lives") > 0) {
            ball().reset();
            return;
        }
        ball().reset();
        getDialogService().showMessageBox("Game over! Score: " + geti("score"), () -> {
            set("score", 0);
            set("lives", 3);
            set("level", 1);
            byType(Type.BRICK).forEach(Entity::removeFromWorld);
            buildLevel();
        });
    }

    /**
     * Moves the ball, which waits on the paddle until it is launched, and
     * bounces it off the walls; losing it below the paddle costs a life.
     */
    private class BallComponent extends Component {
        private Point2D velocity = Point2D.ZERO;
        private boolean launched;

        @Override
        public void onUpdate(double tpf) {
            if (!launched) {
                entity.setPosition(paddle.getCenter().getX() - BALL_R, PADDLE_Y - 2 * BALL_R - 2);
                return;
            }
            entity.translate(velocity.multiply(tpf));
            if (entity.getX() < 0 && velocity.getX() < 0 || entity.getRightX() > WIDTH && velocity.getX() > 0) {
                velocity = new Point2D(-velocity.getX(), velocity.getY());
            }
            if (entity.getY() < 0 && velocity.getY() < 0) {
                velocity = new Point2D(velocity.getX(), -velocity.getY());
            }
            if (entity.getY() > HEIGHT) {
                loseLife();
            }
        }

        void launch(int level) {
            if (!launched) {
                launched = true;
                double speed = 520 + 60 * (level - 1);
                velocity = new Point2D(random(-0.5, 0.5), -1).normalize().multiply(speed);
            }
        }

        void reset() {
            launched = false;
            velocity = Point2D.ZERO;
        }

        void bounceOffPaddle(double offset) {
            if (velocity.getY() > 0) {
                double angle = Math.toRadians(Math.max(-1, Math.min(1, offset)) * 60);
                double speed = velocity.magnitude();
                velocity = new Point2D(Math.sin(angle), -Math.cos(angle)).multiply(speed);
            }
        }

        void bounceOffBrick(Entity brick) {
            // Off the side the ball overlaps least
            double overlapX = Math.min(entity.getRightX() - brick.getX(), brick.getRightX() - entity.getX());
            double overlapY = Math.min(entity.getBottomY() - brick.getY(), brick.getBottomY() - entity.getY());
            if (overlapX < overlapY) {
                velocity = new Point2D(-velocity.getX(), velocity.getY());
            } else {
                velocity = new Point2D(velocity.getX(), -velocity.getY());
            }
        }
    }
}
