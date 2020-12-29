/*
 * Copyright (c) 2020. Laurent Réveillère
 */

package fr.ubx.poo.engine;

import fr.ubx.poo.game.Direction;
import fr.ubx.poo.game.Game;
import fr.ubx.poo.model.go.character.Monster;
import fr.ubx.poo.model.go.character.Player;
import fr.ubx.poo.view.sprite.Sprite;
import fr.ubx.poo.view.sprite.SpriteFactory;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;


public final class GameEngine {

    private static AnimationTimer gameLoop;
    private final String windowTitle;
    private final Game game;
    private final Player player;
    private final List<Sprite> sprites = new ArrayList<>();
    private StatusBar statusBar;
    private Pane layer;
    private Input input;
    private Stage stage;
    private Sprite spritePlayer;
    private List<Sprite> spriteBombs= new ArrayList<>();

    private Sprite spriteMonster;
    private final Monster monster;

    public GameEngine(final String windowTitle, Game game, final Stage stage) {
        this.windowTitle = windowTitle;
        this.game = game;
        this.player = game.getPlayer();
        this.monster = game.getMonster();
        initialize(stage, game);
        buildAndSetGameLoop();
    }

    private void initialize(Stage stage, Game game) {
        this.stage = stage;
        Group root = new Group();
        layer = new Pane();

        int height = game.getWorld().dimension.height;
        int width = game.getWorld().dimension.width;
        int sceneWidth = width * Sprite.size;
        int sceneHeight = height * Sprite.size;
        Scene scene = new Scene(root, sceneWidth, sceneHeight + StatusBar.height);
        scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());

        stage.setTitle(windowTitle);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        input = new Input(scene);
        root.getChildren().add(layer);
        statusBar = new StatusBar(root, sceneWidth, sceneHeight, game);
        // Create decor sprites
        game.getWorld().forEach( (pos,d) -> sprites.add(SpriteFactory.createDecor(layer, pos, d)));
        spritePlayer = SpriteFactory.createPlayer(layer, player);
        spriteMonster = SpriteFactory.createMonster(layer, monster);
    }

    protected final void buildAndSetGameLoop() {
        gameLoop = new AnimationTimer() {
            public void handle(long now) {
                // Check keyboard actions
                processInput(now);

                // Do actions
                update(now);

                // Graphic update
                render();
                statusBar.update(game);
            }
        };
    }

    private void processInput(long now) {
        if (input.isExit()) {
            gameLoop.stop();
            Platform.exit();
            System.exit(0);
        }
        if (input.isMoveDown()) {
            player.requestMove(Direction.S);
        }
        if (input.isMoveLeft()) {
            player.requestMove(Direction.W);
        }
        if (input.isMoveRight()) {
            player.requestMove(Direction.E);
        }
        if (input.isMoveUp()) {
            player.requestMove(Direction.N);
        }
        if (input.isBomb()){
            player.requestBomb();
        }
        input.clear();
    }

    private void showMessage(String msg, Color color) {
        Text waitingForKey = new Text(msg);
        waitingForKey.setTextAlignment(TextAlignment.CENTER);
        waitingForKey.setFont(new Font(60));
        waitingForKey.setFill(color);
        StackPane root = new StackPane();
        root.getChildren().add(waitingForKey);
        Scene scene = new Scene(root, 400, 200, Color.WHITE);
        stage.setTitle(windowTitle);
        stage.setScene(scene);
        input = new Input(scene);
        stage.show();
        new AnimationTimer() {
            public void handle(long now) {
                processInput(now);
            }
        }.start();
    }


    private void update(long now) {
        player.update(now);
        monster.update(now, 1000);

        if(player.getPosition().x == monster.getPosition().x && player.getPosition().y == monster.getPosition().y){
            player.hurt();
        }

        if(game.getWorld().update) {
            /*for(int x=0 ; x<sprites.size() ; x++){
                if(sprites.get(x) instanceof SpriteDecor && sprites.get(x) != null && game.getWorld().get(sprites.get(x).getPosition()) == null){
                    sprites.get(x).setImage(ImageFactory.getInstance().get(ImageResource.FLOOR));
                    //sprites.get(x).remove();
                }
                if(sprites.get(x) instanceof SpriteDoor && sprites.get(x) != null && game.getWorld().get(sprites.get(x).getPosition()) instanceof DoorNextOpened){
                    ((SpriteDoor) sprites.get(x)).setOpen(true);
                }
                if(sprites.get(x) instanceof SpriteDecor && sprites.get(x) !=null && game.getWorld().get(sprites.get(x).getPosition()) instanceof Box){
                    ((SpriteDecor) sprites.get(x)).setIsBox(true);
                }
                if(sprites.get(x) instanceof SpriteDecor && sprites.get(x) !=null && game.getWorld().get(sprites.get(x).getPosition()) instanceof Floor){
                    ((SpriteDecor) sprites.get(x)).setIsFloor(true);
                }

            }*/
            sprites.forEach(sprite -> sprite.remove());
            sprites.clear();
            game.getWorld().forEach( (pos,d) -> sprites.add(SpriteFactory.createDecor(layer, pos, d)));

            game.getWorld().update = false;
        }
        if (game.getWorld().getPlacedBombs().isEmpty()==false){
            if(game.getWorld().getPlacedBombs().size()!=spriteBombs.size()){
                for(int x=0;x<game.getWorld().getPlacedBombs().size();x++){
                    if(game.getWorld().getPlacedBombs().get(x).getBombTimer()<4){
                       //ne se passe rien
                    }
                    else{
                        spriteBombs.add(SpriteFactory.createBombSprite(layer,game.getWorld().getPlacedBombs().get(x)));
                    }
                }
            }
        }

        if (player.isAlive() == false) {
            gameLoop.stop();
            showMessage("Perdu!", Color.RED);
        }
        if (player.isWinner()) {
            gameLoop.stop();
            showMessage("Gagné", Color.BLUE);
        }
    }

    private void render() {
        sprites.forEach(Sprite::render);
        spriteBombs.forEach(Sprite::render);
        // last rendering to have player in the foreground
        spriteMonster.render();
        spritePlayer.render();
    }

    public void start() {
        gameLoop.start();
    }
}
