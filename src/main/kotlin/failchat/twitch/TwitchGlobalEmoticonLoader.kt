package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

/** Uses official twitch API. */
class TwitchGlobalEmoticonLoader(
        private val twitchClient: TokenAwareTwitchApiClient
) : EmoticonLoader<TwitchEmoticon> {

    override val origin = Origin.TWITCH

    override fun loadEmoticons(): CompletableFuture<List<TwitchEmoticon>> {
        return CoroutineScope(Dispatchers.Default).future { twitchClient.getGlobalEmoticons() }
    }
}
