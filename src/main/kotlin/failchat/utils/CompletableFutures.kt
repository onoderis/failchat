package failchat.utils

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

inline fun List<CompletableFuture<*>>.compose(): CompletableFuture<Void?> {
    return CompletableFuture.allOf(*this.toTypedArray())
}

inline fun <T> CompletableFuture<T>.get(timeout: Duration): T = this.get(timeout.toMillis(), TimeUnit.MILLISECONDS)
