package failchat.goodgame

import failchat.Origin
import failchat.emoticon.EmoticonLoadConfiguration

class GgEmoticonLoadConfiguration(override val loader: GgEmoticonLoader) : EmoticonLoadConfiguration<GgEmoticon> {
    override val origin = Origin.GOODGAME
    override val idExtractor = GgEmoticonIdExtractor
}
