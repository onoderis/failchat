package failchat.sc2tv;

import failchat.core.Message;
import failchat.core.MessageHandler;

public class Sc2tvHighlightHandler implements MessageHandler {
    private String appeal;

    public Sc2tvHighlightHandler(String channel) {
        this.appeal = channel + ',';
    }

    @Override
    public void handleMessage(Message message) {
        if (message.getText().contains(appeal)) {
            message.setHighlighted(true);
        }
    }
}
