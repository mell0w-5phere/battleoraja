package bms.player.beatoraja.battle;

import bms.player.beatoraja.*;
import bms.player.beatoraja.MainController.IRStatus;
import bms.player.beatoraja.config.KeyConfigurationSkin;
import bms.player.beatoraja.input.BMSPlayerInputProcessor;
import bms.player.beatoraja.input.KeyBoardInputProcesseor;
import bms.player.beatoraja.input.KeyBoardInputProcesseor.ControlKeys;
import bms.player.beatoraja.ir.IRPlayerData;
import bms.player.beatoraja.skin.Skin;
import bms.player.beatoraja.skin.SkinHeader;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.util.logging.Logger;

public class BattleMatching extends MainState {
    private BitmapFont font;
    private BMSPlayerInputProcessor input;
    private KeyBoardInputProcesseor keyin;
    private MatchState matchState;
    private IRStatus battleIrStatus;
    private BattleConnection battleIrConn;
    private int curIndex;
    private IRPlayerData opponent;
    private String roomKey;
    private RoomInputField roomInput;
    public BattleMatching(MainController main) {
        super(main);
    }
    private Skin skin;

    public void create() {
        if (getSkin() == null) {
            SkinHeader header = new SkinHeader();
            header.setSourceResolution(Resolution.HD);
            header.setDestinationResolution(main.getConfig().getResolution());
            this.setSkin(new BattleMatchingSkin(header));
        }
        skin = getSkin();

        try {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(
                    Gdx.files.internal(main.getConfig().getSystemfontpath()));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = (int) (40 * skin.getScaleY());
            font = generator.generateFont(parameter);
            generator.dispose();
        } catch (GdxRuntimeException e) {
            Logger.getGlobal().severe("Font読み込み失敗");
        }
        input = main.getInputProcessor();
        keyin = input.getKeyBoardInputProcesseor();
        IRStatus[] irs = main.getIRStatus();
        for (IRStatus ir : irs) {
            if (ir.connection instanceof BattleConnection) {
                battleIrStatus = ir;
                battleIrConn = (BattleConnection) ir.connection;
                break;
            }
        }
        float y = main.getConfig().getWindowHeight() - 135;
        roomInput = new RoomInputField(this, new Rectangle(400, y, 400, 40), skin.header.getSourceResolution());
        setStage(roomInput);
        curIndex = 0;
        matchState = MatchState.IDLE;
    }
    public void drawFont(SpriteBatch sprite, String str, float x, float y) {
        font.draw(sprite, str, x * (float)skin.getScaleX(), y * (float)skin.getScaleY());
    }

    public void drawRect(ShapeRenderer sr, float x, float y, float w, float h, Color c, ShapeRenderer.ShapeType s) {
        float sx = (float)skin.getScaleX();
        float sy = (float)skin.getScaleY();
        sr.setColor(c);
        sr.set(s);
        sr.rect(x * sx, y* sy, w*sx, h*sy);
    }

    public void render() {
        SpriteBatch sprite = main.getSpriteBatch();
        ShapeRenderer sr = new ShapeRenderer();
        sr.setAutoShapeType(true);

        if (battleIrConn == null) {
            sprite.begin();
            sprite.setColor(Color.RED);
            drawFont(sprite, "Cannot connect to IR which supports battle mode.", 100, 100);
            sprite.end();
            return;
        }
        float lineTopY = main.getConfig().getWindowHeight() - 10;
        sprite.setColor(Color.WHITE);
        sprite.begin();
        drawFont(sprite, String.format("YOU: %s", battleIrStatus.player.name), 10, lineTopY);
        sprite.end();

        int lineCnt = 0;
        switch (matchState) {
            case IDLE:
                // cursor
                sr.begin();
                drawRect(sr, 10, lineTopY - 45 * (curIndex + 2), 100, 45, Color.BLUE, ShapeRenderer.ShapeType.Filled);
                Rectangle boxRect = roomInput.getSearchBounds();
                drawRect(sr, boxRect.x, boxRect.y, boxRect.width, boxRect.height, Color.GRAY, ShapeRenderer.ShapeType.Line);
                sr.end();

                sprite.begin();
                drawFont(sprite, "Create New Room", 10, lineTopY - 45 * ++lineCnt);
                drawFont(sprite, "Join Existing Room", 10, lineTopY - 45 * ++lineCnt);
                drawFont(sprite, "bypass", 10, lineTopY - 45 * ++lineCnt);
                sprite.end();

                if (input.isControlKeyPressed(ControlKeys.UP)) {
                    curIndex--;
                }
                if (input.isControlKeyPressed(ControlKeys.DOWN)) {
                    curIndex++;
                }
                curIndex = (curIndex + 3) % 3;

                if (input.isControlKeyPressed(ControlKeys.ENTER)) {
                    if (curIndex == 0) {
                        if (battleIrConn.createRoom()) {
                            setStage(null);
                            matchState = MatchState.WAITING;
                            roomKey = battleIrConn.getRoomKey();
                        } else {
                            main.getMessageRenderer().addMessage("nope", Color.RED, 0);
                        }
                    } else if (curIndex == 1) {
                        // TODO
                    } else if(curIndex == 2) {
                        matchState = MatchState.READY;
                    }
                }
                break;
            case WAITING:
                IRPlayerData opp = battleIrConn.getOpponent();
                sprite.begin();
                drawFont(sprite, "This room's keyphrase: " + roomKey, 10, lineTopY - 45 * ++lineCnt);
                drawFont(sprite, "Waiting for the opponent...", 10, lineTopY - 45 * ++lineCnt);
                sprite.end();

                if (opp != null) {
                    // opponent found
                    main.getMessageRenderer().addMessage("Opponent found", 1000, Color.GREEN, 0);
                    opponent = opp;
                    battleIrConn.sendSongList(main.getSongDatabase().getSongDatas("mode", "7"));
                    matchState = MatchState.MATCHED;
                }
                break;
            case MATCHED:
                sprite.begin();
                drawFont(sprite, "Opponent: "+opponent.name, 10, lineTopY - 45 * ++lineCnt);
                sprite.end();
                break;
            case READY:

                if (input.isControlKeyPressed(ControlKeys.ENTER)) {
                    main.changeState(MainStateType.MUSICSELECT);
                }
                break;
        }
    }

    private enum MatchState {
        IDLE, WAITING, MATCHED, READY;
    }

    public void joinRoom(String roomKey) {
        curIndex = 1;
        if(battleIrConn.joinRoom(roomKey)) {
            setStage(null);
            matchState = MatchState.WAITING;
        } else {
            main.getMessageRenderer().addMessage("ohooo", 10000, Color.RED, 0);
        }
    }
}
