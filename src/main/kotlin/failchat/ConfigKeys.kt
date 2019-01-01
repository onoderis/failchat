package failchat

@Suppress("ClassName")
object ConfigKeys {

    //todo use properties from here

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

    object nativeClient {
        private const val prefix = "native-client"
        const val backgroundColor = "$prefix.background-color"
        const val hideMessages = "$prefix.hide-messages"
        const val hideMessagesAfter = "$prefix.hide-messages-after"
        const val showStatusMessages = "$prefix.show-status-messages"
    }

    object externalClient {
        private const val prefix = "external-client"
        const val backgroundColor = "$prefix.background-color"
        const val hideMessages = "$prefix.hide-messages"
        const val hideMessagesAfter = "$prefix.hide-messages-after"
        const val showStatusMessages = "$prefix.show-status-messages"
    }

    const val skin = "skin"
    const val frame = "frame"
    const val onTop = "on-top"
    const val showViewers = "show-viewers"
    const val showImages = "show-images"
    const val opacity = "opacity"
    const val showOriginBadges = "show-origin-badges"
    const val showUserBadges = "show-user-badges"
    const val zoomPercent = "zoom-percent"
    const val deletedMessagePlaceholder = "deleted-message-placeholder"
    const val ignore = "ignore"
    const val showHiddenMessages = "show-hidden-messages"

    const val resetConfiguration = "reset-configuration"

    const val frankerfacezApiUrl = "frankerfacez.api-url"

    fun lastUpdatedEmoticons(origin: Origin): String = "${origin.commonName}.emoticons.last-updated"

}
