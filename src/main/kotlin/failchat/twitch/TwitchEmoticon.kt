package failchat.twitch

import failchat.Origin
import failchat.chat.ImageFormat.RASTER
import failchat.emoticon.Emoticon

class TwitchEmoticon(
        val twitchId: String,
        code: String
) : Emoticon(Origin.TWITCH, code, RASTER) {

    override val url: String
        get() = TwitchEmoticonUrlFactory.create(twitchId)
}
