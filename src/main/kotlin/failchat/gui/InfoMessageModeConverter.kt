package failchat.gui

import failchat.core.chat.InfoMessageMode
import failchat.core.chat.InfoMessageMode.EVERYWHERE
import failchat.core.chat.InfoMessageMode.NATIVE_CLIENT
import failchat.core.chat.InfoMessageMode.NOWHERE
import javafx.util.StringConverter

class InfoMessageModeConverter : StringConverter<InfoMessageMode>() {

    // Too few values for bidirectional map, don't want to include guava only for this
    private val modToTitleList = listOf(
            EVERYWHERE to "Everywhere",
            NATIVE_CLIENT to "On native client",
            NOWHERE to "Nowhere"
    )

    override fun toString(mode: InfoMessageMode): String {
        return modToTitleList
                .find { it.first == mode }?.second
                ?: throw IllegalArgumentException("Unmapped InfoMessageMode: ${mode.name}")
    }

    override fun fromString(modTitle: String): InfoMessageMode {
        return modToTitleList
                .find { it.second == modTitle }?.first
                ?: throw IllegalArgumentException("Unmapped InfoMessageMode title: $modTitle")
    }

}
