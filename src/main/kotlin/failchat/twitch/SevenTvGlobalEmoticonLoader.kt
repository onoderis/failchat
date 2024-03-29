package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class SevenTvGlobalEmoticonLoader(private val sevenTvApiClient: SevenTvApiClient) : EmoticonLoader<SevenTvEmoticon> {

    override val origin = Origin.SEVEN_TV_GLOBAL

    override fun loadEmoticons(): CompletableFuture<List<SevenTvEmoticon>> {
        return CoroutineScope(Dispatchers.Default).future {
            sevenTvApiClient.loadGlobalEmoticons()
        }
    }
}
