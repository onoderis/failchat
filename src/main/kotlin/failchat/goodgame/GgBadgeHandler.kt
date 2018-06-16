package failchat.goodgame

import failchat.chat.MessageHandler
import failchat.chat.badge.Badge
import org.apache.commons.configuration2.Configuration

class GgBadgeHandler(
        private val channel: GgChannel,
        config: Configuration
): MessageHandler<GgMessage> {

    private val badgeUrl: String = config.getString("goodgame.badge-url").removeSuffix("/")

    override fun handleMessage(message: GgMessage) {
        if (!channel.premium) return

        val duration: Int = message.subscriptionDuration.get(channel.id) ?: return

        val badge = Badge("$badgeUrl/${channel.id}-$duration-16.png")
        message.addBadge(badge)
    }

}