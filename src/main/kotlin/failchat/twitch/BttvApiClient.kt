package failchat.twitch

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.core.Origin
import failchat.core.emoticon.Emoticon
import failchat.exception.UnexpectedResponseCodeException
import failchat.exception.UnexpectedResponseException
import failchat.util.thenApplySafe
import failchat.util.toFuture
import failchat.util.withSuffix
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.CompletableFuture

class BttvApiClient(
        private val httpClient: OkHttpClient,
        private val apiUrl: String,
        private val objectMapper: ObjectMapper = ObjectMapper()
) {

    fun loadGlobalEmoticons(): CompletableFuture<List<Emoticon>> {
        val globalEmoticonsUrl = apiUrl.withSuffix("/") + "2/emotes"

        val request = Request.Builder()
                .url(globalEmoticonsUrl)
                .get()
                .build()

        return httpClient.newCall(request)
                .toFuture()
                .thenApplySafe {
                    if (it.code() != 200) throw UnexpectedResponseCodeException(it.code())
                    val responseBody = it.body() ?: throw UnexpectedResponseException("null body")
                    val bodyString = responseBody.string()
                    return@thenApplySafe parseEmoticons(bodyString, Origin.bttvGlobal)
                }
    }

    /**
     * @param channel is case insensitive.
     * */
    fun loadChannelEmoticons(channel: String): CompletableFuture<List<Emoticon>> {
        val globalEmoticonsUrl = apiUrl.withSuffix("/") + "2/channels/" + channel

        val request = Request.Builder()
                .url(globalEmoticonsUrl)
                .get()
                .build()

        return httpClient.newCall(request)
                .toFuture()
                .thenApplySafe {
                    if (it.code() != 200) throw UnexpectedResponseCodeException(it.code())
                    val responseBody = it.body() ?: throw UnexpectedResponseException("null body")
                    val bodyString = responseBody.string()
                    return@thenApplySafe parseEmoticons(bodyString, Origin.bttvChannel)
                }
    }

    private fun parseEmoticons(responseBody: String, origin: Origin): List<Emoticon> {
        val channelEmoticonsNode = objectMapper.readTree(responseBody)

        return channelEmoticonsNode
                .get("emotes")
                .map {
                    Emoticon(
                            origin,
                            it.get("code").asText(),
                            "https://cdn.betterttv.net/emote/${it.get("id").asText()}/2x"
                    )
                }
    }

}
