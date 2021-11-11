package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonIdExtractor

object SevenTvEmoticonIdExtractor : EmoticonIdExtractor<SevenTvEmoticon> {

    override val origin = Origin.SEVEN_TV_GLOBAL

    override fun extractId(emoticon: SevenTvEmoticon): String {
        return emoticon.id
    }
}
