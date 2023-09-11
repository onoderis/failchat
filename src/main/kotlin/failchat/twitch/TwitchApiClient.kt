package failchat.twitch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import failchat.Origin
import failchat.chat.ImageFormat
import failchat.chat.badge.ImageBadge
import failchat.exception.ChannelNotFoundException
import failchat.exception.ChannelOfflineException
import failchat.exception.UnexpectedResponseCodeException
import failchat.util.await
import failchat.util.nonNullBody
import mu.KotlinLogging
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KClass

class TwitchApiClient(
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper,
        private val clientId: String
) {

    private companion object {
        val logger = KotlinLogging.logger {}
        const val oauthUrl = "https://id.twitch.tv/oauth2/token"
        const val helixApiUrl = "https://api.twitch.tv/helix"
        val usersUrl = "$helixApiUrl/users".toHttpUrl()
        val streamsUrl = "$helixApiUrl/streams".toHttpUrl()
        val globalEmotesUrl = "$helixApiUrl/chat/emotes/global".toHttpUrl()
        val globalBadgesUrl = "$helixApiUrl/chat/badges/global".toHttpUrl()
        val channelBadgesUrl = "$helixApiUrl/chat/badges".toHttpUrl()
    }

    // https://dev.twitch.tv/docs/api/reference/#get-users
    suspend fun getUserId(userName: String, token: String): Long {
        val url = usersUrl.newBuilder().addQueryParameter("login", userName).build()
        val response = doRequest(url, token, UsersResponse::class)

        if (response.data.isEmpty()) {
            throw ChannelNotFoundException("Twitch user $userName not found")
        }

        return response.data.first().id
    }

    // https://dev.twitch.tv/docs/api/reference/#get-streams
    suspend fun getViewersCount(userName: String, token: String): Int {
        val url = streamsUrl.newBuilder().addQueryParameter("user_login", userName).build()
        val response = doRequest(url, token, StreamsResponse::class)

        if (response.data.isEmpty()) {
            throw ChannelOfflineException(Origin.TWITCH, userName)
        }

        return response.data.first().viewerCount
    }

    // https://dev.twitch.tv/docs/api/reference/#get-global-emotes
    suspend fun getGlobalEmoticons(token: String): List<TwitchEmoticon> {
        val response = doRequest(globalEmotesUrl, token, EmotesResponse::class)
        return response.data.map {
            TwitchEmoticon(
                    twitchId = it.id,
                    code = it.name
            )
        }
    }

    // https://dev.twitch.tv/docs/api/reference/#get-streams
    suspend fun getFirstLiveChannelName(token: String): String {
        val url = streamsUrl.newBuilder()
                .addQueryParameter("type", "live")
                .addQueryParameter("first", "1")
                .build()
        val response = doRequest(url, token, StreamsResponse::class)
        return response.data.first().userLogin
    }

    suspend fun getGlobalBadges(token: String): Map<TwitchBadgeId, ImageBadge> {
        // https://dev.twitch.tv/docs/api/reference/#get-global-chat-badges
        return getBadges(globalBadgesUrl, token)
    }

    suspend fun getChannelBadges(channelId: Long, token: String): Map<TwitchBadgeId, ImageBadge> {
        // https://dev.twitch.tv/docs/api/reference/#get-channel-chat-badges
        val url = channelBadgesUrl.newBuilder().addQueryParameter("broadcaster_id", channelId.toString()).build()
        return getBadges(url, token)
    }

    private suspend fun getBadges(url: HttpUrl, token: String): Map<TwitchBadgeId, ImageBadge> {
        val badgesResponse = doRequest(url, token, BadgesResponse::class)

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

    private suspend fun <T : Any> doRequest(url: HttpUrl, token: String, responseType: KClass<T>): T {
        val request = Request.Builder()
                .get()
                .url(url)
                .header("Authorization", "Bearer $token")
                .header("Client-Id", clientId)
                .build()

        return httpClient.newCall(request).await().use { response ->
            if (response.code == 401) {
                throw InvalidTokenException()
            }
            if (!response.isSuccessful) {
                throw UnexpectedResponseCodeException(response.code, url.toString())
            }
            objectMapper.readValue(response.nonNullBody.charStream(), responseType.java)
        }
    }

    suspend fun generateToken(clientSecret: String): HelixApiToken {
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
            throw UnexpectedResponseCodeException(response.code, oauthUrl)
        }

        val body = response.nonNullBody.string()
        val authResponse = objectMapper.readValue<AuthResponse>(body)

        val token = HelixApiToken(
                value = authResponse.accessToken,
                expiresAt = Instant.now() + Duration.ofSeconds(authResponse.expiresIn) - Duration.ofSeconds(60)
        )
        logger.info("New helix token was generated")
        return token
    }
}
