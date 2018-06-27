package failchat.gui

import failchat.skin.Skin
import failchat.util.warn
import javafx.util.StringConverter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SkinConverter(skins: List<Skin>) : StringConverter<Skin>() {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(SkinConverter::class.java)
    }

    private val skinMap: Map<String, Skin> = skins.map { it.name to it }.toMap()
    private val defaultSkin: Skin = skins.first()

    override fun toString(skin: Skin): String {
        return skin.name
    }

    override fun fromString(skinName: String): Skin {
        return skinMap.get(skinName)
                ?: run {
                    log.warn { "Unknown skin '$skinName', default skin '${defaultSkin.name}' will be used" }
                    defaultSkin
                }
    }

}
