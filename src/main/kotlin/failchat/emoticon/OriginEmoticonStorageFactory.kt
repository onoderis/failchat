package failchat.emoticon

import failchat.Origin
import failchat.Origin.BTTV_CHANNEL
import failchat.Origin.BTTV_GLOBAL
import failchat.Origin.GOODGAME
import failchat.Origin.PEKA2TV
import failchat.Origin.TWITCH
import failchat.twitch.TwitchEmoticonUrlFactory
import org.mapdb.DB

object OriginEmoticonStorageFactory {

    private val commonDbOrigins: List<Origin> = listOf(BTTV_GLOBAL, GOODGAME, PEKA2TV)
    private val inMemoryOrigins: List<Origin> = listOf(BTTV_CHANNEL)

    val mapdbOrigins: List<Origin> = commonDbOrigins + TWITCH

    fun create(db: DB, twitchEmoticonUrlFactory: TwitchEmoticonUrlFactory): List<OriginEmoticonStorage> {
        return commonDbOrigins.map { CommonDbEmoticonStorage(db, it) } +
                inMemoryOrigins.map { CommonInMemoryEmoticonStorage(it) } +
                TwitchEmoticonStorage(db, twitchEmoticonUrlFactory)
    }
}
