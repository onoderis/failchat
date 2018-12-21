package failchat

@Suppress("ClassName")
object ConfigKeys {

    //todo user properties from here

    object peka2tv {
        const val enabled = "peka2tv.enabled"
        const val channel = "peka2tv.channel"
    }

    object goodgame {
        const val enabled = "goodgame.enabled"
        const val channel = "goodgame.channel"
    }

    object twitch {
        const val enabled = "twitch.enabled"
        const val channel = "twitch.channel"
    }

    object youtube {
        const val enabled = "youtube.enabled"
        const val channel = "youtube.channel"
    }

    object cybergame {
        const val enabled = "cybergame.enabled"
        const val channel = "cybergame.channel"
    }


    object backgroundColor {
        const val native = "background-color.native"
        const val external = "background-color.external"
    }

    const val skin = "skin"
    const val frame = "frame"
    const val onTop = "on-top"
    const val showViewers = "show-viewers"
    const val showImages = "show-images"
    const val opacity = "opacity"
    const val statusMessageMode = "status-message-mode"
    const val showOriginBadges = "show-origin-badges"
    const val showUserBadges = "show-user-badges"
    const val zoomPercent = "zoom-percent"
    const val ignore = "ignore"
    const val hideMessages = "hide-messages"
    const val hideMessagesAfter = "hide-messages-after"


    val configurableByUserProperties: List<String> = listOf(
            peka2tv.enabled, peka2tv.channel,
            goodgame.enabled, goodgame.channel,
            twitch.enabled, twitch.channel,
            youtube.enabled, youtube.channel,
            cybergame.enabled, cybergame.channel,
            backgroundColor.native, backgroundColor.external,
            skin,
            frame,
            onTop,
            showViewers,
            showImages,
            opacity,
            statusMessageMode,
            showOriginBadges,
            showUserBadges,
            zoomPercent,
            ignore
    )

    fun lastUpdatedEmoticons(origin: Origin): String = "${origin.commonName}.emoticons.last-updated"

}