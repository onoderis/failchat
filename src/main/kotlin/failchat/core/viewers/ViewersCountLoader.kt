package failchat.core.viewers

import failchat.core.Origin
import java.util.concurrent.CompletableFuture

interface ViewersCountLoader {
    val origin: Origin
    fun loadViewersCount(): CompletableFuture<Int>
}
