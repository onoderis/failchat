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
        val subscriptionDuration: Map<Long, Int>,
        val badgeName: String,
        val authorColorName: String,
        val sponsorLevel: Int,
        val authorRights: Int
) : ChatMessage(id, Origin.GOODGAME, Author(author, Origin.GOODGAME), text)
