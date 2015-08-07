package failchat.funstream;

import failchat.core.MessageFilter;

/**
 * Фильтруент сообщения с других источников
 * */
public class SourceFilter implements MessageFilter<FsMessage> {
    @Override
    public boolean filterMessage(FsMessage message) {
        return message.getFrom().getId() < 0 || message.getTo() == null;
    }
}
