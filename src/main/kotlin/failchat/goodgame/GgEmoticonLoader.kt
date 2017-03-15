package failchat.goodgame

import failchat.core.Origin
import failchat.core.emoticon.EmoticonLoader
import java.util.concurrent.CompletableFuture

class GgEmoticonLoader(private val ggApiClient: GgApiClient) : EmoticonLoader<GgEmoticon> {

    override val origin = Origin.goodgame

    override fun loadEmoticons(): CompletableFuture<Map<out Any, GgEmoticon>> {
        return ggApiClient.requestEmoticonList()
                .thenApply { emoticons ->
                    emoticons
                            .map { it.code to it }
                            .toMap()
                }
    }
}
