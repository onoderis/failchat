package failchat.twitch

import failchat.Origin
import failchat.chat.Author
import failchat.chat.ChatMessage
import javafx.scene.paint.Color

class TwitchMessage(
        id: Long,
        author: String,
        text: String,
        /** Non-empty string with emotes. */
        val emotesTag: String?,
        /** Non-empty string with badges. */
        val badgesTag: String?,
        authorColor: Color?
) : ChatMessage(id, Origin.TWITCH, Author(author, Origin.TWITCH, color = authorColor), text)
