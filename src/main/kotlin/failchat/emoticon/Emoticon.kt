package failchat.emoticon

import failchat.Origin
import failchat.chat.MessageElement
import failchat.emoticon.EmoticonFormat.RASTER
import java.io.Serializable

open class Emoticon(
        val origin: Origin,
        val code: String,
        val url: String,
        val format: EmoticonFormat = RASTER
) : MessageElement, Serializable
