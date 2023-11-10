package failchat.goodgame

import failchat.Origin
import failchat.emoticon.EmoticonLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class GgEmoticonLoader(private val ggApiClient: GgApiClient) : EmoticonLoader<GgEmoticon> {

    override val origin = Origin.GOODGAME

    override fun loadEmoticons(): CompletableFuture<List<GgEmoticon>> {
        return CoroutineScope(Dispatchers.Default).future {
            ggApiClient.requestEmoticonList()
        }
    }
}
