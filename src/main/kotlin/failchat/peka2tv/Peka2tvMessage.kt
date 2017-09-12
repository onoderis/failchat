package failchat.peka2tv

import failchat.Origin.peka2tv
import failchat.chat.Author
import failchat.chat.ChatMessage

class Peka2tvMessage(
        id: Long,
        val peka2tvId: Long,
        val fromUser: Peka2tvUser, //for SourceFilter and HighlightHandler
        text: String,
        val type: String,
        val toUser: Peka2tvUser? //for HighlightHandler
) : ChatMessage(id, peka2tv, Author(fromUser.name), text)

