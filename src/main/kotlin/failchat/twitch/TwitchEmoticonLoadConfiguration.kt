package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonLoadConfiguration

class TwitchEmoticonLoadConfiguration(
        override val loader: TwitchGlobalEmoticonLoader
) : EmoticonLoadConfiguration<TwitchEmoticon> {
    override val origin = Origin.TWITCH
    override val idExtractor = TwitchEmoticonIdExtractor
}
