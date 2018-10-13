package failchat.twitch

import failchat.Origin
import failchat.chat.Author
import failchat.chat.ChatMessage

class TwitchMessage(
        id: Long,
        author: String,
        text: String,
        /** Non-empty string with emotes. */
        val emotesTag: String?,
        /** Non-empty string with badges. */
        val badgesTag: String?
) : ChatMessage(id, Origin.TWITCH, Author(author, Origin.TWITCH), text)
