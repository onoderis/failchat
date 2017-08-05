package failchat.goodgame

import failchat.core.Origin
import failchat.core.chat.Author
import failchat.core.chat.ChatMessage

class GgMessage(
        id: Long,
        val ggId: Long,
        author: String,
        text: String,
        val authorHasPremium: Boolean
) : ChatMessage(id, Origin.goodgame, Author(author), text)
