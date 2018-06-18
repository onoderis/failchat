package failchat.youtube

import failchat.Origin.YOUTUBE
import failchat.chat.ImageFormat.VECTOR
import failchat.chat.MessageHandler
import failchat.emoticon.Emoticon
import failchat.util.toCodePoint
import failchat.util.toHexString
import java.util.regex.Pattern

class YtEmojiHandler : MessageHandler<YtMessage> {

    private companion object {
        val emojiPattern: Pattern = Pattern.compile(
                "[\\x{231A}-\\x{2B55}\\x{1F004}-\\x{1F3FA}\\x{1F400}-\\x{1F9E6}][\\x{1F3FB}-\\x{1F3FF}]?")
    }

    override fun handleMessage(message: YtMessage) {
        val matcher = emojiPattern.matcher(message.text)

        while (matcher.find()) {
            val emoji = matcher.group()
            val emojiHexSequence = when (emoji.length) {
                1 -> emoji[0].toInt().toHexString()
                2 -> toCodePoint(emoji[0], emoji[1]).toHexString()
                3 -> emoji[0].toInt().toHexString() + "_" + toCodePoint(emoji[1], emoji[2]).toHexString()
                4 -> toCodePoint(emoji[0], emoji[1]).toHexString() + "_" + toCodePoint(emoji[2], emoji[3]).toHexString()
                else -> throw IllegalStateException("Regex should find 1-4 characters")
            }
            val emoticonUrl = "https://gaming.youtube.com/s/gaming/emoji/72836fb0/emoji_u$emojiHexSequence.svg"
            val emoticon = Emoticon(YOUTUBE, emoji, emoticonUrl, VECTOR)

            val elementLabel = message.addElement(emoticon)

            message.text = matcher.replaceFirst(elementLabel)
            matcher.reset(message.text)
        }
    }

}
