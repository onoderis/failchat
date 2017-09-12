package failchat.viewers

import failchat.Origin
import java.util.concurrent.CompletableFuture

interface ViewersCountLoader {
    val origin: Origin
    fun loadViewersCount(): CompletableFuture<Int>
}
