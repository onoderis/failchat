package failchat.cybergame

import com.fasterxml.jackson.databind.JsonNode
import failchat.Origin
import failchat.exception.ChannelOfflineException
import failchat.exception.UnexpectedResponseCodeException
import failchat.exception.UnexpectedResponseException
import failchat.util.await
import failchat.util.objectMapper
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class CgApiClient(
        private val httpClient: OkHttpClient,
        apiUrl: String
) {

    private val statusUrl: HttpUrl = HttpUrl.parse(apiUrl)?.let {
        it.newBuilder()
                .addPathSegment("p")
                .addPathSegment("statusv2")
                .build()
    }
            ?: throw IllegalArgumentException("Invalid url '$apiUrl'")


    suspend fun requestChannelId(channelName: String): Long {
        return statusRequest(channelName).get("id").asLong()
    }

    /** @throws ChannelOfflineException */
    suspend fun requestViewersCount(channelName: String): Int {
        val status = statusRequest(channelName)
        if (status.get("online").asInt() == 0) {
            throw ChannelOfflineException(Origin.CYBERGAME, channelName)
        }

        return status.get("spectators").asInt()
    }

    private suspend fun statusRequest(channelName: String) : JsonNode {
        val requestUrl = statusUrl.newBuilder()
                .addQueryParameter("channel", channelName)
                .build()

        val request = Request.Builder()
                .url(requestUrl)
                .build()

        return httpClient.newCall(request)
                .await()
                .use {
                    if (it.code() != 200) throw UnexpectedResponseCodeException(it.code(), it.request().url().toString())
                    val body = it.body()?.bytes()
                            ?: throw UnexpectedResponseException("Response have no body. Request: $requestUrl")
                    objectMapper.readTree(body)
                }
    }

}