package failchat.twitch

import failchat.Origin
import failchat.chat.ImageFormat
import failchat.emoticon.Emoticon

class SevenTvEmoticon(
        origin: Origin,
        code: String,
        val id: String,
        override val url: String
) : Emoticon(origin, code, ImageFormat.RASTER)
