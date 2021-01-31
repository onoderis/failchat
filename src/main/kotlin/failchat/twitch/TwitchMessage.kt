package failchat.twitch

import failchat.Origin
import failchat.chat.Author
import failchat.chat.ChatMessage

class TwitchMessage(
        id: Long,
        author: String,
        text: String,
        val tags: Map<String, String>
) : ChatMessage(id, Origin.TWITCH, Author(author, Origin.TWITCH), text)
