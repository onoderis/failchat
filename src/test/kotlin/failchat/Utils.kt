package failchat

import failchat.util.await
import okhttp3.Request
import kotlin.test.assertEquals

object Utils

fun readResourceAsString(resource: String): String {
    val bytes = Utils::class.java.getResourceAsStream(resource)?.readBytes() ?: error("No resource $resource")
    return String(bytes)
}

suspend fun assertRequestToUrlReturns200(url: String) {
    val request = Request.Builder().url(url).get().build()
    okHttpClient.newCall(request).await().use {
        assertEquals(200, it.code)
    }
}
