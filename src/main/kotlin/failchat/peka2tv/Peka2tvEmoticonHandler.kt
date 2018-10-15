package failchat.peka2tv

import failchat.Origin
import failchat.chat.MessageHandler
import failchat.chat.handlers.SemicolonCodeProcessor
import failchat.chat.handlers.SemicolonCodeProcessor.Decision
import failchat.emoticon.Emoticon
import failchat.emoticon.EmoticonFinder
import failchat.goodgame.GgEmoticon

class Peka2tvEmoticonHandler(private val emoticonFinder: EmoticonFinder) : MessageHandler<Peka2tvMessage> {

    override fun handleMessage(message: Peka2tvMessage) {
        message.text = SemicolonCodeProcessor.process(message.text) { code ->
            val emoticon = findByMultiOriginCode(code.toLowerCase()) //ignore case
            if (emoticon != null) {
                val elementLabel = message.addElement(emoticon)
                Decision.Replace(elementLabel)
            } else {
                Decision.Skip
            }
        }
    }

    private fun findByMultiOriginCode(code: String): Emoticon? {
        return when {
            code.startsWith("tw-") -> emoticonFinder.findByCode(Origin.TWITCH, code.substring(3))
            code.startsWith("gg-") -> {
                val emoticon = emoticonFinder.findByCode(Origin.GOODGAME, code.substring(3))
                        ?.let { it as GgEmoticon }
                val animatedEmoticon = emoticon?.animatedInstance
                animatedEmoticon ?: emoticon
            }
            else -> emoticonFinder.findByCode(Origin.PEKA2TV, code)
        }
    }

}
