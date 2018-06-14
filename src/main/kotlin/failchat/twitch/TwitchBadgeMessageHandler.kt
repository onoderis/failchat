package failchat.twitch

import failchat.chat.Badge
import failchat.chat.MessageHandler

class TwitchBadgeMessageHandler(
        private val globalBadges: Map<TwitchBadgeId, Badge>,
        private val channelBadges: Map<TwitchBadgeId, Badge>
) : MessageHandler<TwitchMessage> {

    override fun handleMessage(message: TwitchMessage) {
        val badgesTag = message.badgesTag ?: return

        val badges = parseBadgesTag(badgesTag)
        val subscriberBadgeId = badges.find { it.setId == "subscriber" }

        channelBadges.get(subscriberBadgeId)?.let {
            message.addBadge(it)
        }

        //todo global badges
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