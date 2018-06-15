package failchat

/**
 * Первоисточник сообщений / emoticon'ов.
 */
enum class Origin(val commonName: String) { //todo rename to MessageOrigin, remove BTTV
    PEKA2TV("peka2tv"),
    GOODGAME("goodgame"),
    TWITCH("twitch"),
    BTTV_GLOBAL("bttvGlobal"), //todo refactor?
    BTTV_CHANNEL("bttvChannel"),
    YOUTUBE("youtube"),
    CYBERGAME("cybergame"),
    FAILCHAT("failchat")
}
