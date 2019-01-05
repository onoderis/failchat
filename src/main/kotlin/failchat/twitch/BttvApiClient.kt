package failchat.twitch

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.Origin
import failchat.exception.UnexpectedResponseCodeException
import failchat.exception.UnexpectedResponseException
import failchat.util.thenUse
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

    fun loadGlobalEmoticons(): CompletableFuture<List<BttvEmoticon>> {
        val globalEmoticonsUrl = apiUrl.withSuffix("/") + "2/emotes"

        val request = Request.Builder()
                .url(globalEmoticonsUrl)
                .get()
                .build()

        return httpClient.newCall(request)
                .toFuture()
                .thenUse {
                    if (it.code() != 200) throw UnexpectedResponseCodeException(it.code())
                    val responseBody = it.body() ?: throw UnexpectedResponseException("null body")
                    val bodyString = responseBody.string()
                    return@thenUse parseEmoticons(bodyString, Origin.BTTV_GLOBAL)
                }
    }

    /**
     * @param channel is case insensitive.
     * @throws [BttvChannelNotFoundException].
     * */
    fun loadChannelEmoticons(channel: String): CompletableFuture<List<BttvEmoticon>> {
        val globalEmoticonsUrl = apiUrl.withSuffix("/") + "2/channels/" + channel

        val request = Request.Builder()
                .url(globalEmoticonsUrl)
                .get()
                .build()

        return httpClient.newCall(request)
                .toFuture()
                .thenUse {
                    when (it.code()) {
                        200 -> {}
                        404 -> throw BttvChannelNotFoundException(channel)
                        else -> throw UnexpectedResponseCodeException(it.code())
                    }
                    val responseBody = it.body() ?: throw UnexpectedResponseException("null body")
                    val bodyString = responseBody.string()
                    return@thenUse parseEmoticons(bodyString, Origin.BTTV_CHANNEL)
                }
    }

    private fun parseEmoticons(responseBody: String, origin: Origin): List<BttvEmoticon> {
        val channelEmoticonsNode = objectMapper.readTree(responseBody)

        return channelEmoticonsNode
                .get("emotes")
                .map {
                    BttvEmoticon(
                            origin,
                            it.get("code").asText(),
                            "https://cdn.betterttv.net/emote/${it.get("id").asText()}/2x"
                    )
                }
    }

}
