package failchat.goodgame

import failchat.Origin.GOODGAME
import failchat.viewers.ViewersCountLoader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class GgViewersCountLoader(
        private val ggApi2Client: GgApi2Client,
        private val channelName: String
) : ViewersCountLoader {

    override val origin = GOODGAME

    override fun loadViewersCount(): CompletableFuture<Int> {
        return GlobalScope.future {
            ggApi2Client.requestViewersCount(channelName)
        }
    }
}
