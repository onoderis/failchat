package failchat.goodgame

import failchat.Origin
import failchat.emoticon.EmoticonBulkLoader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class GgEmoticonBulkLoader(private val ggApiClient: GgApiClient) : EmoticonBulkLoader<GgEmoticon> {

    override val origin = Origin.GOODGAME

    override fun loadEmoticons(): CompletableFuture<List<GgEmoticon>> {
        return GlobalScope.future {
            ggApiClient.requestEmoticonList()
        }
    }
}
