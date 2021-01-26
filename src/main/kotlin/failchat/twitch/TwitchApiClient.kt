package failchat.twitch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import failchat.Origin
import failchat.chat.ImageFormat.RASTER
import failchat.chat.badge.ImageBadge
import failchat.exception.ChannelOfflineException
import failchat.exception.DataNotFoundException
import failchat.util.await
import failchat.util.getBodyIfStatusIs
import failchat.util.isEmpty
import failchat.util.nonNullBody
import failchat.util.thenUse
import failchat.util.toFuture
import failchat.util.withSuffix
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.CompletableFuture

class TwitchApiClient(
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper,
        mainApiUrl: String,
        badgeApiUrl: String,
        private val token: String,
        private val emoticonUrlFactory: TwitchEmoticonUrlFactory
) {

    private val mainApiUrl: String = mainApiUrl.withSuffix("/")
    private val badgeApiUrl: String = badgeApiUrl.withSuffix("/")


    fun getUserId(userName: String): CompletableFuture<Long> {
        // https://dev.twitch.tv/docs/v5/reference/users/#get-users
        return request("/users", mapOf("login" to userName))
                .parseResponse()
                .thenApply {
                    val usersArray: JsonNode = it.get("users")
                    if (usersArray.isEmpty()) throw DataNotFoundException("Twitch user $userName not found")
                    return@thenApply usersArray.get(0).get("_id").asLong()
                }
    }

    fun getViewersCount(userId: Long): CompletableFuture<Int> {
        // https://dev.twitch.tv/docs/v5/reference/streams/#get-stream-by-user
        return request("/streams/$userId")
                .parseResponse()
                .thenApply {
                    val streamNode = it.get("stream")
                    if (streamNode.isNull) throw ChannelOfflineException(Origin.TWITCH, userId.toString())
                    return@thenApply streamNode.get("viewers").asInt()
                }
    }

    fun getCommonEmoticons(): CompletableFuture<List<TwitchEmoticon>> {
        return request("/chat/emoticon_images", mapOf("emotesets" to "0"))
                .thenUse {
                    val bodyText = it.getBodyIfStatusIs(200).nonNullBody.string()
                    val response = objectMapper.readValue<EmoticonSetsResponse>(bodyText)

                    response.emoticonSets
                            .flatMap { it.value }
                            .map {
                                TwitchEmoticon(
                                        twitchId = it.id,
                                        code = it.code,
                                        urlFactory = emoticonUrlFactory
                                )
                            }
                }
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
            val bodyText = it.getBodyIfStatusIs(200).nonNullBody.string()
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
            val bodyText = it.getBodyIfStatusIs(200).nonNullBody.string()
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
