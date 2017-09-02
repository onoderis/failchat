package failchat.core.emoticon

import failchat.core.Origin
import java.io.Serializable

open class Emoticon(
        val origin: Origin,
        val code: String,
        val url: String,
        val format: EmoticonFormat = EmoticonFormat.raster
) : Serializable
