package failchat.core;

public interface MessageFilter<T extends Message> {
    /**
     * @return true if message should be dropped
     * */
    boolean filterMessage(T message);
}
