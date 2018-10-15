package failchat.chat.handlers

import failchat.chat.ChatMessage
import failchat.chat.MessageHandler
import failchat.chat.handlers.SemicolonCodeProcessor.Decision
import failchat.emoticon.CustomEmoticonScanner
import failchat.emoticon.Emoticon
import mu.KLogging
import kotlin.system.measureTimeMillis

class CustomEmoticonHandler(
        private val scanner: CustomEmoticonScanner
) : MessageHandler<ChatMessage> {

    private companion object : KLogging()

    @Volatile
    private var emoticons: Map<String, Emoticon> = emptyMap()

    override fun handleMessage(message: ChatMessage) {
        val localEmoticons = emoticons

        message.text = SemicolonCodeProcessor.process(message.text) { code ->
            val emoticon = localEmoticons[code.toLowerCase()]
            if (emoticon != null) {
                val label = message.addElement(emoticon)
                Decision.Replace(label)
            } else {
                Decision.Skip
            }
        }
    }

    fun scanEmoticonsDirectory() {
        val time = measureTimeMillis {
            emoticons = scanner.scan()
        }
        logger.debug { "Custom emoticons scanned in $time ms" }
    }
}

