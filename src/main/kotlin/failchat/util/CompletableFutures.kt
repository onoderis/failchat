package failchat.util

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

inline fun List<CompletableFuture<*>>.compose(): CompletableFuture<Void?> {
    return CompletableFuture.allOf(*this.toTypedArray())
}

inline fun <T> CompletableFuture<T>.get(timeout: Duration): T = this.get(timeout.toMillis(), TimeUnit.MILLISECONDS)

inline fun <T> completedFuture(value: T): CompletableFuture<T> = CompletableFuture.completedFuture(value)

inline fun <T> exceptionalFuture(exception: Throwable): CompletableFuture<T> {
    return CompletableFuture<T>().apply { completeExceptionally(exception) }
}
