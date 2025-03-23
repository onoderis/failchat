package failchat.youtube

import failchat.Origin
import failchat.util.LateinitVal
import failchat.viewers.ViewersCountLoader
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future

class YoutubeViewersCountLoader(
    private val videoId: String,
    private val youtubeClient: YoutubeClient,
) : ViewersCountLoader {
    private val lazyInnertubeApiKey = LateinitVal<String>()

    override val origin = Origin.YOUTUBE

    override fun loadViewersCount(): CompletableFuture<Int> =
        CoroutineScope(Dispatchers.Default).future {
            val key =
                lazyInnertubeApiKey.get()
                    ?: run {
                        val newKey =
                            youtubeClient.getNewLiveChatSessionData(videoId).innertubeApiKey
                        lazyInnertubeApiKey.set(newKey)
                        newKey
                    }

            youtubeClient.getViewersCount(videoId, key)
        }
}
