package failchat.core;

public interface MessageFilter<T extends Message> {
    boolean filterMessage(T message);
}
