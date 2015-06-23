package failchat.core;

/**
 * Источники сообщений/смайлов
 */
public enum Source {
    SC2TV,
    GOODGAME,
    TWITCH,
    TEST;

    // for json
    private String lcSource;

    Source() {
        lcSource = this.toString().toLowerCase();
    }

    public String getLowerCased() {
        return lcSource;
    }
}
