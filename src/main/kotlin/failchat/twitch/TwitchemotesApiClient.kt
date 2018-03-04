package failchat.twitch

import failchat.exception.UnexpectedResponseCodeException
import failchat.exception.UnexpectedResponseException
import failchat.util.await
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
                    val body = it.body()?.bytes()
                            ?: throw UnexpectedResponseException("Response have no body. Request: ${request.url()}")
                    objectMapper.readTree(body)
                }
                .map {
                    val id = it.get("id").longValue()
                    TwitchEmoticon(
                            id,
                            it.get("code").textValue(),
                            emoticonUrlFactory.create(id)
                    )
                }
    }

}