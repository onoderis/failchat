package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonLoadConfiguration
import failchat.emoticon.EmoticonLoadConfiguration.LoadType
import failchat.emoticon.EmoticonStreamLoader

class SevenTvGlobalEmoticonLoadConfiguration(
        loader: SevenTvGlobalEmoticonLoader
) : EmoticonLoadConfiguration<SevenTvEmoticon> {
    override val origin = Origin.SEVEN_TV_GLOBAL
    override val loadType = LoadType.BULK
    override val bulkLoaders = listOf(loader)
    override val streamLoaders: List<EmoticonStreamLoader<SevenTvEmoticon>> = emptyList()
    override val idExtractor = SevenTvEmoticonIdExtractor
}
