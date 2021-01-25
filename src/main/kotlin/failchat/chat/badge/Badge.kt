package failchat.chat.badge

import failchat.chat.ImageFormat
import javafx.scene.paint.Color

sealed class Badge {
    abstract val description: String?
}

data class ImageBadge(
        val url: String,
        val format: ImageFormat,
        override val description: String? = null
) : Badge()

data class CharacterBadge(
        /** Html character entity. */
        val characterEntity: String,
        /** Color in hexadecimal format. */
        val color: Color,
        override val description: String? = null
) : Badge()
