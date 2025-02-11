package failchat

/**
 * Первоисточник сообщений / emoticon'ов.
 */
enum class Origin(val commonName: String) { //todo rename to MessageOrigin, remove BTTV
    GOODGAME("goodgame"),
    TWITCH("twitch"),
    BTTV_GLOBAL("bttvGlobal"), //todo refactor?
    BTTV_CHANNEL("bttvChannel"),
    FRANKERFASEZ("frankerfacez"),
    SEVEN_TV_GLOBAL("7tvGlobal"),
    SEVEN_TV_CHANNEL("7tvChannel"),
    YOUTUBE("youtube"),
    FAILCHAT("failchat");


    companion object {
        val values: List<Origin> = Origin.values().toList()

        private val map: Map<String, Origin> = values().map { it.commonName to it }.toMap()

        fun byCommonName(name: String): Origin {
            return map[name] ?: throw IllegalArgumentException("No origin found with name '$name'")
        }
    }
}

val chatOrigins = listOf(
    Origin.GOODGAME,
    Origin.TWITCH,
    Origin.YOUTUBE
)
