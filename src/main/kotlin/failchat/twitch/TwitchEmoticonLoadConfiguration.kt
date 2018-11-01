package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonBulkLoader
import failchat.emoticon.EmoticonLoadConfiguration
import failchat.emoticon.EmoticonLoadConfiguration.LoadType

class TwitchEmoticonLoadConfiguration(
        twitchLoader: TwitchEmoticonStreamLoader,
        twitchemotesLoader: TwitchemotesStreamLoader
) : EmoticonLoadConfiguration<TwitchEmoticon> {
    override val streamLoaders = listOf(twitchemotesLoader, twitchLoader) //twitchemotes is faster and more stable than official api
    override val origin = Origin.TWITCH
    override val loadType = LoadType.STREAM
    override val bulkLoaders: List<EmoticonBulkLoader<TwitchEmoticon>> = emptyList()
    override val idExtractor = TwitchEmoticonIdExtractor
}
