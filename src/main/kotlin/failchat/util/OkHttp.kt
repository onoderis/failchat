package failchat.util

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.experimental.suspendCoroutine

val jsonMediaType: MediaType = MediaType.parse("application/json")!!
val textMediaType: MediaType = MediaType.parse("text/plain")!!
val emptyBody: RequestBody = RequestBody.create(textMediaType, "")

fun Call.toFuture(): CompletableFuture<Response> {
    val future = CompletableFuture<Response>()
    this.enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            future.completeExceptionally(e)
        }

        override fun onResponse(call: Call, response: Response) {
            future.complete(response)
        }
    })
    return future
}

/**
 * Execute operation and close [Response].
 * */
inline fun <T> CompletableFuture<Response>.thenApplySafe(crossinline operation: (Response) -> T): CompletableFuture<T> {
    return this.thenApply { response ->
        response.use(operation)
    }
}

suspend fun Call.await(): Response {
    return suspendCoroutine { continuation ->
        this.enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }

            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }
        })
    }
}
