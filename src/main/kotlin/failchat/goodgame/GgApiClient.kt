package failchat.goodgame

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import failchat.Origin
import failchat.exception.ChannelOfflineException
import failchat.exception.UnexpectedResponseCodeException
import failchat.exception.UnexpectedResponseException
import failchat.util.await
import failchat.util.withSuffix
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class GgApiClient(
        private val httpClient: OkHttpClient,
        apiUrl: String,
        private val emoticonsJsUrl: String,
        private val objectMapper: ObjectMapper = ObjectMapper()
) {

    private companion object {
        val globalEmoticonsPattern: Pattern = Pattern.compile("""Smiles : (\[.+?\]),""")
        val channelEmoticonsPattern: Pattern = Pattern.compile("""Channel_Smiles : (\{.+?\}\]\}),""")
        val log: Logger = LoggerFactory.getLogger(GgApiClient::class.java)
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
                    if (it.code() != 200) throw UnexpectedResponseCodeException(it.code())
                    val responseBody = it.body() ?: throw UnexpectedResponseException("null body")
                    val jsContent = responseBody.string()
                    parseGlobalEmoticons(jsContent) + parseChannelEmoticons(jsContent)
                }
    }

    suspend fun requestChannelId(channelName: String): Long {
        // https://github.com/GoodGame/API/blob/master/Streams/stream_api.md
        return requestChannelStatus(channelName)
                .first()
                .get("stream_id")
                .textValue()
                .toLong()
    }

    suspend fun requestChannelInfo(channelName: String): GgChannel {
        // https://github.com/GoodGame/API/blob/master/Streams/stream_api.md
        val channelNode = requestChannelStatus(channelName).first()

        return GgChannel(
                channelName,
                channelNode.get("stream_id").textValue().toLong(),
                channelNode.get("premium").textValue()!!.toBoolean()
        )
    }

    suspend fun requestViewersCount(channelName: String): Int {
        // https://github.com/GoodGame/API/blob/master/Streams/stream_api.md
        val response = requestChannelStatus(channelName)
        val statusNode = response.first()
        if (statusNode.get("status").asText() != "Live") throw ChannelOfflineException(Origin.GOODGAME, channelName)
        return statusNode.get("viewers").asText().toInt()
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
                    if (it.code() != 200) throw UnexpectedResponseCodeException(it.code())
                    val responseBody = it.body() ?: throw UnexpectedResponseException("null body")
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
        val emoticon = GgEmoticon(
                code = code,
                url = node.get("img_big").asText()
        )

        if (node.get("animated").asBoolean()) {
            emoticon.animatedInstance = GgEmoticon(
                    code = code,
                    url = node.get("img_gif").asText()
            )
        }

        return emoticon
    }

}
