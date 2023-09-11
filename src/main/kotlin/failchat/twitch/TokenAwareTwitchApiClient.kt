package failchat.twitch

import failchat.chat.badge.ImageBadge
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The [TwitchApiClient] wrapper that:
 * - reuses existing token.
 * - retries the request if the token is expired.
 * */
class TokenAwareTwitchApiClient(
        private val twitchApiClient: TwitchApiClient,
        private val clientSecret: String,
        private val tokenContainer: HelixTokenContainer
) {

    private val mutex = Mutex() // forbid parallel execution to prevent multiple tokens generation in the same time

    suspend fun getUserId(userName: String): Long {
        return mutex.withLock {
            doWithRetryOnAuthError(twitchApiClient, clientSecret, tokenContainer) {
                twitchApiClient.getUserId(userName, it)
            }
        }
    }

    suspend fun getViewersCount(userName: String): Int {
        return mutex.withLock {
            doWithRetryOnAuthError(twitchApiClient, clientSecret, tokenContainer) {
                twitchApiClient.getViewersCount(userName, it)
            }
        }
    }

    suspend fun getGlobalEmoticons(): List<TwitchEmoticon> {
        return mutex.withLock {
            doWithRetryOnAuthError(twitchApiClient, clientSecret, tokenContainer) {
                twitchApiClient.getGlobalEmoticons(it)
            }
        }
    }

    suspend fun getFirstLiveChannelName(): String {
        return mutex.withLock {
            doWithRetryOnAuthError(twitchApiClient, clientSecret, tokenContainer) {
                twitchApiClient.getFirstLiveChannelName(it)
            }
        }
    }

    suspend fun getGlobalBadges(): Map<TwitchBadgeId, ImageBadge> {
        return mutex.withLock {
            doWithRetryOnAuthError(twitchApiClient, clientSecret, tokenContainer) {
                twitchApiClient.getGlobalBadges(it)
            }
        }
    }

    suspend fun getChannelBadges(channelId: Long): Map<TwitchBadgeId, ImageBadge> {
        return mutex.withLock {
            doWithRetryOnAuthError(twitchApiClient, clientSecret, tokenContainer) {
                twitchApiClient.getChannelBadges(channelId, it)
            }
        }
    }
}
