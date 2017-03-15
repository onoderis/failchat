package failchat.peka2tv

import failchat.core.Origin.peka2tv
import failchat.core.chat.MessageHandler
import failchat.core.emoticon.EmoticonManager
import java.util.regex.Pattern

class Peka2tvEmoticonHandler(private val emoticonManager: EmoticonManager) : MessageHandler<Peka2tvMessage> {

    private companion object {
        val emoticonCodePattern: Pattern = Pattern.compile("""((?<=:)(\w+)(?=:))""")
    }

    override fun handleMessage(message: Peka2tvMessage) {
        //todo refactor
        var matcher = emoticonCodePattern.matcher(message.text)
        var position = 0 // чтобы не начинать искать сначала, если :something: найдено, но это не смайл
        while (matcher.find(position)) {
            val code = matcher.group().toLowerCase() //ignore case


            val emoticon = emoticonManager.find(peka2tv, code)
            if (emoticon != null) {
                val num = message.addElement(emoticon)

                //replace emoticon text for object
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
