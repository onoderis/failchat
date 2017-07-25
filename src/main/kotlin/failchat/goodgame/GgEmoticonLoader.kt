package failchat.goodgame

import failchat.core.Origin
import failchat.core.emoticon.EmoticonLoader
import java.util.concurrent.CompletableFuture

class GgEmoticonLoader(private val ggApiClient: GgApiClient) : EmoticonLoader<GgEmoticon> {

    override val origin = Origin.goodgame

    override fun loadEmoticons(): CompletableFuture<List<GgEmoticon>> {
        return ggApiClient.requestEmoticonList()
    }

    override fun getId(emoticon: GgEmoticon): Long {
        throw UnsupportedOperationException()
    }
}
