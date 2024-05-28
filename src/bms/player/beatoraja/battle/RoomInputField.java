package bms.player.beatoraja.battle;

import bms.player.beatoraja.Resolution;
import bms.player.beatoraja.input.KeyBoardInputProcesseor;
import bms.player.beatoraja.select.MusicSelectSkin;
import bms.player.beatoraja.select.MusicSelector;
import bms.player.beatoraja.select.bar.SearchWordBar;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.FitViewport;

import java.util.logging.Logger;

public class RoomInputField extends Stage {

    private TextField search;
    /**
     * 画面クリック感知用Actor
     */
    private Group screen;

    public RoomInputField(BattleMatching matching, Rectangle rect, Resolution resolution) {
        super(new FitViewport(resolution.width, resolution.height));

        try {

            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal(matching.main.getConfig().getSystemfontpath()));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = (int) rect.height;
            parameter.incremental = true;
            BitmapFont font = generator.generateFont(parameter);

            final TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle(); // background
            textFieldStyle.font = font;
            textFieldStyle.fontColor = Color.WHITE;

            Pixmap cursorp = new Pixmap(8, 8, Pixmap.Format.RGBA8888);
            cursorp.setColor(Color.toIntBits(255, 255, 255, 255));
            cursorp.fill();
            textFieldStyle.cursor = new TextureRegionDrawable(new TextureRegion(new Texture(cursorp)));
            cursorp.dispose();

            Pixmap selectionp = new Pixmap(2, 8, Pixmap.Format.RGBA8888);
            selectionp.setColor(Color.toIntBits(255, 255, 255, 255));
            selectionp.fill();
            textFieldStyle.selection = new TextureRegionDrawable(new TextureRegion(new Texture(selectionp)));
            selectionp.dispose();

            textFieldStyle.messageFont = font;
            textFieldStyle.messageFontColor = Color.GRAY;

            search = new TextField("", textFieldStyle);
            search.setMessageText("...");
            search.setTextFieldListener((textField, key) -> {
                        String s = textField.getText();
                        if (key == '\n' || key == 13) {
                            if (s.length() == 4) {
                                matching.joinRoom(textField.getText());

                                textField.getOnscreenKeyboard().show(false);
                                setKeyboardFocus(null);
                            }

                        }
                    });
            search.setTextFieldFilter((textField, key) ->{
                // restrict input to capital alphabet
                if (key >= 'A' && key <= 'Z') {
                    return true;
                }
                if (key >= 'a' && key <= 'z') {
                    textField.appendText(String.valueOf((char)(key - 32)));
                }
                return false;

            });
            search.setBounds(rect.x, rect.y, rect.width, rect.height);
            search.setMaxLength(4);
            search.setFocusTraversal(false);

            search.setVisible(true);
            search.addListener((e) -> {
                if (e.isHandled()) {
                    matching.main.getInputProcessor().getKeyBoardInputProcesseor()
                            .setTextInputMode(getKeyboardFocus() != null);
                }
                return false;
            });

            screen = new Group();
            screen.setBounds(0, 0, resolution.width, resolution.height);
            screen.addListener(new ClickListener() {
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    if (getKeyboardFocus() != null && !rect.contains(x, y)) {
                        unfocus(matching);
                    }
                    return false;
                }
            });
            screen.addActor(search);
            addActor(screen);
        } catch (GdxRuntimeException e) {
            Logger.getGlobal().warning("Search Text読み込み失敗");
        }
    }

    public void unfocus(BattleMatching matching) {
        if(search != null) {
            search.setText("");
            search.setMessageText("search song");
            search.getStyle().messageFontColor = Color.GRAY;
            search.getOnscreenKeyboard().show(false);
        }
        setKeyboardFocus(null);
        matching.main.getInputProcessor().getKeyBoardInputProcesseor().setTextInputMode(false);
    }

    public void dispose() {
//		super.dispose();
    }

    public Rectangle getSearchBounds() {
        return search != null ? new Rectangle(search.getX(), search.getY(), search.getWidth(), search.getHeight()) : null;
    }
}