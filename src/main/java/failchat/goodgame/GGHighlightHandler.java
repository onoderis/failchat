package failchat.goodgame;

import failchat.core.MessageHandler;

public class GGHighlightHandler implements MessageHandler <GGMessage> {
    private String appeal;

    public GGHighlightHandler(String channel) {
        this.appeal = channel + ',';
    }

    @Override
    public void handleMessage(GGMessage message) {
        if (message.getText().contains(appeal)) {
            message.setHighlighted(true);
        }
    }
}
