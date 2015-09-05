package failchat.core;

public interface ViewersCounter {
    /**
     * @return viewers count or -1
     * */
    int getViewersCount();
    void updateViewersCount();
}
