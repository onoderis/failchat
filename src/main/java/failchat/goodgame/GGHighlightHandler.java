package failchat.goodgame;

import failchat.core.MessageHandler;
import org.apache.commons.lang.StringUtils;

public class GGHighlightHandler implements MessageHandler <GGMessage> {
    private String appeal;

    public GGHighlightHandler(String channel) {
        this.appeal = channel + ',';
    }

    @Override
    public void handleMessage(GGMessage message) {
        if (StringUtils.containsIgnoreCase(message.getText(), appeal)) {
            message.setHighlighted(true);
        }
    }
}
