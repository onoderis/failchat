package failchat.goodgame

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import failchat.core.Origin
import failchat.exception.ChannelOfflineException
import failchat.exception.UnexpectedResponseCodeException
import failchat.exception.UnexpectedResponseException
import failchat.util.thenApplySafe
import failchat.util.toFuture
import failchat.util.withSuffix
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern

class GgApiClient(
        private val httpClient: OkHttpClient,
        apiUrl: String,
        private val emoticonsJsUrl: String,
        private val objectMapper: ObjectMapper = ObjectMapper()
) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GgApiClient::class.java)
        val globalEmoticonsPattern = Pattern.compile("""Smiles : (\[.+?\]),""")
        val channelEmoticonsPattern = Pattern.compile("""Channel_Smiles : (\{.+?\}\]\}),""")
    }

    private val apiUrl = apiUrl.withSuffix("/")

    fun requestEmoticonList(): CompletableFuture<List<GgEmoticon>> {
        val request = Request.Builder()
                .url(emoticonsJsUrl)
                .get()
                .build()

        return httpClient.newCall(request)
                .toFuture()
                .thenApplySafe {
                    if (it.code() != 200) throw UnexpectedResponseCodeException(it.code())
                    val responseBody = it.body() ?: throw UnexpectedResponseException("null body")
                    val jsContent = responseBody.string()
                    return@thenApplySafe parseGlobalEmoticons(jsContent) + parseChannelEmoticons(jsContent)
                }
    }

    fun requestChannelId(channelName: String): CompletableFuture<Long> {
        // https://github.com/GoodGame/API/blob/master/Streams/stream_api.md
        return requestChannelStatus(channelName)
                .thenApply { it.first().get("stream_id").asText().toLong() }
    }

    fun requestViewersCount(channelName: String): CompletableFuture<Int> {
        // https://github.com/GoodGame/API/blob/master/Streams/stream_api.md
        return requestChannelStatus(channelName)
                .thenApply {
                    val statusNode = it.first()
                    if (statusNode.get("status").asText() != "Live") throw ChannelOfflineException(Origin.goodgame, channelName)
                    return@thenApply statusNode.get("viewers").asText().toInt()
                }
    }

    fun requestChannelStatus(channelName: String): CompletableFuture<JsonNode> {
        val parameters = "?fmt=json&id=$channelName"

        val request = Request.Builder()
                .url(apiUrl + "getchannelstatus" + parameters)
                .get()
                .build()

        return httpClient.newCall(request)
                .toFuture()
                .thenApplySafe {
                    if (it.code() != 200) throw UnexpectedResponseCodeException(it.code())
                    val responseBody = it.body() ?: throw UnexpectedResponseException("null body")
                    return@thenApplySafe objectMapper.readTree(responseBody.string())
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
