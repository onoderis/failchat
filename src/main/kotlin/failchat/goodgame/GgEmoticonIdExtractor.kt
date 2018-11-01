package failchat.goodgame

import failchat.Origin
import failchat.emoticon.EmoticonIdExtractor

object GgEmoticonIdExtractor : EmoticonIdExtractor<GgEmoticon> {

    override val origin = Origin.GOODGAME

    override fun extractId(emoticon: GgEmoticon): String {
        return emoticon.ggId.toString()
    }
}
