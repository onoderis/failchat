package failchat.goodgame

import failchat.Origin
import failchat.chat.ImageFormat.RASTER
import failchat.emoticon.Emoticon

class GgEmoticon(
        code: String,
        override val url: String,
        val ggId: Long
) : Emoticon(Origin.GOODGAME, code, RASTER) {

    var animatedInstance: GgEmoticon? = null

}
