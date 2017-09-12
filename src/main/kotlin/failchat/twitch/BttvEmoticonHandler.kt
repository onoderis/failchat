package failchat.twitch

import failchat.Origin
import failchat.chat.MessageHandler
import failchat.emoticon.Emoticon
import failchat.emoticon.EmoticonFinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

class BttvEmoticonHandler(private val emoticonFinder: EmoticonFinder) : MessageHandler<TwitchMessage> {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(BttvEmoticonHandler::class.java)
    }

    @Volatile private var globalEmoticonsPattern: Pattern? = null
    @Volatile private var channelEmoticonsPattern: Pattern? = null

    override fun handleMessage(message: TwitchMessage) {
        //todo optimize
        if (globalEmoticonsPattern == null && emoticonFinder.getList(Origin.bttvGlobal).isNotEmpty()) {
            globalEmoticonsPattern = compileEmoticonPattern(emoticonFinder.getList(Origin.bttvGlobal))
        }

        if (channelEmoticonsPattern == null && emoticonFinder.getList(Origin.bttvChannel).isNotEmpty()) {
            channelEmoticonsPattern = compileEmoticonPattern(emoticonFinder.getList(Origin.bttvChannel))
        }

        handleEmoticons(message, globalEmoticonsPattern, Origin.bttvGlobal)
        handleEmoticons(message, channelEmoticonsPattern, Origin.bttvChannel)
    }

    fun resetChannelPattern() {
        channelEmoticonsPattern = null
    }

    private fun handleEmoticons(message: TwitchMessage, pattern: Pattern?, origin: Origin) {
        val matcher = pattern?.matcher(message.text) ?: return
        while (matcher.find()) {
            val code = matcher.group(0)
            val emoticon = emoticonFinder.findByCode(origin, code.toLowerCase())
            if (emoticon == null) {
                log.warn("Emoticon code '{}' found in message, but emoticon is not. Message: '{}', emoticon origin: '{}'",
                        code, message.text, origin.name)
                return
            }

            val elementLabel = message.addElement(emoticon)
            message.text = matcher.replaceFirst(elementLabel)

            matcher.reset(message.text)
        }
    }

    private fun compileEmoticonPattern(emoticons: List<Emoticon>): Pattern {
        return if (emoticons.isEmpty()) throw IllegalArgumentException("List is empty")
        else emoticons
                .map { it.code.replace("""\""", """\\""") }
                .joinToString(separator = "\\E|\\Q", prefix = "\\Q", postfix = "\\E")
                .let { Pattern.compile(it) }
    }

}
