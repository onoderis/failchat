package failchat.youtube

import failchat.Origin.YOUTUBE
import failchat.chat.ImageFormat
import failchat.emoticon.Emoticon

class YtEmoticon(
        code: String,
        override val url: String,
        format: ImageFormat
) : Emoticon(YOUTUBE, code, format)
