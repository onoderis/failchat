package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class TwitchEmoticonLoader(private val twitchClient: TwitchApiClient) : EmoticonLoader<TwitchEmoticon> {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(TwitchEmoticonLoader::class.java)
    }

    override val origin = Origin.TWITCH

    override fun loadEmoticons(): CompletableFuture<List<TwitchEmoticon>> {
        return twitchClient.requestEmoticons()
    }

    override fun getId(emoticon: TwitchEmoticon) = emoticon.twitchId

}
