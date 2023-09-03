package failchat.twitch

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.Origin
import failchat.exception.UnexpectedResponseCodeException
import failchat.exception.UnexpectedResponseException
import failchat.util.await
import okhttp3.OkHttpClient
import okhttp3.Request

class SevenTvApiClient(
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper
) {

    private companion object {
        const val apiUrl = "https://api.7tv.app/v2"
        const val globalEmotesUrl = "$apiUrl/emotes/global"
    }

    suspend fun loadGlobalEmoticons(): List<SevenTvEmoticon> {
        val request = Request.Builder()
                .url(globalEmotesUrl)
                .get()
                .build()

        return httpClient.newCall(request)
                .await()
                .use {
                    if (it.code != 200) throw UnexpectedResponseCodeException(it.code)
                    val responseBody = it.body ?: throw UnexpectedResponseException("null body")
                    val bodyString = responseBody.string()
                    parseEmoticons(bodyString, Origin.SEVEN_TV_GLOBAL)
                }
    }

    /**
     * Load channel emoticons.
     * @param channelId case-incentive channel name.
     * */
    suspend fun loadChannelEmoticons(channelId: Long): List<SevenTvEmoticon> {
        val request = Request.Builder()
                .url("$apiUrl/users/$channelId/emotes")
                .get()
                .build()

        return httpClient.newCall(request)
                .await()
                .use {
                    if (it.code == 404) throw SevenTvChannelNotFoundException(channelId)
                    if (it.code != 200) throw UnexpectedResponseCodeException(it.code)
                    val responseBody = it.body ?: throw UnexpectedResponseException("null body")
                    val bodyString = responseBody.string()
                    parseEmoticons(bodyString, Origin.SEVEN_TV_CHANNEL)
                }
    }

    private fun parseEmoticons(responseBody: String, origin: Origin): List<SevenTvEmoticon> {
        val channelEmoticonsNode = objectMapper.readTree(responseBody)
        return channelEmoticonsNode.map {
            val id = it.get("id").asText()
            val url1x = it.get("urls").first().get(1).asText()
            val url2x = it.get("urls").get(1)?.get(1)?.asText()
            SevenTvEmoticon(
                    origin,
                    it.get("name").asText(),
                    id,
                    url2x ?: url1x
            )
        }
    }
}
