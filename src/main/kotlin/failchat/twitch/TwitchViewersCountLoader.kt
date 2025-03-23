package failchat.twitch

import failchat.Origin
import failchat.viewers.ViewersCountLoader
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future

class TwitchViewersCountLoader(
    private val userName: String,
    private val twitchClient: TokenAwareTwitchApiClient,
) : ViewersCountLoader {
    override val origin = Origin.TWITCH

    override fun loadViewersCount(): CompletableFuture<Int> =
        CoroutineScope(Dispatchers.Default).future { twitchClient.getViewersCount(userName) }
}
