package failchat.goodgame

import failchat.Origin
import failchat.viewers.ViewersCountLoader
import java.util.concurrent.CompletableFuture

class GgViewersCountLoader(
        private val channelName: String,
        private val apiClient: GgApiClient
) : ViewersCountLoader {

    override val origin = Origin.goodgame

    override fun loadViewersCount(): CompletableFuture<Int> {
        return apiClient.requestViewersCount(channelName)
    }

}
