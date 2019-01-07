package failchat.chat.badge

import failchat.chat.ImageFormat
import javafx.scene.paint.Color

sealed class Badge(
        val description: String?
)

class ImageBadge(
        val url: String,
        val format: ImageFormat,
        description: String? = null
) : Badge(description)

class CharacterBadge(
        /** Html character entity. */
        val characterEntity: String,
        /** Color in hexadecimal format. */
        val color: Color,
        description: String? = null
) : Badge(description)
