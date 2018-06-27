package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonLoader
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import mu.KLogging
import java.util.concurrent.CompletableFuture

class TwitchEmoticonLoader(
        private val twitchClient: TwitchApiClient,
        private val twitchemotesApiClient: TwitchemotesApiClient
) : EmoticonLoader<TwitchEmoticon> {

    private companion object : KLogging()

    override val origin = Origin.TWITCH

    override fun loadEmoticons(): CompletableFuture<List<TwitchEmoticon>> {
        return future(Unconfined) {
            try {
                return@future twitchClient.requestEmoticons().await()
            } catch (e: Exception) {
                logger.warn("Failed to load twitch emoticons via official twitch API. Will retry via twitchemotes.com API", e)
            }

            return@future twitchemotesApiClient.requestGlobalEmoticons() + twitchemotesApiClient.requestAllEmoticons()
        }
    }

    override fun getId(emoticon: TwitchEmoticon) = emoticon.twitchId

}
