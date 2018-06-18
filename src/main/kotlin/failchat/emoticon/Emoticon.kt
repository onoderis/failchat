package failchat.emoticon

import failchat.Origin
import failchat.chat.ImageFormat
import failchat.chat.ImageFormat.RASTER
import failchat.chat.MessageElement
import java.io.Serializable

open class Emoticon(
        val origin: Origin,
        val code: String,
        val url: String,
        val format: ImageFormat = RASTER
) : MessageElement, Serializable
