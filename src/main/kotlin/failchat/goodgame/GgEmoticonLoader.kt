package failchat.goodgame

import failchat.Origin
import failchat.emoticon.EmoticonLoader
import kotlinx.coroutines.experimental.future.future
import java.util.concurrent.CompletableFuture

class GgEmoticonLoader(private val ggApiClient: GgApiClient) : EmoticonLoader<GgEmoticon> {

    override val origin = Origin.GOODGAME

    override fun loadEmoticons(): CompletableFuture<List<GgEmoticon>> {
        return future {
            ggApiClient.requestEmoticonList()
        }
    }

    override fun getId(emoticon: GgEmoticon): Long {
        throw UnsupportedOperationException()
    }
}
