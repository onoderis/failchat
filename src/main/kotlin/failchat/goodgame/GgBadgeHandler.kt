package failchat.goodgame

import failchat.chat.ImageFormat.RASTER
import failchat.chat.MessageHandler
import failchat.chat.badge.Badge
import failchat.chat.badge.CharacterBadge
import failchat.chat.badge.ImageBadge
import javafx.scene.paint.Color
import mu.KLogging
import org.apache.commons.configuration2.Configuration

class GgBadgeHandler(
        private val channel: GgChannel,
        config: Configuration
) : MessageHandler<GgMessage> {

    private companion object : KLogging() {
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
        val sponsorLevelToSponsorBadge: Map<Int, String> = mapOf(
                1 to "&#58892;",
                2 to "&#58897;",
                3 to "&#58895;",
                4 to "&#58896;",
                5 to "&#58887;",
                6 to "&#58930;"
        )
        val subDurationToStarBadge: Map<Int, String> = mapOf(
                1 to "&#59730;",
                2 to "&#59729;",
                3 to "&#59728;",
                4 to "&#59727;",
                5 to "&#59726;",
                6 to "&#59725;"
        )

        const val helperBadgeEntity = "&#59710;"
    }

    private val badgeUrl: String = config.getString("goodgame.badge-url").removeSuffix("/")

    override fun handleMessage(message: GgMessage) {
        val badgeName = message.badgeName
        if (badgeName == "none") return

        if (!channel.premium) {
            if (badgeName == "star") {
                // premium user in non-premium channel (most likely the streamer himself)
                val color = findColorOrDefault(message.authorColorName)
                message.addBadge(CharacterBadge(starBadge, color))
            }
            return
        }

        // channel is premium
        when (badgeName) {
            "star" -> {
                // custom channel badge (image) or goodgame premium badge (star)

                // channel badge have higher priority than global premium badge
                val premiumType = when {
                    message.subscriptionDuration.containsKey(channel.id) -> Premium.CHANNEL
                    message.subscriptionDuration.containsKey(0) -> Premium.GLOBAL
                    else -> {
                        logger.warn("GG user '{}' have 'star' icon, but he is not channel subscriber or global subscriber",
                                message.author.name)
                        return
                    }
                }

                val duration: Int = when (premiumType) {
                    Premium.CHANNEL -> message.subscriptionDuration.get(channel.id)!!
                    Premium.GLOBAL -> message.subscriptionDuration.get(0)!!
                }
                val badge: Badge = when (premiumType) {
                    Premium.CHANNEL -> ImageBadge("$badgeUrl/${channel.id}-$duration-16.png", RASTER)
                    Premium.GLOBAL -> {
                        val badgeEntity = subDurationToStarBadge.get(duration) ?: run {
                            logger.warn("Unknown global premium badge. user: '{}', duration: {}", message.author.name, duration)
                            return
                        }

                        // global subscriber with color 'premium-personal' actually have 'premium' color
                        val realColorName = message.authorColorName.let {
                            if (it == "premium-personal") "premium"
                            else it
                        }

                        CharacterBadge(badgeEntity, findColorOrDefault(realColorName))
                    }
                }

                message.addBadge(badge)
            }

            "" -> {
                // todo other rights
                if (message.authorRights == 10) {
                    message.addBadge(CharacterBadge(
                            helperBadgeEntity,
                            GgColors.byRole.get(message.authorColorName) ?: findColorOrDefault("streamer-helper")
                    ))
                }
            }

            else -> {
                // probably sponsor badge
                val badgeCharacterEntity = badgeNameToCharEntity.get(badgeName)
                        ?: sponsorLevelToSponsorBadge.get(message.sponsorLevel) // reproduce GG behaviour
                        ?: return

                message.addBadge(CharacterBadge(badgeCharacterEntity, findColorOrDefault(message.authorColorName)))
            }
        }
    }

    private fun findColorOrDefault(ggColorName: String): Color {
        return GgColors.byRole.get(ggColorName) ?: GgColors.defaultColor
    }

    private enum class Premium {
        GLOBAL, CHANNEL
    }

}
