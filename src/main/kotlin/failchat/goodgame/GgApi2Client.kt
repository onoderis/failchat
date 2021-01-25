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
        private val objectMapper: ObjectMapper,
        apiUrl: String
) {

    // http://api2.goodgame.ru/apigility/documentation/Goodgame-v2

    private val apiUrl = apiUrl.removeSuffix("/")

    /** @return viewers count for only a goodgame player. */
    suspend fun requestViewersCount(channelName: String): Int {
        val request = Request.Builder()
                .get()
                .url("$apiUrl/streams/$channelName")
                .header("Accept", "application/vnd.goodgame.v2+json")
                .build()

        val response = httpClient.newCall(request).await()

        if (response.code() != 200) throw UnexpectedResponseCodeException(response.code())
        val responseBody = response.body() ?: throw UnexpectedResponseException("null body")
        val responseNode = objectMapper.readTree(responseBody.string())


        val streamLive = responseNode.get("status").textValue() == "Live"
        val ggPlayerLive = responseNode.get("is_broadcast").booleanValue()

        if (!streamLive) throw ChannelOfflineException(GOODGAME, channelName)
        if (!ggPlayerLive) return 0

        return responseNode.get("player_viewers").asInt()
    }

}
