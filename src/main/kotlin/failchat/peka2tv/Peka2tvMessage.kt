package failchat.peka2tv

import failchat.Origin
import failchat.Origin.PEKA2TV
import failchat.chat.Author
import failchat.chat.ChatMessage

class Peka2tvMessage(
        id: Long,
        val peka2tvId: Long,
        val fromUser: Peka2tvUser, //for SourceFilter and HighlightHandler
        text: String,
        val type: String,
        val toUser: Peka2tvUser?, //for HighlightHandler
        val badgeId: Peka2tvBadgeId
) : ChatMessage(id, PEKA2TV, Author(fromUser.name, Origin.PEKA2TV), text)

