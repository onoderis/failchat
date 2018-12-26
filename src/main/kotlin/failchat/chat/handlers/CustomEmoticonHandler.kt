package failchat.chat.handlers

import failchat.Origin.FAILCHAT
import failchat.chat.ChatMessage
import failchat.chat.MessageHandler
import failchat.emoticon.EmoticonFinder
import failchat.emoticon.ReplaceDecision
import failchat.emoticon.SemicolonCodeProcessor
import mu.KLogging

class CustomEmoticonHandler(
        private val finder: EmoticonFinder
) : MessageHandler<ChatMessage> {

    private companion object : KLogging()

    override fun handleMessage(message: ChatMessage) {
        message.text = SemicolonCodeProcessor.process(message.text) { code ->
            val emoticon = finder.findByCode(FAILCHAT, code)
            if (emoticon != null) {
                val label = message.addElement(emoticon)
                ReplaceDecision.Replace(label)
            } else {
                ReplaceDecision.Skip
            }
        }
    }

}

