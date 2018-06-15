package failchat.twitch

import failchat.chat.MessageHandler
import failchat.chat.badge.BadgeFinder
import failchat.chat.badge.BadgeOrigin

class TwitchBadgeMessageHandler(
        private val badgeFinder: BadgeFinder
) : MessageHandler<TwitchMessage> {

    override fun handleMessage(message: TwitchMessage) {
        val badgesTag = message.badgesTag ?: return

        val messageBadgeIds = parseBadgesTag(badgesTag)

        messageBadgeIds.forEach { messageBadgeId ->
            val badge = badgeFinder.findBadge(BadgeOrigin.TWITCH_CHANNEL, messageBadgeId)
                    ?: badgeFinder.findBadge(BadgeOrigin.TWITCH_GLOBAL, messageBadgeId)
            badge?.let { message.addBadge(it) }
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