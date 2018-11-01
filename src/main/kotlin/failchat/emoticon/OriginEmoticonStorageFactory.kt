package failchat.emoticon

import failchat.Origin
import failchat.Origin.BTTV_CHANNEL
import failchat.Origin.BTTV_GLOBAL
import failchat.Origin.GOODGAME
import failchat.Origin.PEKA2TV
import failchat.twitch.TwitchEmoticonUrlFactory
import org.mapdb.DB

object OriginEmoticonStorageFactory {

    private val inMemoryOrigins: List<Origin> = listOf(BTTV_GLOBAL, BTTV_CHANNEL, GOODGAME, PEKA2TV)

    fun create(db: DB, twitchEmoticonUrlFactory: TwitchEmoticonUrlFactory): List<OriginEmoticonStorage> {
        return inMemoryOrigins
                .map { CommonInMemoryEmoticonStorage(it) }
                .plus(TwitchEmoticonStorage(db, twitchEmoticonUrlFactory))
    }
}
