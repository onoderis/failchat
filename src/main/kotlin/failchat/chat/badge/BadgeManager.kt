package failchat.chat.badge

import failchat.chat.badge.BadgeOrigin.TWITCH_GLOBAL
import failchat.twitch.TwitchApiClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class BadgeManager(
        private val badgeStorage: BadgeStorage,
        private val twitchApiClient: TwitchApiClient
) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(BadgeManager::class.java)
    }

    suspend fun loadGlobalBadges() {
        val twitchGlobalBadges = twitchApiClient.requestGlobalBadges()
        log.info("Global twitch badges was received. Count: {}", twitchGlobalBadges.size)

        badgeStorage.putBadges(TWITCH_GLOBAL, twitchGlobalBadges)
    }

    suspend fun loadTwitchChannelBadges(channelId: Long) {
        val twitchChannelBadges = twitchApiClient.requestChannelBadges(channelId)
        log.info("Channel badges was received for twitch channel '{}'. Count: {}", channelId, twitchChannelBadges.size)

        badgeStorage.putBadges(TWITCH_GLOBAL, twitchChannelBadges)
    }

    fun resetChannelBadges() {
        badgeStorage.putBadges(TWITCH_GLOBAL, emptyMap())
    }

}