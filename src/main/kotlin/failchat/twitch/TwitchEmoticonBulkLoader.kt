package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonBulkLoader
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import java.util.concurrent.CompletableFuture

/** Uses official twitch API. */
class TwitchEmoticonBulkLoader(
        private val twitchClient: TwitchApiClient
) : EmoticonBulkLoader<TwitchEmoticon> {

    override val origin = Origin.TWITCH

    override fun loadEmoticons(): CompletableFuture<List<TwitchEmoticon>> {
        return GlobalScope.future {
            twitchClient.requestEmoticons().await()
        }
    }

}
