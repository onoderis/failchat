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
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CompletableFuture
import kotlin.reflect.KClass

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
        val usersUrl = "$helixApiUrl/users".toHttpUrl()
        val globalBadgesUrl = "$helixApiUrl/chat/badges/global".toHttpUrl()
        val channelBadgesUrl = "$helixApiUrl/chat/badges".toHttpUrl()
    }

    // https://dev.twitch.tv/docs/api/reference/#get-users
    suspend fun getUserId(userName: String): Long {
        val url = usersUrl.newBuilder().addQueryParameter("login", userName).build()
        val response = doRequest(url, UsersResponse::class)

        if (response.data.isEmpty()) {
            throw DataNotFoundException("Twitch user $userName not found")
        }

        return response.data.first().id
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
        // https://dev.twitch.tv/docs/api/reference/#get-global-chat-badges
        return getBadges(globalBadgesUrl)
    }

    suspend fun getChannelBadges(channelId: Long): Map<TwitchBadgeId, ImageBadge> {
        // https://dev.twitch.tv/docs/api/reference/#get-channel-chat-badges
        val url = channelBadgesUrl.newBuilder().addQueryParameter("broadcaster_id", channelId.toString()).build()
        return getBadges(url)
    }

    private suspend fun getBadges(url: HttpUrl): Map<TwitchBadgeId, ImageBadge> {
        val badgesResponse = doRequest(url, BadgesResponse::class)

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

    private suspend fun <T : Any> doRequest(url: HttpUrl, responseType: KClass<T>): T {
        val token = getOrGenerateToken()

        val request = Request.Builder()
                .get()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Client-Id", clientId)
                .build()

        return httpClient.newCall(request).await().use { response ->
            if (!response.isSuccessful) {
                throw UnexpectedResponseCodeException(response.code, url.toString())
            }
            objectMapper.readValue(response.nonNullBody.charStream(), responseType.java)
        }
    }

    private suspend fun getOrGenerateToken(): String {
        val token = tokenContainer.getToken()

        if (token == null) {
            val newToken = generateToken()
            tokenContainer.setToken(newToken)
            return newToken.value
        }

        logger.info("Helix token was retrieved from configuration")
        return token.value
    }

    private suspend fun generateToken(): HelixApiToken {
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
        val authResponse = objectMapper.readValue<AuthResponse>(body)

        val token = HelixApiToken(
                value = authResponse.accessToken,
                ttl = Instant.now() + Duration.ofSeconds(authResponse.expiresIn) - Duration.ofSeconds(60)
        )
        logger.info("New helix token was generated")
        return token
    }
}
