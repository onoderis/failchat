package failchat.goodgame

import failchat.Origin.GOODGAME
import failchat.viewers.ViewersCountLoader
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future

class GgViewersCountLoader(
    private val ggApi2Client: GgApi2Client,
    private val channelName: String,
) : ViewersCountLoader {
    override val origin = GOODGAME

    override fun loadViewersCount(): CompletableFuture<Int> =
        CoroutineScope(Dispatchers.Default).future { ggApi2Client.requestViewersCount(channelName) }
}
