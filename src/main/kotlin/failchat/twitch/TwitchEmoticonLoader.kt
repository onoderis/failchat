package failchat.twitch

import failchat.core.Origin
import failchat.core.emoticon.EmoticonLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class TwitchEmoticonLoader(private val twitchClient: TwitchApiClient) : EmoticonLoader<TwitchEmoticon> {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(TwitchEmoticonLoader::class.java)
    }

    override val origin = Origin.twitch

    override fun loadEmoticons(): CompletableFuture<Map<out Any, TwitchEmoticon>> {
        return twitchClient.requestEmoticons().thenApply {
            it
                    .map { it.id to it }
                    .toMap(HashMap())
        }
    }

}
