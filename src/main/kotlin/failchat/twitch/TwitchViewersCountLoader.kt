package failchat.twitch

import failchat.core.Origin
import failchat.core.viewers.ViewersCountLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

class TwitchViewersCountLoader(
        private val userName: String,
        private val twitchClient: TwitchApiClient
) : ViewersCountLoader {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(TwitchViewersCountLoader::class.java)
    }

    private val lazyUserId: AtomicReference<Long?> = AtomicReference(null)

    override val origin = Origin.twitch

    override fun loadViewersCount(): CompletableFuture<Int> {
        val userIdFuture: CompletableFuture<Long> = lazyUserId.get()
                ?.let { CompletableFuture.completedFuture(it) }
                ?: twitchClient.requestUserId(userName).whenComplete { id, _ -> id?.let { lazyUserId.set(it) } }

        return userIdFuture
                .thenCompose { userId -> twitchClient.requestViewersCount(userId) }
    }

}
