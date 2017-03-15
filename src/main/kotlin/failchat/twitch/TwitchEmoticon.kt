package failchat.twitch

import failchat.core.Origin
import failchat.core.emoticon.Emoticon

class TwitchEmoticon(val id: Long, regex: String, url: String) : Emoticon(Origin.twitch, regex, url)
