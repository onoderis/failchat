package failchat.twitch

import failchat.Origin
import failchat.viewers.ViewersCountLoader
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

class TwitchViewersCountLoader(
        private val userName: String,
        private val twitchClient: TwitchApiClient
) : ViewersCountLoader {

    private val lazyUserId: AtomicReference<Long?> = AtomicReference(null)

    override val origin = Origin.TWITCH

    override fun loadViewersCount(): CompletableFuture<Int> {
        val userIdFuture: CompletableFuture<Long> = lazyUserId.get()
                ?.let { CompletableFuture.completedFuture(it) }
                ?: twitchClient.requestUserId(userName).whenComplete { id, _ -> id?.let { lazyUserId.set(it) } }

        return userIdFuture
                .thenCompose { userId -> twitchClient.requestViewersCount(userId) }
    }

}
