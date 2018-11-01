package failchat.twitch

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.JsonNode
import failchat.Origin
import failchat.chat.ImageFormat.RASTER
import failchat.chat.badge.ImageBadge
import failchat.exception.ChannelOfflineException
import failchat.exception.DataNotFoundException
import failchat.util.await
import failchat.util.completionCause
import failchat.util.expect
import failchat.util.isEmpty
import failchat.util.nextNonNullToken
import failchat.util.nonNullBody
import failchat.util.objectMapper
import failchat.util.thenUse
import failchat.util.toFuture
import failchat.util.validateResponseCode
import failchat.util.withSuffix
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.toList
import kotlinx.coroutines.experimental.future.future
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.CompletableFuture

class TwitchApiClient(
        private val httpClient: OkHttpClient,
        mainApiUrl: String,
        badgeApiUrl: String,
        private val token: String,
        private val emoticonUrlFactory: TwitchEmoticonUrlFactory
) {

    private val mainApiUrl: String = mainApiUrl.withSuffix("/")
    private val badgeApiUrl: String = badgeApiUrl.withSuffix("/")


    fun requestUserId(userName: String): CompletableFuture<Long> {
        // https://dev.twitch.tv/docs/v5/reference/users/#get-users
        return request("/users", mapOf("login" to userName))
                .parseResponse()
                .thenApply {
                    val usersArray: JsonNode = it.get("users")
                    if (usersArray.isEmpty()) throw DataNotFoundException("Twitch user $userName not found")
                    return@thenApply usersArray.get(0).get("_id").asLong()
                }
    }

    fun requestViewersCount(userId: Long): CompletableFuture<Int> {
        // https://dev.twitch.tv/docs/v5/reference/streams/#get-stream-by-user
        return request("/streams/$userId")
                .parseResponse()
                .thenApply {
                    val streamNode = it.get("stream")
                    if (streamNode.isNull) throw ChannelOfflineException(Origin.TWITCH, userId.toString())
                    return@thenApply streamNode.get("viewers").asInt()
                }
    }

    fun requestEmoticons(): CompletableFuture<List<TwitchEmoticon>> {
        // https://dev.twitch.tv/docs/v5/reference/chat/#get-chat-emoticons-by-set !! Формат ответа без setId не по доке
        // https://dev.twitch.tv/docs/v5/guides/irc/#privmsg-twitch-tags формат ссылки на смайл

        return GlobalScope.future {
            requestEmoticonsToChannel().toList()
        }
    }

    fun requestEmoticonsToChannel(): ReceiveChannel<TwitchEmoticon> {
        // https://dev.twitch.tv/docs/v5/reference/chat/#get-chat-emoticons-by-set !! Формат ответа без setId не по доке
        // https://dev.twitch.tv/docs/v5/guides/irc/#privmsg-twitch-tags формат ссылки на смайл

        val channel = Channel<TwitchEmoticon>(Channel.UNLIMITED)

        request("/chat/emoticon_images")
                .thenUse {
                    val body = it.validateResponseCode(200).nonNullBody

                    val jsonFactory = JsonFactory().apply {
                        codec = objectMapper
                    }
                    val bodyInputStream = body.source().inputStream()
                    val parser = jsonFactory.createParser(bodyInputStream)

                    // parse response. okio thread blocks here
                    parser.expect(JsonToken.START_OBJECT) // root object
                    parser.expect(JsonToken.FIELD_NAME) // 'emoticons' field
                    parser.expect(JsonToken.START_ARRAY) // 'emoticons' array

                    var token = parser.expect(JsonToken.START_OBJECT) // emoticon object

                    while (token != JsonToken.END_ARRAY) {
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
        val id = node.get("id").asLong()
        return TwitchEmoticon(
                twitchId = id,
                regex = node.get("code").asText(),
                urlFactory = emoticonUrlFactory
        )
    }


    private fun request(path: String, parameters: Map<String, String> = emptyMap()): CompletableFuture<Response> {
        val formattedParameters = if (parameters.isEmpty()) {
            ""
        } else {
            parameters
                    .map { (key, value) -> "$key=$value" }
                    .joinToString(separator = "&", prefix = "?")
        }
        val url = mainApiUrl + path.removePrefix("/") + formattedParameters

        val request = Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/vnd.twitchtv.v5+json")
                .header("Client-ID", token)
                .build()

        return httpClient.newCall(request).toFuture()
    }

    private fun CompletableFuture<Response>.parseResponse(): CompletableFuture<JsonNode> {
        return this.thenUse {
            val bodyText = it.validateResponseCode(200).nonNullBody.string()
            objectMapper.readTree(bodyText)
        }
    }


    suspend fun requestGlobalBadges(): Map<TwitchBadgeId, ImageBadge> {
        return requestBadges("/global/display")
    }

    suspend fun requestChannelBadges(channelId: Long): Map<TwitchBadgeId, ImageBadge> {
        return requestBadges("/channels/$channelId/display")
    }

    private suspend fun requestBadges(pathSegment: String): Map<TwitchBadgeId, ImageBadge> {
        val url = badgeApiUrl + pathSegment.removePrefix("/")

        val request = Request.Builder()
                .url(url)
                .get()
                .build()

        val parsedBody = httpClient.newCall(request).await().use {
            val bodyText = it.validateResponseCode(200).nonNullBody.string()
            objectMapper.readTree(bodyText)
        }


        val badges: MutableMap<TwitchBadgeId, ImageBadge> = HashMap()

        val setsNode = parsedBody.get("badge_sets")
        setsNode.fields().forEach { (setId, setNode) ->
            setNode.get("versions").fields().forEach { (version, versionNode) ->
                badges.put(
                        TwitchBadgeId(setId, version),
                        ImageBadge(
                                versionNode.get("image_url_2x").textValue(),
                                RASTER,
                                versionNode.get("title").textValue()
                        )
                )
            }
        }

        return badges
    }

}
