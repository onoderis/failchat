package failchat.twitch

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import failchat.Origin
import failchat.chat.ImageFormat
import failchat.chat.badge.ImageBadge
import failchat.exception.ChannelOfflineException
import failchat.exception.DataNotFoundException
import failchat.exception.UnexpectedResponseCodeException
import failchat.util.await
import failchat.util.getBodyIfStatusIs
import failchat.util.nonNullBody
import failchat.util.thenUse
import failchat.util.toFuture
import mu.KLogging
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture

class TwitchApiClient(
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper,
        private val clientId: String,
        private val clientSecret: String,
        private val emoticonUrlFactory: TwitchEmoticonUrlFactory,
        private val tokenContainer: HelixTokenContainer
) {

    private companion object : KLogging() {
        const val oauthUrl = "https://id.twitch.tv/oauth2/token"
        const val krakenApiUrl = "https://api.twitch.tv/kraken"
        const val helixApiUrl = "https://api.twitch.tv/helix"
        const val globalBadgesUrl = "$helixApiUrl/chat/badges/global"
        const val channelBadgesUrl = "$helixApiUrl/chat/badges"
    }

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
        val url = krakenApiUrl + path.removePrefix("/") + formattedParameters

        val request = Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/vnd.twitchtv.v5+json")
                .header("Client-ID", clientId)
                .build()

        return httpClient.newCall(request).toFuture()
    }

    private fun CompletableFuture<Response>.parseResponse(): CompletableFuture<JsonNode> {
        return this.thenUse {
            val bodyText = it.getBodyIfStatusIs(200).nonNullBody.string()
            objectMapper.readTree(bodyText)
        }
    }


    suspend fun getGlobalBadges(): Map<TwitchBadgeId, ImageBadge> {
        return getBadges(globalBadgesUrl)
    }

    suspend fun getChannelBadges(channelId: Long): Map<TwitchBadgeId, ImageBadge> {
        return getBadges("$channelBadgesUrl?broadcaster_id=$channelId")
    }

    private suspend fun getBadges(url: String): Map<TwitchBadgeId, ImageBadge> {
        val token = getOrGenerateToken()

        val request = Request.Builder()
                .get()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Client-Id", clientId)
                .build()

        val response = httpClient.newCall(request).await()
        if (!response.isSuccessful) {
            throw UnexpectedResponseCodeException(response.code, url)
        }

        val badgesResponse = objectMapper.readValue(response.nonNullBody.string(), BadgesResponse::class.java)

        return badgesResponse.data
                .flatMap { data ->
                    data.versions.map { data.setId to it }
                }
                .associate { (setId, version) ->
                    val tbi = TwitchBadgeId(setId, version.id)
                    val ib = ImageBadge(version.imageUrl1x, ImageFormat.RASTER, version.description)
                    tbi to ib
                }
    }

    private suspend fun getOrGenerateToken(): String {
        val token = tokenContainer.getToken()

        if (token == null) {
            val newToken = generateAccessToken()
            tokenContainer.setToken(newToken)
            return newToken.value
        }

        logger.info("Helix token was retrieved from configuration")
        return token.value
    }

    private suspend fun generateAccessToken(): HelixApiToken {
        val request = Request.Builder()
                .url(oauthUrl)
                .post(FormBody.Builder()
                        .add("client_id", clientId)
                        .add("client_secret", clientSecret)
                        .add("grant_type", "client_credentials")
                        .build()
                )
                .addHeader("Accept", "application/json")
                .build()

        val response = httpClient.newCall(request).await()
        if (!response.isSuccessful) {
            throw UnexpectedResponseCodeException(200, oauthUrl)
        }

        val body = response.nonNullBody.string()
        val bodyNode = objectMapper.readTree(body)

        val token = HelixApiToken(
                value = bodyNode.get("access_token").textValue(),
                ttl = Instant.now() + Duration.ofSeconds(bodyNode.get("expires_in").longValue()) - Duration.ofSeconds(60)
        )
        logger.info("New helix token was generated")
        return token
    }
}
