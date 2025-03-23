package failchat.emoticon

import failchat.twitch.TwitchEmoticon

class TwitchEmoticonFactory : EmoticonFactory {
    override fun create(id: String, code: String): Emoticon = TwitchEmoticon(id, code)
}
