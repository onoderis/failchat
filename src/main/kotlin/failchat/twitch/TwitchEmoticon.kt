package failchat.twitch

import failchat.Origin
import failchat.emoticon.Emoticon

class TwitchEmoticon(val twitchId: Long, regex: String, url: String) : Emoticon(Origin.TWITCH, regex, url)
