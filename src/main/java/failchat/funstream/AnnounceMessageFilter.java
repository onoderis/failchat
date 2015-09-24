package failchat.funstream;

import failchat.core.MessageFilter;

import java.util.logging.Logger;

public class AnnounceMessageFilter implements MessageFilter<FsMessage> {
    private static final Logger logger = Logger.getLogger(AnnounceMessageFilter.class.getName());

    @Override
    public boolean filterMessage(FsMessage message) {
        if (message.getType().equals("announce")) {
            logger.fine("Announce message filtered: " + message.getText());
            return true;
        } else {
            return false;
        }
    }
}
