package failchat.youtube

import failchat.core.Origin
import failchat.core.Origin.youtube
import failchat.core.viewers.ViewersCountLoader
import failchat.exception.ChannelOfflineException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.function.Supplier

class YtViewersCountLoader(
        private val videoId: String,
        private val youtubeApiClient: YtApiClient,
        private val youtubeExecutor: ExecutorService
) : ViewersCountLoader {

    override val origin = Origin.youtube

    override fun loadViewersCount(): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync<Int>(Supplier {
            youtubeApiClient.getViewersCount(videoId)
                    ?: throw ChannelOfflineException(youtube, videoId) //todo change exception
        }, youtubeExecutor)
    }

}
