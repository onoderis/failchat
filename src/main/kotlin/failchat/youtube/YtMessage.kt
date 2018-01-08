package failchat.youtube

import failchat.Origin.YOUTUBE
import failchat.chat.Author
import failchat.chat.ChatMessage

class YtMessage(
        id: Long,
        val ytId: String,
        author: Author,
        text: String

) : ChatMessage(id, YOUTUBE, author, text)
