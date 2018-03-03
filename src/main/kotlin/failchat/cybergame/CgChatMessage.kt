package failchat.cybergame

import failchat.Origin
import failchat.chat.Author
import failchat.chat.ChatMessage
import failchat.chat.MessageElement

class CgChatMessage(
        id: Long,
        author: Author,
        text: String,
        elements: List<MessageElement>
) : ChatMessage(id, Origin.CYBERGAME, author, text, elements = elements)
