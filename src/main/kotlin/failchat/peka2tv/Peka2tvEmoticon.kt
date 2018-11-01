package failchat.peka2tv

import failchat.Origin.PEKA2TV
import failchat.chat.ImageFormat.RASTER
import failchat.emoticon.Emoticon

class Peka2tvEmoticon(
        code: String,
        override val url: String,
        val peka2tvId: Long
) : Emoticon(PEKA2TV, code, RASTER)
