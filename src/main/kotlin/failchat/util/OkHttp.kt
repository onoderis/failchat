package failchat.util

import failchat.exception.UnexpectedResponseCodeException
import failchat.exception.UnexpectedResponseException
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody
import java.io.IOException
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

val jsonMediaType: MediaType = "application/json".toMediaTypeOrNull()!!
val textMediaType: MediaType = "text/plain".toMediaTypeOrNull()!!
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

fun Response.getBodyIfStatusIs(expectedStatus: Int): Response {
    if (this.code != expectedStatus) {
        throw UnexpectedResponseCodeException(this.code, request.url.toString())
    }
    return this
}

val Response.nonNullBody: ResponseBody
    get() = this.body ?: throw UnexpectedResponseException("null body")
