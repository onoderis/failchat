package failchat.goodgame

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import failchat.exception.UnexpectedResponseCodeException
import failchat.exception.UnexpectedResponseException
import failchat.util.await
import failchat.util.withSuffix
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

class GgApiClient(
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper,
        apiUrl: String,
        private val emoticonsJsUrl: String
) {

    private companion object {
        val globalEmoticonsPattern: Pattern = Pattern.compile("""Smiles ?: ?(\[.+?\]),""")
        val channelEmoticonsPattern: Pattern = Pattern.compile("""Channel_Smiles ?: ?(\{.+?\}\]\}),""")
    }

    private val apiUrl = apiUrl.withSuffix("/")

    suspend fun requestEmoticonList(): List<GgEmoticon> {
        val request = Request.Builder()
                .url(emoticonsJsUrl)
                .get()
                .build()

        return httpClient.newCall(request)
                .await()
                .use {
                    if (it.code != 200) throw UnexpectedResponseCodeException(it.code)
                    val responseBody = it.body ?: throw UnexpectedResponseException("null body")
                    val jsContent = responseBody.string()
                    parseGlobalEmoticons(jsContent) + parseChannelEmoticons(jsContent)
                }
    }

    suspend fun requestChannelId(channelName: String): Long {
        // https://github.com/GoodGame/API/blob/master/Streams/stream_api.md
        return requestChannelStatus(channelName)
                .first()
                .get("stream_id")
                .longValue()
    }

    suspend fun requestChannelInfo(channelName: String): GgChannel {
        // https://github.com/GoodGame/API/blob/master/Streams/stream_api.md
        val channelNode = requestChannelStatus(channelName).first()

        return GgChannel(
                channelName,
                channelNode.get("stream_id").longValue(),
                channelNode.get("premium").textValue()!!.toBoolean()
        )
    }

    private suspend fun requestChannelStatus(channelName: String): JsonNode {
        val parameters = "?fmt=json&id=$channelName"

        val request = Request.Builder()
                .get()
                .url(apiUrl + "getchannelstatus" + parameters)
                .build()

        return httpClient.newCall(request)
                .await()
                .use {
                    if (it.code != 200) throw UnexpectedResponseCodeException(it.code)
                    val responseBody = it.body ?: throw UnexpectedResponseException("null body")
                    objectMapper.readTree(responseBody.string())
                }
    }

    private fun parseGlobalEmoticons(content: String): List<GgEmoticon> {
        val matcher = globalEmoticonsPattern.matcher(content)
        if (!matcher.find()) throw UnexpectedResponseException("Couldn't find goodgame global emoticons array")

        val emoticonsNode = objectMapper.readTree(matcher.group(1))

        return emoticonsNode.map { parseEmoticonNode(it) }
    }

    private fun parseChannelEmoticons(content: String): List<GgEmoticon> {
        val matcher = channelEmoticonsPattern.matcher(content)
        if (!matcher.find()) throw UnexpectedResponseException("Couldn't find goodgame channel emoticons array")

        val channelEmoticonsNode = objectMapper.readTree(matcher.group(1))

        return channelEmoticonsNode
                .map { it }
                .flatMap { it }
                .map { parseEmoticonNode(it) }
    }

    private fun parseEmoticonNode(node: JsonNode): GgEmoticon {
        val code = node.get("name").asText()
        val id = node.get("id").asLong()

        val emoticon = GgEmoticon(
                code = code,
                url = node.get("img_big").asText(),
                ggId = id
        )

        if (node.get("animated").asText() == "1") { // "1" for animated, "" for static
            emoticon.animatedInstance = GgEmoticon(
                    code = code,
                    url = node.get("img_gif").asText(),
                    ggId = id
            )
        }

        return emoticon
    }

}
