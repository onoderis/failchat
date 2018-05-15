package failchat.twitch

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.JsonNode
import failchat.exception.UnexpectedResponseCodeException
import failchat.exception.UnexpectedResponseException
import failchat.util.await
import failchat.util.expect
import failchat.util.nextNonNullToken
import failchat.util.objectMapper
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
        return requestEmoticons("images.json")
    }

    suspend fun requestGlobalEmoticons(): List<TwitchEmoticon> {
        return requestEmoticons("global.json")
    }

    private suspend fun requestEmoticons(pathSegment: String): List<TwitchEmoticon> {
        val url = apiCacheUrl.newBuilder()
                .addPathSegment(pathSegment)
                .build()
        val request = Request.Builder()
                .url(url)
                .build()

        return httpClient.newCall(request)
                .await()
                .use {
                    if (it.code() != 200) throw UnexpectedResponseCodeException(it.code(), request.url().toString())
                    val body = it.body() ?: throw UnexpectedResponseException("Response have no body. Request: ${request.url()}")

                    val emoticons: MutableList<TwitchEmoticon> = ArrayList()
                    val jsonFactory = JsonFactory().apply {
                        codec = objectMapper
                    }

                    val bodyInputStream = body.source().inputStream()
                    val parser = jsonFactory.createParser(bodyInputStream)

                    // parse response. thread blocks here
                    var token = parser.expect(JsonToken.START_OBJECT) // root object
                    parser.expect(JsonToken.FIELD_NAME) // emoticon id/code field

                    while (token != JsonToken.END_OBJECT) {
                        parser.expect(JsonToken.START_OBJECT) // emoticon object

                        val node: JsonNode = parser.readValueAsTree()
                        emoticons.add(parseEmoticon(node))
                        token = parser.nextNonNullToken()
                    }
                    emoticons
                }
    }

    private fun parseEmoticon(node: JsonNode): TwitchEmoticon {
        val id = node.get("id").longValue()
        return TwitchEmoticon(
                id,
                node.get("code").textValue(),
                emoticonUrlFactory.create(id)
        )
    }

}