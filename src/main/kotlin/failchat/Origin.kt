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
    FAILCHAT("failchat");

    companion object {
        private val map: Map<String, Origin> = values().map { it.commonName to it }.toMap()

        fun byCommonName(name: String): Origin {
            return map[name] ?: throw IllegalArgumentException("No origin found with name '$name'")
        }
    }
}

val chatOrigins = listOf(
        Origin.PEKA2TV,
        Origin.GOODGAME,
        Origin.TWITCH,
        Origin.YOUTUBE,
        Origin.CYBERGAME
)
