package failchat.chat.badge

import failchat.chat.badge.BadgeOrigin.TWITCH_CHANNEL
import failchat.chat.badge.BadgeOrigin.TWITCH_GLOBAL
import failchat.twitch.TokenAwareTwitchApiClient
import failchat.util.CoroutineExceptionLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import mu.KotlinLogging

class BadgeManager(
    private val badgeStorage: BadgeStorage,
    private val twitchApiClient: TokenAwareTwitchApiClient
) {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    suspend fun loadGlobalBadges() {
        val jobsList: MutableList<Deferred<Unit>> = ArrayList()

        jobsList += CoroutineScope(Dispatchers.Default + CoroutineExceptionLogger).async {
            val twitchBadges = twitchApiClient.getGlobalBadges()
            logger.info("Global twitch badges was loaded. Count: {}", twitchBadges.size)
            badgeStorage.putBadges(TWITCH_GLOBAL, twitchBadges)
        }

        jobsList.forEach { it.join() }
    }

    suspend fun loadTwitchChannelBadges(channelId: Long) {
        val twitchBadges = twitchApiClient.getChannelBadges(channelId)
        logger.info("Channel badges was received for twitch channel '{}'. Count: {}", channelId, twitchBadges.size)

        badgeStorage.putBadges(TWITCH_CHANNEL, twitchBadges)
    }

    fun resetChannelBadges() {
        badgeStorage.putBadges(TWITCH_CHANNEL, emptyMap())
    }

}
