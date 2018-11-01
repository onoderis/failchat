package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonIdExtractor

object TwitchEmoticonIdExtractor: EmoticonIdExtractor<TwitchEmoticon> {
    override val origin = Origin.TWITCH

    override fun extractId(emoticon: TwitchEmoticon): String {
        return emoticon.twitchId.toString()
    }
}
