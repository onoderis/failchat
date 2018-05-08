package failchat.goodgame

import failchat.Origin
import failchat.viewers.ViewersCountLoader
import kotlinx.coroutines.experimental.future.future
import java.util.concurrent.CompletableFuture

class GgViewersCountLoader(
        private val channelName: String,
        private val apiClient: GgApiClient
) : ViewersCountLoader {

    override val origin = Origin.GOODGAME

    override fun loadViewersCount(): CompletableFuture<Int> {
        return future {
            apiClient.requestViewersCount(channelName)
        }
    }

}
