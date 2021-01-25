package failchat.youtube2

import failchat.Origin
import failchat.util.LateinitVal
import failchat.viewers.ViewersCountLoader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class YoutubeViewersCountLoader(
        private val videoId: String,
        private val youtubeClient: YoutubeClient2
) : ViewersCountLoader {

    private val lazyInnertubeApiKey = LateinitVal<String>()

    override val origin = Origin.YOUTUBE

    override fun loadViewersCount(): CompletableFuture<Int> {
        //todo scope
        return GlobalScope.future {
            val key = lazyInnertubeApiKey.get() ?: run {
                val newKey = youtubeClient.getNewLiveChatSessionData(videoId).innertubeApiKey
                lazyInnertubeApiKey.set(newKey)
                newKey
            }

            youtubeClient.getViewersCount(videoId, key)
        }
    }
}
