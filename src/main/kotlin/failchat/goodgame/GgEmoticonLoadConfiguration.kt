package failchat.goodgame

import failchat.Origin
import failchat.emoticon.EmoticonLoadConfiguration
import failchat.emoticon.EmoticonLoadConfiguration.LoadType
import failchat.emoticon.EmoticonStreamLoader

class GgEmoticonLoadConfiguration(loader: GgEmoticonBulkLoader) : EmoticonLoadConfiguration<GgEmoticon> {
    override val origin = Origin.GOODGAME
    override val loadType = LoadType.BULK
    override val bulkLoaders = listOf(loader)
    override val streamLoaders: List<EmoticonStreamLoader<GgEmoticon>> = emptyList()
    override val idExtractor = GgEmoticonIdExtractor
}
