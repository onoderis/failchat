package failchat.goodgame

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.Origin.GOODGAME
import failchat.exception.ChannelOfflineException
import failchat.exception.UnexpectedResponseCodeException
import failchat.exception.UnexpectedResponseException
import failchat.util.await
import okhttp3.OkHttpClient
import okhttp3.Request

class GgApi2Client(
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper
) {

    private companion object {
        // Documentation: https://api2.goodgame.ru/apigility/documentation/Goodgame-v2
        const val apiUrl = "https://api2.goodgame.ru/v2"
    }

    /** @return viewers count for a goodgame video player. */
    suspend fun requestViewersCount(channelName: String): Int {
        val request = Request.Builder()
                .get()
                .url("$apiUrl/streams/$channelName")
                .header("Accept", "application/vnd.goodgame.v2+json")
                .build()

        val response = httpClient.newCall(request).await().use { response ->
            if (response.code != 200) throw UnexpectedResponseCodeException(response.code)
            val responseBody = response.body ?: throw UnexpectedResponseException("null body")
            objectMapper.readValue(responseBody.charStream(), StreamResponse::class.java)
        }

        if (response.status != "Live") {
            throw ChannelOfflineException(GOODGAME, channelName)
        }

        return response.playerViewers
    }

}
