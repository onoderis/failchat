package failchat.twitch

import failchat.chat.MessageHandler
import failchat.chat.badge.Badge
import failchat.chat.badge.BadgeFinder
import failchat.chat.badge.BadgeOrigin.TWITCH_CHANNEL
import failchat.chat.badge.BadgeOrigin.TWITCH_GLOBAL
import mu.KLogging

class TwitchBadgeHandler(
        private val badgeFinder: BadgeFinder
) : MessageHandler<TwitchMessage> {

    private companion object : KLogging()

    override fun handleMessage(message: TwitchMessage) {
        val badgesTag = message.badgesTag ?: return

        val messageBadgeIds = parseBadgesTag(badgesTag)

        messageBadgeIds.forEach { messageBadgeId ->
            val badge: Badge? = badgeFinder.findBadge(TWITCH_CHANNEL, messageBadgeId)
                    ?: badgeFinder.findBadge(TWITCH_GLOBAL, messageBadgeId)

            if (badge == null) {
                logger.debug("Badge not found. Origin: {}, {}; badge id: {}", TWITCH_CHANNEL, TWITCH_GLOBAL, messageBadgeId)
                return@forEach
            }

            message.addBadge(badge)
        }
    }

    private fun parseBadgesTag(badgesTag: String): List<TwitchBadgeId> {
        return badgesTag
                .split(',')
                .asSequence()
                .map { it.split('/', limit = 2) }
                .map {
                    val version = if (it.size < 2) "" else it[1]
                    TwitchBadgeId(it[0], version)
                }
                .toList()
    }

}