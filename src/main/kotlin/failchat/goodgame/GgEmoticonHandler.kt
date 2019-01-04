package failchat.goodgame

import failchat.Origin.GOODGAME
import failchat.chat.MessageHandler
import failchat.emoticon.EmoticonFinder
import failchat.emoticon.ReplaceDecision
import failchat.emoticon.SemicolonCodeProcessor

class GgEmoticonHandler(private val emoticonFinder: EmoticonFinder) : MessageHandler<GgMessage> {

    override fun handleMessage(message: GgMessage) {
        message.text = SemicolonCodeProcessor.process(message.text) { code ->
            val emoticon = emoticonFinder.findByCode(GOODGAME, code) as? GgEmoticon
                    ?: return@process ReplaceDecision.Skip

            val emoticonToAdd = if (message.authorHasPremium && emoticon.animatedInstance != null) {
                emoticon.animatedInstance!!
            } else {
                emoticon
            }

            val label = message.addElement(emoticonToAdd)
            return@process ReplaceDecision.Replace(label)
        }
    }

}
