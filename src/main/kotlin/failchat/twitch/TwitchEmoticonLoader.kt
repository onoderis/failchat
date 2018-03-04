package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonLoader
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.future.future
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class TwitchEmoticonLoader(
        private val twitchClient: TwitchApiClient,
        private val twitchemotesApiClient: TwitchemotesApiClient
) : EmoticonLoader<TwitchEmoticon> {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(TwitchEmoticonLoader::class.java)
    }

    override val origin = Origin.TWITCH

    override fun loadEmoticons(): CompletableFuture<List<TwitchEmoticon>> {
        return future(Unconfined) {
            try {
                return@future twitchClient.requestEmoticons().await()
            } catch (e: Exception) {
                log.warn("Failed to load twitch emoticons via official twitch API. Will retry via twitchemotes.com API", e)
            }

            return@future twitchemotesApiClient.requestGlobalEmoticons() + twitchemotesApiClient.requestAllEmoticons()
        }
    }

    override fun getId(emoticon: TwitchEmoticon) = emoticon.twitchId

}
