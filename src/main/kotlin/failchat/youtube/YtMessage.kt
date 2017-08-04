package failchat.youtube

import failchat.core.Origin.youtube
import failchat.core.chat.ChatMessage

class YtMessage(
        id: Long,
        val ytId: String,
        author: String,
        text: String

) : ChatMessage(id, youtube, author, text)
