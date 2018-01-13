package failchat.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeUnit

private object CompletableFutures
private val log: Logger = LoggerFactory.getLogger(CompletableFutures::class.java)

fun List<CompletableFuture<*>>.compose(): CompletableFuture<Void?> {
    return CompletableFuture.allOf(*this.toTypedArray())
}

fun <T> CompletableFuture<T>.get(timeout: Duration): T = this.get(timeout.toMillis(), TimeUnit.MILLISECONDS)

fun <T> completedFuture(value: T): CompletableFuture<T> = CompletableFuture.completedFuture(value)

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
