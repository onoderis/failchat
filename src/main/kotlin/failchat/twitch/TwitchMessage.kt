package failchat.twitch

import failchat.core.Origin
import failchat.core.chat.ChatMessage

class TwitchMessage(
        id: Long,
        author: String,
        text: String,
        val emotesTag: String?
) : ChatMessage(id, Origin.twitch, author, text)
