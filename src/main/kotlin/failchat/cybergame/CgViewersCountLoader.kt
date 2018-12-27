package failchat.cybergame

import failchat.Origin
import failchat.viewers.ViewersCountLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class CgViewersCountLoader(
        private val cgApiClient: CgApiClient,
        private val channelName: String
) : ViewersCountLoader{

    override val origin = Origin.CYBERGAME

    override fun loadViewersCount(): CompletableFuture<Int> {
        return CoroutineScope(Dispatchers.Default).future {
            cgApiClient.requestViewersCount(channelName)
        }
    }
}
