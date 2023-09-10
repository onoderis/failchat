package failchat.twitch

import failchat.Origin
import failchat.viewers.ViewersCountLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

class TwitchViewersCountLoader(
        private val userName: String,
        private val twitchClient: TokenAwareTwitchApiClient
) : ViewersCountLoader {

    override val origin = Origin.TWITCH

    override fun loadViewersCount(): CompletableFuture<Int> {
        return CoroutineScope(Dispatchers.Default).future { twitchClient.getViewersCount(userName) }
    }
}
