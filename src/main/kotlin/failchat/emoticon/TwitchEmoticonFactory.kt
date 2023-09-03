package failchat.emoticon

import failchat.twitch.TwitchEmoticon

class TwitchEmoticonFactory : EmoticonFactory {
    override fun create(id: String, code: String): Emoticon {
        return TwitchEmoticon(id, code)
    }
}
