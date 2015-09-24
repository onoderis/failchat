package failchat.funstream;

import failchat.core.MessageFilter;

import java.util.Deque;
import java.util.LinkedList;

public class DoubleMessageFilter implements MessageFilter<FsMessage> {
    private Deque<Integer> lastMessages = new LinkedList<>();

    @Override
    public boolean filterMessage(FsMessage message) {
        checkCapacity();
        if (lastMessages.contains(message.getFsId())) {
            return true;
        } else {
            lastMessages.addFirst(message.getFsId());
            return false;
        }
    }

    private void checkCapacity() {
        if (lastMessages.size() >= 9) {
            lastMessages.pollLast();
        }
    }
}
