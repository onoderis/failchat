package failchat.core;

/**
 * Каждый ChatClient работает в свойм thread'е
 */

public interface ChatClient {
    void goOffline();
    void goOnline();
    ChatClientStatus getStatus();
}
