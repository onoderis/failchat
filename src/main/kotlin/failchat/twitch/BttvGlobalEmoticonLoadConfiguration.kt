package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonLoadConfiguration
import failchat.emoticon.EmoticonLoadConfiguration.LoadType
import failchat.emoticon.EmoticonStreamLoader

class BttvGlobalEmoticonLoadConfiguration(
        loader: BttvGlobalEmoticonBulkLoader
) : EmoticonLoadConfiguration<BttvEmoticon> {
    override val origin = Origin.BTTV_GLOBAL
    override val loadType = LoadType.BULK
    override val bulkLoaders = listOf(loader)
    override val streamLoaders: List<EmoticonStreamLoader<BttvEmoticon>> = emptyList()
    override val idExtractor = BttvEmoticonIdExtractor
}
