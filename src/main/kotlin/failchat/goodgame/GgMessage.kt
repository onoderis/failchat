package failchat.goodgame

import failchat.Origin
import failchat.chat.Author
import failchat.chat.ChatMessage

class GgMessage(
        id: Long,
        val ggId: Long,
        author: String,
        text: String,
        val authorHasPremium: Boolean
) : ChatMessage(id, Origin.goodgame, Author(author), text)
