package failchat.goodgame

import failchat.chat.MessageHandler
import failchat.chat.badge.CharacterBadge
import failchat.chat.badge.ImageBadge
import org.apache.commons.configuration2.Configuration

class GgBadgeHandler(
        private val channel: GgChannel,
        config: Configuration
) : MessageHandler<GgMessage> {

    private companion object {
        const val defaultColor = "#73adff"
        val colorMap: Map<String, String> = mapOf(
                "bronze" to "#e7820a",
                "silver" to "#b4b4b4",
                "gold" to "#eefc08",
                "diamond" to "#8781bd",
                "king" to "#30d5c8",
                "top-one" to "#3BCBFF",
                "premium" to "#BD70D7",
                "premium-personal" to "#31a93a",
                "moderator" to "#ec4058",
                "streamer" to "#e8bb00",
                "streamer-helper" to "#e8bb00"
        )
        /* Following html entities should be displayed in icomoon font. */
        const val starBadge = "&#59730;"
        val badgeNameToCharEntity: Map<String, String> = mapOf(
                "coin" to "&#58892;",
                "eagle" to "&#58897;",
                "cup" to "&#58895;",
                "diamond" to "&#58896;",
                "crown" to "&#58887;",
                "top1" to "&#58930;"
        )
        val sponsorLevelToCharEntity: Map<Int, String> = mapOf(
                1 to "&#58892;",
                2 to "&#58897;",
                3 to "&#58895;",
                4 to "&#58896;",
                5 to "&#58887;",
                6 to "&#58930;"
        )
    }

    private val badgeUrl: String = config.getString("goodgame.badge-url").removeSuffix("/")

    override fun handleMessage(message: GgMessage) {
        val badgeName = message.badgeName
        if (badgeName == "none") return

        if (!channel.premium) {
            if (badgeName == "star") {
                // premium user in non-premium channel (most likely the streamer himself)
                val color = colorMap.get(message.authorColorName) ?: defaultColor
                message.addBadge(CharacterBadge(starBadge, color))
            }
            return
        }

        // channel is premium
        when (badgeName) {
            "star" -> {
                // custom badge
                val duration: Int = message.subscriptionDuration.get(channel.id) ?: return
                val badge = ImageBadge("$badgeUrl/${channel.id}-$duration-16.png")
                message.addBadge(badge)
            }

            else -> {
                // probably paid badge
                val badgeCharacterEntity = badgeNameToCharEntity.get(badgeName)
                        ?: sponsorLevelToCharEntity.get(message.sponsorLevel) // reproduce GG behaviour
                        ?: return

                val color = colorMap.get(message.authorColorName) ?: defaultColor
                message.addBadge(CharacterBadge(badgeCharacterEntity, color))
            }
        }
    }

}