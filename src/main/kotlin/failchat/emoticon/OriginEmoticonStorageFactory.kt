package failchat.emoticon

import failchat.Origin
import failchat.Origin.BTTV_CHANNEL
import failchat.Origin.BTTV_GLOBAL
import failchat.Origin.FAILCHAT
import failchat.Origin.FRANKERFASEZ
import failchat.Origin.GOODGAME
import failchat.Origin.PEKA2TV
import failchat.Origin.SEVEN_TV_CHANNEL
import failchat.Origin.SEVEN_TV_GLOBAL
import failchat.Origin.TWITCH
import org.mapdb.DB

object OriginEmoticonStorageFactory {

    //todo code db origin storage, BTTV_GLOBAL

    private val caseSensitiveOptions = mapOf(
            TWITCH to false,
            GOODGAME to false,
            PEKA2TV to false,
            FAILCHAT to false,
            BTTV_GLOBAL to true,
            BTTV_CHANNEL to true,
            FRANKERFASEZ to true,
            SEVEN_TV_GLOBAL to true,
            SEVEN_TV_CHANNEL to true
    )

    private val idCodeDbOrigins: List<Origin> = listOf(BTTV_GLOBAL, SEVEN_TV_GLOBAL, GOODGAME, PEKA2TV)
    private val codeMemoryOrigins: List<Origin> = listOf(BTTV_CHANNEL, SEVEN_TV_CHANNEL, FRANKERFASEZ, FAILCHAT)

    val dbOrigins: List<Origin> = idCodeDbOrigins + TWITCH

    fun create(db: DB, twitchEmoticonFactory: TwitchEmoticonFactory): List<OriginEmoticonStorage> {
        return idCodeDbOrigins.map { EmoticonCodeIdDbStorage(db, it, caseSensitiveOptions[it]!!) } +
                codeMemoryOrigins.map { EmoticonCodeMemoryStorage(it, caseSensitiveOptions[it]!!) } +
                EmoticonCodeIdDbCompactStorage(db, TWITCH, twitchEmoticonFactory)
    }
}
