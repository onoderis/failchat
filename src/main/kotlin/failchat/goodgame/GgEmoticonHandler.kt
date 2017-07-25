package failchat.goodgame

import failchat.core.Origin
import failchat.core.chat.MessageHandler
import failchat.core.emoticon.EmoticonManager
import java.util.regex.Pattern

class GgEmoticonHandler(private val emoticonManager: EmoticonManager) : MessageHandler<GgMessage> {

    private companion object {
        val emoticonCodePattern: Pattern = Pattern.compile("""((?<=:)(\w+)(?=:))""")
    }

    override fun handleMessage(message: GgMessage) {
        //todo refactor
        var matcher = emoticonCodePattern.matcher(message.text)
        var position = 0 // чтобы не начинать искать сначала, если :something: найдено, но это не смайл
        while (matcher.find(position)) {
            val code = matcher.group().toLowerCase()
            var smile = emoticonManager.find(Origin.goodgame, code) as? GgEmoticon
            if (smile != null) {
                if (message.authorHasPremium && smile.animatedInstance != null) {
                    smile = smile.animatedInstance!!
                }
                val num = message.addElement(smile)

                //replace smile text for object
                val start = matcher.start()
                val end = matcher.end()
                val sb = StringBuilder(message.text)
                sb.delete(start - 1, end + 1) // for ':'
                sb.insert(start - 1, num)
                message.text = sb.toString()
                matcher = emoticonCodePattern.matcher(message.text)
                position = start
            } else {
                position = matcher.end()
            }
        }
    }

}
