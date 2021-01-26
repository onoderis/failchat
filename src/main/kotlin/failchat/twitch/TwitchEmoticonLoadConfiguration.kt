package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonLoadConfiguration
import failchat.emoticon.EmoticonLoadConfiguration.LoadType
import failchat.emoticon.EmoticonStreamLoader

class TwitchEmoticonLoadConfiguration(
        twitchLoader: TwitchEmoticonLoader
) : EmoticonLoadConfiguration<TwitchEmoticon> {
    override val streamLoaders = emptyList<EmoticonStreamLoader<TwitchEmoticon>>()
    override val origin = Origin.TWITCH
    override val loadType = LoadType.BULK
    override val bulkLoaders = listOf(twitchLoader)
    override val idExtractor = TwitchEmoticonIdExtractor
}
