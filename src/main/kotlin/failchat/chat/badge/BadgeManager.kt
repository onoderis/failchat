package failchat.chat.badge

import failchat.chat.badge.BadgeOrigin.PEKA2TV
import failchat.chat.badge.BadgeOrigin.TWITCH_CHANNEL
import failchat.chat.badge.BadgeOrigin.TWITCH_GLOBAL
import failchat.peka2tv.Peka2tvApiClient
import failchat.twitch.TwitchApiClient
import failchat.util.CoroutineExceptionLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.await
import mu.KLogging

class BadgeManager(
        private val badgeStorage: BadgeStorage,
        private val twitchApiClient: TwitchApiClient,
        private val peka2tvApiClient: Peka2tvApiClient
) {

    private companion object : KLogging()

    suspend fun loadGlobalBadges() {
        val jobsList: MutableList<Deferred<Unit>> = ArrayList()

        jobsList += CoroutineScope(Dispatchers.Default + CoroutineExceptionLogger).async {
            val twitchBadges = twitchApiClient.getGlobalBadges()
            logger.info("Global twitch badges was loaded. Count: {}", twitchBadges.size)
            badgeStorage.putBadges(TWITCH_GLOBAL, twitchBadges)
        }

        jobsList += CoroutineScope(Dispatchers.Default + CoroutineExceptionLogger).async {
            val peka2tvBadges = peka2tvApiClient.requestBadges().await()
            logger.info("Peka2tv badges was loaded. Count: {}", peka2tvBadges.size)
            badgeStorage.putBadges(PEKA2TV, peka2tvBadges)
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
