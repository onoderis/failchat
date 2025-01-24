package failchat.twitch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
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
        const val apiUrl = "https://7tv.io/v3"
        const val globalEmoteSetId = "global"
    }

    suspend fun loadGlobalEmoticons(): List<SevenTvEmoticon> {
        val request = Request.Builder()
            .url("$apiUrl/emote-sets/$globalEmoteSetId")
            .get()
            .build()

        return httpClient.newCall(request)
            .await()
            .use {
                if (it.code != 200) throw UnexpectedResponseCodeException(it.code)
                val responseBody = it.body ?: throw UnexpectedResponseException("null body")
                val emoteSet = objectMapper.readValue<SevenTvEmoteSetResponse>(responseBody.byteStream())
                parseEmoteSet(emoteSet, Origin.SEVEN_TV_GLOBAL)
            }
    }

    /**
     * Load channel emoticons.
     * @param channelId twitch channel id.
     * */
    suspend fun loadChannelEmoticons(channelId: Long): List<SevenTvEmoticon> {
        val request = Request.Builder()
            .url("$apiUrl/users/twitch/$channelId")
            .get()
            .build()

        return httpClient.newCall(request)
            .await()
            .use {
                if (it.code == 404) throw SevenTvChannelNotFoundException(channelId)
                if (it.code != 200) throw UnexpectedResponseCodeException(it.code)
                val responseBody = it.body ?: throw UnexpectedResponseException("null body")
                val channelResponse = objectMapper.readValue<SevenTvChannelResponse>(responseBody.byteStream())
                parseEmoteSet(channelResponse.emoteSet, Origin.SEVEN_TV_CHANNEL)
            }
    }

    private fun parseEmoteSet(emoteSet: SevenTvEmoteSetResponse, origin: Origin): List<SevenTvEmoticon> {
        return emoteSet.emotes
            .mapNotNull { emote ->
                // 7tv provides emotes in webp format, but JavaFX WebView doesn't support it
                val gifEmotes = emote.data.host.files
                    .filter { it.format == "GIF" }
                    .sortedBy { it.width }
                val pngEmotes = emote.data.host.files
                    .filter { it.format == "PNG" }
                    .sortedBy { it.width }
                // gif is preferable over png
                val sortedWebpEmotes = gifEmotes + pngEmotes

                // get the second-biggest image
                val emoteFile = sortedWebpEmotes.getOrNull(1) ?: sortedWebpEmotes.getOrNull(0)
                if (emoteFile == null) {
                    logger.warn { "No suitable 7tv image found for emote ${emote.name}" }
                    return@mapNotNull null
                }

                SevenTvEmoticon(
                    origin = origin,
                    code = emote.name,
                    id = emote.id,
                    url = "https:${emote.data.host.url}/${emoteFile.name}"
                )
            }
    }
}
