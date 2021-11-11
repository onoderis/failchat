package failchat.chat.handlers

import failchat.Origin
import failchat.chat.ChatMessage
import failchat.chat.MessageHandler
import failchat.emoticon.EmoticonFinder
import failchat.emoticon.ReplaceDecision
import failchat.emoticon.WordReplacer

class SpaceSeparatedEmoticonHandler(
        private val origin: Origin,
        private val emoticonFinder: EmoticonFinder
) : MessageHandler<ChatMessage> {

    override fun handleMessage(message: ChatMessage) {
        message.text = WordReplacer.replace(message.text) { code ->
            val emoticon = emoticonFinder.findByCode(origin, code)
                    ?: return@replace ReplaceDecision.Skip
            val label = message.addElement(emoticon)
            return@replace ReplaceDecision.Replace(label)
        }
    }

}
