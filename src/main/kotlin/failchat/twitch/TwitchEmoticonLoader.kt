package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonBulkLoader
import java.util.concurrent.CompletableFuture

/** Uses official twitch API. */
class TwitchEmoticonLoader(
        private val twitchClient: TwitchApiClient
) : EmoticonBulkLoader<TwitchEmoticon> {

    override val origin = Origin.TWITCH

    override fun loadEmoticons(): CompletableFuture<List<TwitchEmoticon>> {
        return twitchClient.getCommonEmoticons()
    }

}
