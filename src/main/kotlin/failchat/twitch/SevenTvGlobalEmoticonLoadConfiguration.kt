package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonLoadConfiguration

class SevenTvGlobalEmoticonLoadConfiguration(
        override val loader: SevenTvGlobalEmoticonLoader
) : EmoticonLoadConfiguration<SevenTvEmoticon> {
    override val origin = Origin.SEVEN_TV_GLOBAL
    override val idExtractor = SevenTvEmoticonIdExtractor
}
