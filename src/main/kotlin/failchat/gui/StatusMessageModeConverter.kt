package failchat.gui

import failchat.chat.StatusMessageMode
import failchat.chat.StatusMessageMode.EVERYWHERE
import failchat.chat.StatusMessageMode.NATIVE_CLIENT
import failchat.chat.StatusMessageMode.NOWHERE
import javafx.util.StringConverter

class StatusMessageModeConverter : StringConverter<StatusMessageMode>() {

    // Too few values for bidirectional map, don't want to include guava only for this
    private val modeToTitleList = listOf(
            EVERYWHERE to "Everywhere",
            NATIVE_CLIENT to "On native client",
            NOWHERE to "Nowhere"
    )

    override fun toString(mode: StatusMessageMode): String {
        return modeToTitleList
                .find { it.first == mode }?.second
                ?: throw IllegalArgumentException("Unmapped StatusMessageMode: ${mode.name}")
    }

    override fun fromString(modTitle: String): StatusMessageMode {
        return modeToTitleList
                .find { it.second == modTitle }?.first
                ?: throw IllegalArgumentException("Unmapped StatusMessageMode title: $modTitle")
    }

}
