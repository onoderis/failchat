package failchat.twitch

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.util.await
import failchat.util.getBodyIfStatusIs
import failchat.util.nonNullBody
import failchat.util.withSuffix
import okhttp3.OkHttpClient
import okhttp3.Request

class FfzApiClient(
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper,
        apiUrl: String
) {

    private val apiUrl = apiUrl.withSuffix("/")

    /** @throws [FfzChannelNotFoundException]. */
    suspend fun requestEmoticons(roomName: String): List<FfzEmoticon> {
        val request = Request.Builder()
                .url(apiUrl + "room/" + roomName)
                .get()
                .build()

        val parsedBody = httpClient.newCall(request).await().use {
            if (it.code == 404) throw FfzChannelNotFoundException(roomName)
            val bodyText = it.getBodyIfStatusIs(200).nonNullBody.string()
            objectMapper.readTree(bodyText)
        }

        val emoticonSet = parsedBody.get("room").get("set").longValue()

        return parsedBody
                .get("sets")
                .get(emoticonSet.toString())
                .get("emoticons")
                .map { emoticonNode ->
                    FfzEmoticon(
                        emoticonNode.get("name").textValue(),
                        emoticonNode.get("urls").get("1").textValue()
                    )
                }
    }

}
