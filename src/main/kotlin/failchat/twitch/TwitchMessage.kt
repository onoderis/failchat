package failchat.twitch

import failchat.Origin
import failchat.chat.Author
import failchat.chat.ChatMessage

class TwitchMessage(
        id: Long,
        author: String,
        text: String,
        val emotesTag: String?,
        val badgesTag: String?
) : ChatMessage(id, Origin.TWITCH, Author(author), text)
