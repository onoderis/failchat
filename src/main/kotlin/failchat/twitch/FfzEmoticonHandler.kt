package failchat.twitch

import failchat.Origin
import failchat.chat.MessageHandler
import failchat.emoticon.EmoticonFinder
import failchat.emoticon.ReplaceDecision
import failchat.emoticon.WordReplacer

class FfzEmoticonHandler(private val emoticonFinder: EmoticonFinder) : MessageHandler<TwitchMessage> {

    override fun handleMessage(message: TwitchMessage) {
        message.text = WordReplacer.replace(message.text) { code ->
            val emoticon = emoticonFinder.findByCode(Origin.FRANKERFASEZ, code)
                    ?: return@replace ReplaceDecision.Skip
            val label = message.addElement(emoticon)
            return@replace ReplaceDecision.Replace(label)
        }
    }

}
