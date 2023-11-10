package failchat.peka2tv

import failchat.Origin
import failchat.emoticon.EmoticonLoadConfiguration

class Peka2tvEmoticonLoadConfiguration(override val loader: Peka2TvEmoticonLoader) : EmoticonLoadConfiguration<Peka2tvEmoticon> {
    override val origin = Origin.PEKA2TV
    override val idExtractor = Peka2tvEmoticonIdExtractor
}
