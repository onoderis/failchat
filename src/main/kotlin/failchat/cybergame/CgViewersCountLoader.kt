package failchat.cybergame

import failchat.Origin
import failchat.viewers.ViewersCountLoader
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.future.future
import java.util.concurrent.CompletableFuture

class CgViewersCountLoader(
        private val cgApiClient: CgApiClient,
        private val channelName: String
) : ViewersCountLoader{

    override val origin = Origin.CYBERGAME

    override fun loadViewersCount(): CompletableFuture<Int> {
        return future(Unconfined) {
            cgApiClient.requestViewersCount(channelName)
        }
    }
}