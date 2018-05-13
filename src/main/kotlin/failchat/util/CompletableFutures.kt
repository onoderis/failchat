package failchat.util

import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeUnit

private object CompletableFutures
private val log = logger(CompletableFutures::class)

fun Collection<CompletableFuture<*>>.compose(): CompletableFuture<Void?> {
    return CompletableFuture.allOf(*this.toTypedArray()) // excessive array copying here because of spread operator
}

fun <T> CompletableFuture<T>.get(timeout: Duration): T = this.get(timeout.toMillis(), TimeUnit.MILLISECONDS)

fun <T> completedFuture(value: T): CompletableFuture<T> = CompletableFuture.completedFuture(value)
fun completedFuture(): CompletableFuture<Unit> = CompletableFuture.completedFuture(Unit)

fun <T> exceptionalFuture(exception: Throwable): CompletableFuture<T> {
    return CompletableFuture<T>().apply { completeExceptionally(exception) }
}

/**
 * Unwrap [CompletionException] and return it's cause.
 * @throws NullCompletionCauseException if cause of [CompletionException] is null.
 * */
fun Throwable.completionCause(): Throwable {
    return if (this is CompletionException) {
        this.cause ?: throw NullCompletionCauseException(this)
    } else {
        this
    }
}

private class NullCompletionCauseException(e: CompletionException) : Exception(e)

fun <T> CompletableFuture<T>.logException() {
    whenComplete { _, t ->
        if (t !== null) log.error("Unhandled exception from CompletableFuture", t)
    }
}

/**
 * Execute operation and close the [T].
 * */
inline fun <T : AutoCloseable, R> CompletableFuture<T>.thenUse(crossinline operation: (T) -> R): CompletableFuture<R> {
    return this.thenApply { response ->
        response.use(operation)
    }
}
