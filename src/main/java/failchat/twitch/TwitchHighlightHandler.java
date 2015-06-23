package failchat.twitch;

import failchat.core.MessageHandler;
import org.apache.commons.lang.StringUtils;

public class TwitchHighlightHandler implements MessageHandler<TwitchMessage> {
    private String appeal;

    public TwitchHighlightHandler(String channel) {
        this.appeal = '@' + channel;
    }

    @Override
    public void handleMessage(TwitchMessage message) {
        if (StringUtils.containsIgnoreCase(message.getText(), appeal)) {
            message.setHighlighted(true);
        }
    }
}
