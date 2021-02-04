package failchat.goodgame

import failchat.Origin
import failchat.emoticon.EmoticonBulkLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class GgEmoticonBulkLoader(private val ggApiClient: GgApiClient) : EmoticonBulkLoader<GgEmoticon> {

    override val origin = Origin.GOODGAME

    override fun loadEmoticons(): CompletableFuture<List<GgEmoticon>> {
        return CoroutineScope(Dispatchers.Default).future {
            ggApiClient.requestEmoticonList()
        }
    }
}
