package failchat.chat.handlers

import failchat.chat.ChatMessage
import failchat.chat.Link
import failchat.chat.MessageHandler
import failchat.util.urlPattern

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
