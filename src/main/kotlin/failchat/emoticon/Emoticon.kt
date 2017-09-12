package failchat.emoticon

import failchat.Origin
import failchat.emoticon.EmoticonFormat.raster
import java.io.Serializable

open class Emoticon(
        val origin: Origin,
        val code: String,
        val url: String,
        val format: EmoticonFormat = raster
) : Serializable
