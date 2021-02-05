package failchat.util

import mu.KotlinLogging
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.CompletionStage
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

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

fun <T> CompletionStage<T>.logException() {
    whenComplete { _, t ->
        if (t !== null) logger.error("Unhandled exception from CompletionStage", t)
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

/**
 * Perform [action], if it throws [ExecutionException] cause will be thrown instead.
 * */
inline fun <T> doUnwrappingExecutionException(action: () -> T): T {
    try {
        return action.invoke()
    } catch (e: ExecutionException) {
        throw e.cause ?: e
    }
}
