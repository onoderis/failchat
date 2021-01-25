package failchat.twitch

import failchat.Origin
import failchat.chat.ImageFormat.RASTER
import failchat.emoticon.Emoticon

class TwitchEmoticon(
        val twitchId: Long,
        code: String,
        private val urlFactory: TwitchEmoticonUrlFactory
) : Emoticon(Origin.TWITCH, code, RASTER) {

    override val url: String
        get() = urlFactory.create(twitchId)
}
