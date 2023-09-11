package failchat.gui

import failchat.skin.Skin
import javafx.util.StringConverter
import mu.KotlinLogging

class SkinConverter(skins: List<Skin>) : StringConverter<Skin>() {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    private val skinMap: Map<String, Skin> = skins.map { it.name to it }.toMap()
    private val defaultSkin: Skin = skins.first()

    override fun toString(skin: Skin): String {
        return skin.name
    }

    override fun fromString(skinName: String): Skin {
        return skinMap.get(skinName)
                ?: run {
                    logger.warn { "Unknown skin '$skinName', default skin '${defaultSkin.name}' will be used" }
                    defaultSkin
                }
    }

}
