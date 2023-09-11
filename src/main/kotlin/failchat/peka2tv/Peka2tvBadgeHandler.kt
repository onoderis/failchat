package failchat.peka2tv

import failchat.chat.MessageHandler
import failchat.chat.badge.Badge
import failchat.chat.badge.BadgeFinder
import failchat.chat.badge.BadgeOrigin.PEKA2TV
import mu.KotlinLogging

class Peka2tvBadgeHandler(private val badgeFinder: BadgeFinder) : MessageHandler<Peka2tvMessage> {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    override fun handleMessage(message: Peka2tvMessage) {
        if (message.badgeId == 0L) return

        val badge: Badge? = badgeFinder.findBadge(PEKA2TV, message.badgeId)
        if (badge == null) {
            logger.debug("Badge not found. Origin: {}; badge id: {}", PEKA2TV, message.badgeId)
            return
        }

        message.addBadge(badge)
    }

}
