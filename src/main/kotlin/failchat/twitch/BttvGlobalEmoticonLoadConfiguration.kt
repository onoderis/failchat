package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonLoadConfiguration

class BttvGlobalEmoticonLoadConfiguration(
        override val loader: BttvGlobalEmoticonLoader
) : EmoticonLoadConfiguration<BttvEmoticon> {
    override val origin = Origin.BTTV_GLOBAL
    override val idExtractor = BttvEmoticonIdExtractor
}
