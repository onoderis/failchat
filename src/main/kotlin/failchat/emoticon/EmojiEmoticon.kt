package failchat.emoticon

import failchat.Origin
import failchat.chat.ImageFormat

class EmojiEmoticon(
        code: String,
        override val url: String
) : Emoticon(Origin.FAILCHAT, code, ImageFormat.VECTOR)
