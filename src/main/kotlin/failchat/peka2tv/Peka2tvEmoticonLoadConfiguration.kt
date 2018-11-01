package failchat.peka2tv

import failchat.Origin
import failchat.emoticon.EmoticonLoadConfiguration
import failchat.emoticon.EmoticonLoadConfiguration.LoadType
import failchat.emoticon.EmoticonStreamLoader

class Peka2tvEmoticonLoadConfiguration(loader: Peka2tvEmoticonBulkLoader) : EmoticonLoadConfiguration<Peka2tvEmoticon> {
    override val origin = Origin.PEKA2TV
    override val loadType = LoadType.BULK
    override val bulkLoaders = listOf(loader)
    override val streamLoaders: List<EmoticonStreamLoader<Peka2tvEmoticon>> = emptyList()
    override val idExtractor = Peka2tvEmoticonIdExtractor
}
