package failchat.utils

import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CompletableFuture

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

val jsonMediaType: MediaType = MediaType.parse("application/json")!!
val textMediaType: MediaType = MediaType.parse("text/plain")!!
val emptyBody: RequestBody = RequestBody.create(textMediaType, "")
