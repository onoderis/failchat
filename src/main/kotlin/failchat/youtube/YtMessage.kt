package failchat.youtube

import failchat.Origin.youtube
import failchat.chat.Author
import failchat.chat.ChatMessage

class YtMessage(
        id: Long,
        val ytId: String,
        author: Author,
        text: String

) : ChatMessage(id, youtube, author, text)
