package failchat.emoticon

import failchat.twitch.TwitchEmoticon
import failchat.twitch.TwitchEmoticonUrlFactory

class TwitchEmoticonFactory(
        private val urlFactory: TwitchEmoticonUrlFactory
) : EmoticonFactory {
    override fun create(id: Long, code: String): Emoticon {
        return TwitchEmoticon(id, code, urlFactory)
    }
}
