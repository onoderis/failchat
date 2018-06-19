package failchat.youtube

import failchat.Origin.YOUTUBE
import failchat.chat.ImageFormat.VECTOR
import failchat.emoticon.Emoticon

class YtEmoticon(
        code: String,
        override val url: String
) : Emoticon(YOUTUBE, code, VECTOR)
