package failchat.twitch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import failchat.Origin
import failchat.exception.ChannelOfflineException
import failchat.exception.DataNotFoundException
import failchat.exception.UnexpectedResponseCodeException
import failchat.exception.UnexpectedResponseException
import failchat.util.isEmpty
import failchat.util.thenApplySafe
import failchat.util.toFuture
import failchat.util.withSuffix
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class TwitchApiClient(
        private val httpClient: OkHttpClient,
        apiUrl: String,
        private val token: String,
        private val objectMapper: ObjectMapper = ObjectMapper()
) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(TwitchApiClient::class.java)
    }

    private val apiUrl: String = apiUrl.withSuffix("/")


    fun requestUserId(userName: String): CompletableFuture<Long> {
        // https://dev.twitch.tv/docs/v5/reference/users/#get-users
        return request("/users", mapOf("login" to userName))
                .thenApply {
                    val usersArray: JsonNode = it.get("users")
                    if (usersArray.isEmpty()) throw DataNotFoundException("Twitch user $userName not found")
                    return@thenApply usersArray.get(0).get("_id").asLong()
                }
    }

    fun requestViewersCount(userId: Long): CompletableFuture<Int> {
        // https://dev.twitch.tv/docs/v5/reference/streams/#get-stream-by-user
        return request("/streams/$userId")
                .thenApply {
                    val streamNode = it.get("stream")
                    if (streamNode.isNull) throw ChannelOfflineException(Origin.twitch, userId.toString())
                    return@thenApply streamNode.get("viewers").asInt()
                }
    }

    fun requestEmoticons(): CompletableFuture<List<TwitchEmoticon>> {
        // https://dev.twitch.tv/docs/v5/reference/chat/#get-chat-emoticons-by-set !! Формат ответа без setId не по доке
        // https://dev.twitch.tv/docs/v5/guides/irc/#privmsg-twitch-tags формат ссылки на смайл
        return request("/chat/emoticon_images").thenApply {
            it.get("emoticons").map {
                val id = it.get("id").asLong()
                TwitchEmoticon(
                        id,
                        regex = it.get("code").asText(),
                        url = "http://static-cdn.jtvnw.net/emoticons/v1/$id/1.0"
                )
            }
        }
    }

    fun request(path: String, parameters: Map<String, String> = emptyMap()): CompletableFuture<JsonNode> {
        val formattedParameters = if (parameters.isEmpty()) {
            ""
        } else {
            parameters
                    .map { (key, value) -> "$key=$value" }
                    .joinToString(separator = "&", prefix = "?")
        }
        val url = apiUrl + path.removePrefix("/") + formattedParameters

        val request = Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/vnd.twitchtv.v5+json")
                .header("Client-ID", token)
                .build()

        return httpClient.newCall(request)
                .toFuture()
                .thenApplySafe {
                    if (it.code() != 200) throw UnexpectedResponseCodeException(it.code())
                    val responseBody = it.body() ?: throw UnexpectedResponseException("null body")
                    return@thenApplySafe objectMapper.readTree(responseBody.string())
                }
    }

}
