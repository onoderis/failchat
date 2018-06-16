package failchat.goodgame

import failchat.Origin
import failchat.chat.Author
import failchat.chat.ChatMessage

class GgMessage(
        id: Long,
        val ggId: Long,
        author: String,
        text: String,
        val authorHasPremium: Boolean,
        /** Mapping of channel id to subscription duration. */
        val subscriptionDuration: Map<Long, Int>
) : ChatMessage(id, Origin.GOODGAME, Author(author), text)
