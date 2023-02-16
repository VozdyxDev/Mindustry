package io.anuke.mindustry.ui.fragments;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState.State;
import io.anuke.mindustry.io.Platform;
import io.anuke.mindustry.net.Net;
import io.anuke.mindustry.net.NetEvents;
import io.anuke.ucore.core.Core;
import io.anuke.ucore.core.Inputs;
import io.anuke.ucore.core.Timers;
import io.anuke.ucore.scene.ui.Label;
import io.anuke.ucore.scene.ui.Label.LabelStyle;
import io.anuke.ucore.scene.ui.TextField;
import io.anuke.ucore.scene.ui.layout.Table;
import io.anuke.ucore.scene.ui.layout.Unit;
import io.anuke.ucore.util.Log;

import static io.anuke.mindustry.Vars.state;
import static io.anuke.ucore.core.Core.scene;
import static io.anuke.ucore.core.Core.skin;

public class ChatFragment extends Table implements Fragment{
    private final static int messagesShown = 10;
    private final static int maxLength = 150;
    private Array<ChatMessage> messages = new Array<>();
    private float fadetime, lastfade;
    private boolean chatOpen = false;
    private TextField chatfield;
    private Label fieldlabel = new Label(">");
    private BitmapFont font;
    private GlyphLayout layout = new GlyphLayout();
    private float offsetx = Unit.dp.scl(4), offsety = Unit.dp.scl(4), fontoffsetx = Unit.dp.scl(2), chatspace = Unit.dp.scl(50);
    private float textWidth = Unit.dp.scl(600);
    private Color shadowColor = new Color(0, 0, 0, 0.4f);
    private float textspacing = Unit.dp.scl(10);

    public ChatFragment(){
        super();

        setFillParent(true);
        font = Core.skin.getFont("default-font");

        setVisible(() -> !state.is(State.menu) && Net.active());

        //TODO put it in input?
        update(() -> {
            if(!Net.active() && chatOpen){
                hide();
            }

            if(Net.active() && Inputs.keyTap("chat")){
                toggle();
            }
        });

        setup();
    }

    @Override
    public void build() {
        scene.add(this);
    }

    public void clearMessages(){
        messages.clear();
    }

    private void setup(){
        fieldlabel.setStyle(new LabelStyle(fieldlabel.getStyle()));
        fieldlabel.getStyle().font = font;
        fieldlabel.setStyle(fieldlabel.getStyle());

        chatfield = new TextField("", new TextField.TextFieldStyle(skin.get(TextField.TextFieldStyle.class)));
        chatfield.setTextFieldFilter((field, c) -> field.getText().length() < maxLength);
        chatfield.getStyle().background = null;
        chatfield.getStyle().fontColor = Color.WHITE;
        chatfield.getStyle().font = skin.getFont("default-font-chat");
        chatfield.setStyle(chatfield.getStyle());
        Platform.instance.addDialog(chatfield, maxLength);

        bottom().left().marginBottom(offsety).marginLeft(offsetx*2).add(fieldlabel).padBottom(4f);

        add(chatfield).padBottom(offsety).padLeft(offsetx).growX().padRight(offsetx).height(28);

        if(Vars.android) {
            marginBottom(105f);
            marginRight(240f);
        }

        if(Vars.android) {
            addImageButton("icon-arrow-right", 14 * 2, this::toggle).size(46f, 51f).visible(() -> chatOpen).pad(2f);
        }
    }

    @Override
    public void draw(Batch batch, float alpha){

        batch.setColor(shadowColor);

        if(chatOpen)
            batch.draw(skin.getRegion("white"), offsetx, chatfield.getY(), chatfield.getWidth() + 15f, chatfield.getHeight()-1);

        super.draw(batch, alpha);

        float spacing = chatspace;

        chatfield.setVisible(chatOpen);
        fieldlabel.setVisible(chatOpen);

        batch.setColor(shadowColor);

        float theight = offsety + spacing + getMarginBottom();
        for(int i = 0; i < messagesShown && i < messages.size && i < fadetime; i ++){

            layout.setText(font, messages.get(i).formattedMessage, Color.WHITE, textWidth, Align.bottomLeft, true);
            theight += layout.height+textspacing;
            if(i == 0) theight -= textspacing+1;

            font.getCache().clear();
            font.getCache().addText(messages.get(i).formattedMessage, fontoffsetx + offsetx, offsety + theight, textWidth, Align.bottomLeft, true);

            if(fadetime-i < 1f && fadetime-i >= 0f){
                font.getCache().setAlphas(fadetime-i);
                batch.setColor(0, 0, 0, shadowColor.a*(fadetime-i));
            }

            batch.draw(skin.getRegion("white"), offsetx, theight-layout.height-2, textWidth + Unit.dp.scl(4f), layout.height+textspacing);
            batch.setColor(shadowColor);

            font.getCache().draw(batch);
        }

        batch.setColor(Color.WHITE);

        if(fadetime > 0 && !chatOpen)
            fadetime -= Timers.delta()/180f;
    }

    private void sendMessage(){
        String message = chatfield.getText();
        chatfield.clearText();

        if(message.replaceAll(" ", "").isEmpty()) return;

        NetEvents.handleSendMessage(message);
    }

    public void toggle(){

        if(!chatOpen){
            scene.setKeyboardFocus(chatfield);
            chatfield.fireClick();
            chatOpen = !chatOpen;
            lastfade = fadetime;
            fadetime = messagesShown + 1;
        }else{
            scene.setKeyboardFocus(null);
            chatOpen = !chatOpen;
            fadetime = lastfade;
            sendMessage();
        }
    }

    public void hide(){
        scene.setKeyboardFocus(null);
        chatOpen = false;
        chatfield.setText("");
    }

    public boolean chatOpen(){
        return chatOpen;
    }

    public int getMessagesSize(){
        return messages.size;
    }

    public void addMessage(String message, String sender){
        messages.insert(0, new ChatMessage(message, sender));

        fadetime += 1f;
        fadetime = Math.min(fadetime, messagesShown) + 1f;
    }

    private static class ChatMessage{
        public final String sender;
        public final String message;
        public final String formattedMessage;

        public ChatMessage(String message, String sender){
            this.message = message;
            this.sender = sender;
            if(sender == null){ //no sender, this is a server message?
                formattedMessage = message;
            }else{
                formattedMessage = "[CORAL][["+sender+"[CORAL]]:[WHITE] "+message;
            }
        }
    }

}
