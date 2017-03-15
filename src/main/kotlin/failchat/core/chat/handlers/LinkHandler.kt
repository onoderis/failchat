package failchat.core.chat.handlers

import failchat.core.chat.ChatMessage
import failchat.core.chat.Link
import failchat.core.chat.MessageHandler
import failchat.utils.urlPattern

class LinkHandler : MessageHandler<ChatMessage> {

    override fun handleMessage(message: ChatMessage) {
        val matcher = urlPattern.matcher(message.text)
        while (matcher.find()) {
            val url = Link(matcher.group(), matcher.group(4), matcher.group(3))
            val elementNumber = message.addElement(url)

            message.text = matcher.replaceFirst(elementNumber)
            matcher.reset(message.text)
        }
    }

}
