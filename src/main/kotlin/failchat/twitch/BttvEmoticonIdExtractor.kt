package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonIdExtractor

object BttvEmoticonIdExtractor : EmoticonIdExtractor<BttvEmoticon> {

    override val origin = Origin.BTTV_GLOBAL

    override fun extractId(emoticon: BttvEmoticon): String {
        return emoticon.code
    }
}
