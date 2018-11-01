package failchat.twitch

import failchat.Origin
import failchat.chat.MessageHandler
import failchat.emoticon.Emoticon
import failchat.emoticon.EmoticonFinder
import mu.KLogging
import java.util.regex.Pattern

class BttvEmoticonHandler(private val emoticonFinder: EmoticonFinder) : MessageHandler<TwitchMessage> {

    private companion object : KLogging()

    @Volatile private var globalEmoticonsPattern: Pattern? = null
    @Volatile private var channelEmoticonsPattern: Pattern? = null

    override fun handleMessage(message: TwitchMessage) {
        handleEmoticons(message, globalEmoticonsPattern, Origin.BTTV_GLOBAL)
        handleEmoticons(message, channelEmoticonsPattern, Origin.BTTV_CHANNEL)
    }

    fun compileGlobalEmoticonsPattern() {
        globalEmoticonsPattern = compileEmoticonPattern(emoticonFinder.getAll(Origin.BTTV_GLOBAL))
    }

    fun compileChannelEmoticonsPattern() {
        channelEmoticonsPattern = compileEmoticonPattern(emoticonFinder.getAll(Origin.BTTV_CHANNEL))
    }

    fun resetChannelEmoticonsPattern() {
        channelEmoticonsPattern = null
    }

    private fun handleEmoticons(message: TwitchMessage, pattern: Pattern?, origin: Origin) {
        if (pattern == null) return

        val matcher = pattern.matcher(message.text)
        while (matcher.find()) {
            val code = matcher.group(0)
            val emoticon = emoticonFinder.findByCode(origin, code)
            if (emoticon == null) {
                logger.warn("Emoticon code '{}' found in message, but emoticon is not. Message: '{}', emoticon origin: '{}'",
                        code, message.text, origin)
                return
            }

            val elementLabel = message.addElement(emoticon)
            message.text = matcher.replaceFirst(elementLabel)

            matcher.reset(message.text)
        }
    }

    private fun compileEmoticonPattern(emoticons: Collection<Emoticon>): Pattern {
        return if (emoticons.isEmpty()) throw IllegalArgumentException("List is empty")
        else emoticons
                .map { it.code.replace("""\""", """\\""") }
                .joinToString(separator = "\\E|\\Q", prefix = "\\Q", postfix = "\\E")
                .let { Pattern.compile(it) }
    }

}
