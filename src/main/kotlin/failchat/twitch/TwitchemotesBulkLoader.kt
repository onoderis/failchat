package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonBulkLoader
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.future.future
import java.util.concurrent.CompletableFuture

/** Uses twitchemotes.com API. */
class TwitchemotesBulkLoader(
        private val twitchemotesApiClient: TwitchemotesApiClient
) : EmoticonBulkLoader<TwitchEmoticon> {

    override val origin = Origin.TWITCH

    override fun loadEmoticons(): CompletableFuture<List<TwitchEmoticon>> {
        return GlobalScope.future {
            twitchemotesApiClient.requestGlobalEmoticons() + twitchemotesApiClient.requestAllEmoticons()
        }
    }

}
