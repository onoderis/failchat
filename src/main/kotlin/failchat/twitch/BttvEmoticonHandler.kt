package failchat.twitch

import failchat.Origin
import failchat.chat.MessageHandler
import failchat.emoticon.EmoticonFinder
import failchat.emoticon.ReplaceDecision
import failchat.emoticon.WordReplacer

class BttvEmoticonHandler(private val emoticonFinder: EmoticonFinder) :
    MessageHandler<TwitchMessage> {
    override fun handleMessage(message: TwitchMessage) {
        handleEmoticons(message, Origin.BTTV_GLOBAL)
        handleEmoticons(message, Origin.BTTV_CHANNEL)
    }

    private fun handleEmoticons(message: TwitchMessage, origin: Origin) {
        message.text =
            WordReplacer.replace(message.text) { code ->
                val emoticon =
                    emoticonFinder.findByCode(origin, code) ?: return@replace ReplaceDecision.Skip
                val label = message.addElement(emoticon)
                return@replace ReplaceDecision.Replace(label)
            }
    }
}
