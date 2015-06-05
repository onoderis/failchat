package failchat.core;

public interface MessageHandler<T extends Message> {
    void handleMessage(T message);
}
