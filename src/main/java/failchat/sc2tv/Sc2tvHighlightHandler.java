package failchat.sc2tv;

import failchat.core.Message;
import failchat.core.MessageHandler;
import org.apache.commons.lang.StringUtils;

public class Sc2tvHighlightHandler implements MessageHandler {
    private String appeal;

    public Sc2tvHighlightHandler(String channel) {
        this.appeal = channel + ',';
    }

    @Override
    public void handleMessage(Message message) {
        if (StringUtils.containsIgnoreCase(message.getText(), appeal)) {
            message.setHighlighted(true);
        }
    }
}
