package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonLoader
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future

/** Uses official twitch API. */
class TwitchGlobalEmoticonLoader(private val twitchClient: TokenAwareTwitchApiClient) :
    EmoticonLoader<TwitchEmoticon> {
    override val origin = Origin.TWITCH

    override fun loadEmoticons(): CompletableFuture<List<TwitchEmoticon>> =
        CoroutineScope(Dispatchers.Default).future { twitchClient.getGlobalEmoticons() }
}
