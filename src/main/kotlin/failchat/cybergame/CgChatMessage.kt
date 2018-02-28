package failchat.cybergame

import failchat.Origin
import failchat.chat.Author
import failchat.chat.ChatMessage

class CgChatMessage(
        id: Long,
        author: Author,
        text: String,
        elements: List<Any>
) : ChatMessage(id, Origin.CYBERGAME, author, text, elements = elements)
