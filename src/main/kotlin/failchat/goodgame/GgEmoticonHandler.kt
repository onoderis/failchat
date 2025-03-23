package failchat.goodgame

import failchat.Origin.GOODGAME
import failchat.chat.MessageHandler
import failchat.emoticon.EmoticonFinder
import failchat.emoticon.ReplaceDecision
import failchat.emoticon.SemicolonCodeProcessor

class GgEmoticonHandler(private val emoticonFinder: EmoticonFinder) : MessageHandler<GgMessage> {
    override fun handleMessage(message: GgMessage) {
        message.text =
            SemicolonCodeProcessor.process(message.text) { code ->
                val emoticon =
                    emoticonFinder.findByCode(GOODGAME, code) as? GgEmoticon
                        ?: return@process ReplaceDecision.Skip

                // prefer animated emoticons even for non-premium users
                val emoticonToAdd = emoticon.animatedInstance ?: emoticon

                val label = message.addElement(emoticonToAdd)
                return@process ReplaceDecision.Replace(label)
            }
    }
}
