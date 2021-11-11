package failchat.twitch

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.Origin
import failchat.exception.UnexpectedResponseCodeException
import failchat.exception.UnexpectedResponseException
import failchat.util.await
import failchat.util.withSuffix
import okhttp3.OkHttpClient
import okhttp3.Request

class SevenTvApiClient(
        private val httpClient: OkHttpClient,
        private val apiUrl: String,
        private val objectMapper: ObjectMapper
) {

    suspend fun loadGlobalEmoticons(): List<SevenTvEmoticon> {
        val globalEmoticonsUrl = apiUrl.withSuffix("/") + "emotes/global"

        val request = Request.Builder()
                .url(globalEmoticonsUrl)
                .get()
                .build()

        return httpClient.newCall(request)
                .await()
                .use {
                    if (it.code() != 200) throw UnexpectedResponseCodeException(it.code())
                    val responseBody = it.body() ?: throw UnexpectedResponseException("null body")
                    val bodyString = responseBody.string()
                    parseEmoticons(bodyString, Origin.SEVEN_TV_GLOBAL)
                }
    }

    /**
     * Load channel emoticons.
     * @param channelName case-incentive channel name.
     * */
    suspend fun loadChannelEmoticons(channelName: String): List<SevenTvEmoticon> {
        val globalEmoticonsUrl = apiUrl.withSuffix("/") + "users/$channelName/emotes"

        val request = Request.Builder()
                .url(globalEmoticonsUrl)
                .get()
                .build()

        return httpClient.newCall(request)
                .await()
                .use {
                    if (it.code() == 404) throw SevenTvChannelNotFoundException(channelName)
                    if (it.code() != 200) throw UnexpectedResponseCodeException(it.code())
                    val responseBody = it.body() ?: throw UnexpectedResponseException("null body")
                    val bodyString = responseBody.string()
                    parseEmoticons(bodyString, Origin.SEVEN_TV_CHANNEL)
                }
    }

    private fun parseEmoticons(responseBody: String, origin: Origin): List<SevenTvEmoticon> {
        val channelEmoticonsNode = objectMapper.readTree(responseBody)

        return channelEmoticonsNode.map {
            val id = it.get("id").asText()
            SevenTvEmoticon(
                    origin,
                    it.get("name").asText(),
                    id,
                    "https://cdn.7tv.app/emote/${id}/2x"
            )
        }
    }

}
