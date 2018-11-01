package failchat.twitch

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.JsonNode
import failchat.util.completionCause
import failchat.util.expect
import failchat.util.nextNonNullToken
import failchat.util.nonNullBody
import failchat.util.objectMapper
import failchat.util.thenUse
import failchat.util.toFuture
import failchat.util.validateResponseCode
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.toList
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class TwitchemotesApiClient(
        private val httpClient: OkHttpClient,
        apiUrl: String,
        private val emoticonUrlFactory: TwitchEmoticonUrlFactory
) {

    private val apiCacheUrl: HttpUrl = HttpUrl.parse(apiUrl)?.let {
        it.newBuilder()
                .addPathSegment("api_cache")
                .addPathSegment("v3")
                .build()
    }
            ?: throw IllegalArgumentException("Invalid url '$apiUrl'")

    suspend fun requestAllEmoticons(): List<TwitchEmoticon> {
        return requestEmoticonsToChannel("images.json").toList()
    }

    fun requestAllEmoticonsToChannel(): ReceiveChannel<TwitchEmoticon> {
        return requestEmoticonsToChannel("images.json")
    }

    suspend fun requestGlobalEmoticons(): List<TwitchEmoticon> {
        return requestEmoticonsToChannel("global.json").toList()
    }

    fun requestGlobalEmoticonsToChannel(): ReceiveChannel<TwitchEmoticon> {
        return requestEmoticonsToChannel("global.json")
    }

    private fun requestEmoticonsToChannel(pathSegment: String): ReceiveChannel<TwitchEmoticon> {
        val url = apiCacheUrl.newBuilder()
                .addPathSegment(pathSegment)
                .build()
        val request = Request.Builder()
                .url(url)
                .build()
        val channel = Channel<TwitchEmoticon>(Channel.UNLIMITED)

        httpClient.newCall(request)
                .toFuture()
                .thenUse {
                    val body = it.validateResponseCode(200).nonNullBody

                    val jsonFactory = JsonFactory().apply {
                        codec = objectMapper
                    }
                    val bodyInputStream = body.source().inputStream()
                    val parser = jsonFactory.createParser(bodyInputStream)

                    // parse response. okio thread blocks here
                    var token = parser.expect(JsonToken.START_OBJECT) // root object
                    parser.expect(JsonToken.FIELD_NAME) // emoticon id/code field

                    while (token != JsonToken.END_OBJECT) {
                        parser.expect(JsonToken.START_OBJECT) // emoticon object

                        val node: JsonNode = parser.readValueAsTree()
                        channel.offer(parseEmoticon(node))
                        token = parser.nextNonNullToken()
                    }

                    channel.close()
                }
                .exceptionally { e ->
                    channel.close(e.completionCause())
                }

        return channel
    }

    private fun parseEmoticon(node: JsonNode): TwitchEmoticon {
        val id = node.get("id").longValue()
        return TwitchEmoticon(
                id,
                node.get("code").textValue(),
                emoticonUrlFactory
        )
    }

}
