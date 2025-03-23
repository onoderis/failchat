package failchat.goodgame

import failchat.Origin
import failchat.emoticon.EmoticonLoader
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future

class GgEmoticonLoader(private val ggApiClient: GgApiClient) : EmoticonLoader<GgEmoticon> {
    override val origin = Origin.GOODGAME

    override fun loadEmoticons(): CompletableFuture<List<GgEmoticon>> =
        CoroutineScope(Dispatchers.Default).future { ggApiClient.requestEmoticonList() }
}
