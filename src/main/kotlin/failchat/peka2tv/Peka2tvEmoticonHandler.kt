package failchat.peka2tv

import failchat.Origin
import failchat.chat.MessageHandler
import failchat.emoticon.Emoticon
import failchat.emoticon.EmoticonFinder
import java.util.regex.Pattern

class Peka2tvEmoticonHandler(private val emoticonFinder: EmoticonFinder) : MessageHandler<Peka2tvMessage> {

    private companion object {
        val emoticonCodePattern: Pattern = Pattern.compile("""((?<=:)([\w-]+)(?=:))""")
    }

    override fun handleMessage(message: Peka2tvMessage) {
        //todo refactor
        var matcher = emoticonCodePattern.matcher(message.text)
        var position = 0 // чтобы не начинать искать сначала, если :something: найдено, но это не смайл
        while (matcher.find(position)) {
            val code = matcher.group().toLowerCase() //ignore case

            val emoticon = findByMultiOriginCode(code)
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

    private fun findByMultiOriginCode(code: String): Emoticon? {
        return when {
            code.startsWith("tw-") -> emoticonFinder.findByCode(Origin.twitch, code.substring(3))
            code.startsWith("gg-") -> emoticonFinder.findByCode(Origin.goodgame, code.substring(3))
            else -> emoticonFinder.findByCode(Origin.peka2tv, code)
        }
    }

}
