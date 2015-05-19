package failchat.core;

public interface ChatClient {
    void goOffline();
    void goOnline();
    ChatClientStatus getStatus();
}
