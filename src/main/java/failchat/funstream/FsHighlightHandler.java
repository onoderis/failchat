package failchat.funstream;

import failchat.core.MessageHandler;
import org.apache.commons.lang.StringUtils;

public class FsHighlightHandler implements MessageHandler<FsMessage> {
    private String channelName;

    public FsHighlightHandler(String channel) {
        this.channelName = channel;
    }

    @Override
    public void handleMessage(FsMessage message) {
        if (!message.getTo().getName().equals("")) {
            message.setText(message.getTo().getName() + ", " + message.getText());
            if (StringUtils.equalsIgnoreCase(message.getTo().getName(), channelName)) {
                message.setHighlighted(true);
            }
        }
    }
}
