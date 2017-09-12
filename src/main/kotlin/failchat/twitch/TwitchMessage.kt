package failchat.twitch

import failchat.Origin
import failchat.chat.Author
import failchat.chat.ChatMessage

class TwitchMessage(
        id: Long,
        author: String,
        text: String,
        val emotesTag: String?
) : ChatMessage(id, Origin.twitch, Author(author), text)
