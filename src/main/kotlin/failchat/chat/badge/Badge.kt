package failchat.chat.badge

import failchat.chat.ImageFormat

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
        val color: String,
        description: String? = null
) : Badge(description)
