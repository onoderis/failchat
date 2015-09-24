package failchat.core;

import failchat.funstream.FsMessage;
import failchat.goodgame.GGMessage;
import failchat.twitch.TwitchMessage;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

public class MessageHistory {
    private static volatile MessageHistory instance;
    private static final int CAPACITY = 60;

    private Deque<Message> history = new LinkedList<>();

    private MessageHistory() {}

    public static MessageHistory getInstance() {
        MessageHistory localInstance = instance;
        if (localInstance == null) {
            synchronized (MessageHistory.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new MessageHistory();
                }
            }
        }
        return localInstance;
    }

    public synchronized void addMessage(Message message) {
        history.offerFirst(message);
        if (history.size() >= CAPACITY) {
            history.pollLast();
        }
    }

    public synchronized GGMessage findGGMessage(int ggMessageId) {
        for (Message message : history) {
            if (message.getSource() == Source.GOODGAME && ((GGMessage)message).getGgId() == ggMessageId) {
                return (GGMessage)message;
            }
        }
        return null;
    }

    public synchronized FsMessage findFsMessage(int fsMessageId) {
        for (Message message : history) {
            if (message.getSource() == Source.SC2TV && ((FsMessage)message).getFsId() == fsMessageId) {
                return (FsMessage)message;
            }
        }
        return null;
    }

    public synchronized List<TwitchMessage> findTwitchMessages(String author) {
        List<TwitchMessage> messages = null;
        for (Message message : history) {
            if (message.getSource() == Source.TWITCH && StringUtils.equalsIgnoreCase(message.getAuthor(), author)) {
                if (messages == null) {
                    messages = new ArrayList<>();
                }
                messages.add((TwitchMessage)message);
            }
        }
        return messages;
    }

    public void clear() {
        history.clear();
    }
}
