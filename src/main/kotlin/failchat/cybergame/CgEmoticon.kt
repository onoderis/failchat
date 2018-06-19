package failchat.cybergame

import failchat.Origin.CYBERGAME
import failchat.chat.ImageFormat
import failchat.emoticon.Emoticon

class CgEmoticon(
        code: String,
        override val url: String,
        format: ImageFormat
) : Emoticon(CYBERGAME, code, format)
