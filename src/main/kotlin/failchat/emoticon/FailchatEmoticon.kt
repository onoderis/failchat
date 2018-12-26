package failchat.emoticon

import failchat.Origin.FAILCHAT
import failchat.chat.ImageFormat

class FailchatEmoticon(
        code: String,
        format: ImageFormat,
        override val url: String
) : Emoticon(FAILCHAT, code, format)
