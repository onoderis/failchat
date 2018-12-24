package failchat.twitch

import failchat.Origin
import failchat.chat.ImageFormat
import failchat.emoticon.Emoticon

class FfzEmoticon(
        code: String,
        override val url: String
) : Emoticon(Origin.FRANKERFASEZ, code, ImageFormat.RASTER)
