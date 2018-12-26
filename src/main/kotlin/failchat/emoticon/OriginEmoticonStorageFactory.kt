package failchat.emoticon

import failchat.Origin
import failchat.Origin.BTTV_CHANNEL
import failchat.Origin.BTTV_GLOBAL
import failchat.Origin.FAILCHAT
import failchat.Origin.FRANKERFASEZ
import failchat.Origin.GOODGAME
import failchat.Origin.PEKA2TV
import failchat.Origin.TWITCH
import failchat.twitch.TwitchEmoticonUrlFactory
import org.mapdb.DB

object OriginEmoticonStorageFactory {

    private val idCodeDbOrigins: List<Origin> = listOf(BTTV_GLOBAL, GOODGAME, PEKA2TV)
    private val idCodeMemoryOrigins: List<Origin> = listOf(BTTV_CHANNEL)
    private val codeMemoryOrigins: List<Origin> = listOf(FRANKERFASEZ, FAILCHAT)

    val dbOrigins: List<Origin> = idCodeDbOrigins + TWITCH

    fun create(db: DB, twitchEmoticonUrlFactory: TwitchEmoticonUrlFactory): List<OriginEmoticonStorage> {
        return idCodeDbOrigins.map { EmoticonCodeIdDbStorage(db, it) } +
                idCodeMemoryOrigins.map { EmoticonCodeIdMemoryStorage(it) } +
                codeMemoryOrigins.map { EmoticonCodeMemoryStorage(it) } +
                TwitchEmoticonStorage(db, twitchEmoticonUrlFactory)
    }
}
