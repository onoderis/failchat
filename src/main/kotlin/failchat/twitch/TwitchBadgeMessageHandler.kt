package failchat.twitch

import failchat.chat.Badge
import failchat.chat.MessageHandler

class TwitchBadgeMessageHandler : MessageHandler<TwitchMessage> {

    var badges: Map<TwitchBadgeId, Badge> = emptyMap()

    override fun handleMessage(message: TwitchMessage) {
        val badgesTag = message.badgesTag ?: return

        val localBadges = badges
        if (localBadges.isEmpty()) return

        val messageBadgeIds = parseBadgesTag(badgesTag)

        messageBadgeIds.forEach { messageBadgeId ->
            localBadges.get(messageBadgeId)?.let {
                message.addBadge(it)
            }
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