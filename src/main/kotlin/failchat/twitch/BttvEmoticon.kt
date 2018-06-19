package failchat.twitch

import failchat.Origin
import failchat.chat.ImageFormat.RASTER
import failchat.emoticon.Emoticon

class BttvEmoticon(
        origin: Origin,
        code: String,
        override val url: String
) : Emoticon(origin, code, RASTER)
