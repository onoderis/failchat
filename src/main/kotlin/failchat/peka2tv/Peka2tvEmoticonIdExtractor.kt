package failchat.peka2tv

import failchat.Origin
import failchat.emoticon.EmoticonIdExtractor

object Peka2tvEmoticonIdExtractor : EmoticonIdExtractor<Peka2tvEmoticon> {

    override val origin = Origin.PEKA2TV

    override fun extractId(emoticon: Peka2tvEmoticon): String {
        return emoticon.peka2tvId.toString()
    }
}
