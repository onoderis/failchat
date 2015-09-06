package failchat.funstream;

import failchat.core.MessageFilter;

import java.util.logging.Logger;

public class AnnounceMessageFilter implements MessageFilter<FsMessage> {
    private static final Logger logger = Logger.getLogger(AnnounceMessageFilter.class.getName());
    private static final String ANNOUNCE_MARKER = "начал трансляцию";

    @Override
    public boolean filterMessage(FsMessage message) {
        if (message.getFrom().getId() == 0 && message.getType().equals("donate") && message.getText().contains(ANNOUNCE_MARKER)) {
            logger.fine("Announce message filtered: " + message.getText());
            return true;
        } else {
            return false;
        }
    }
}
