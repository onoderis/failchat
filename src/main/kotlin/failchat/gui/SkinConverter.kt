package failchat.gui

import failchat.core.skin.Skin
import javafx.util.StringConverter

class SkinConverter(skins: List<Skin>) : StringConverter<Skin>() {

    private val skinMap: Map<String, Skin> = skins.map { it.name to it }.toMap()

    override fun toString(skin: Skin): String {
        if (skinMap.containsKey(skin.name)) return skin.name
        else throw RuntimeException("Unknown skin. name=${skin.name}; htmlPath=${skin.htmlPath}") //todo change exception
    }

    override fun fromString(skinName: String): Skin {
        return skinMap.get(skinName)
                ?: throw RuntimeException("Unknown skin: $skinName") //todo change exception
    }

}