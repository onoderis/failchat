package failchat.handlers;

import failchat.core.Message;
import failchat.core.MessageHandler;
import org.apache.commons.lang.StringUtils;

public class CommonHighlightHandler implements MessageHandler {
    private String appeal;

    public CommonHighlightHandler(String channel) {
        this.appeal = channel + ',';
    }

    @Override
    public void handleMessage(Message message) {
        if (StringUtils.containsIgnoreCase(message.getText(), appeal)) {
            message.setHighlighted(true);
        }
    }
}
