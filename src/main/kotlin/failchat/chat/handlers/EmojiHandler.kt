package failchat.chat.handlers

import com.vdurmont.emoji.EmojiParser
import failchat.chat.ChatMessage
import failchat.chat.MessageHandler
import failchat.emoticon.EmojiEmoticon
import failchat.util.toCodePoint
import failchat.util.toHexString

/**
 * Searches for unicode emojis in message and replaces them with svg images.
 * */
class EmojiHandler : MessageHandler<ChatMessage> {

    override fun handleMessage(message: ChatMessage) {
        val transformedText = EmojiParser.parseFromUnicode(message.text) {
            val hexEmoji = toHex(it.emoji.unicode)
            val hexFitzpatrick: String? = it.fitzpatrick?.let { f ->
                toHex(f.unicode)
            }

            val emojiHexSequence = if (hexFitzpatrick == null) {
                hexEmoji
            } else {
                "$hexEmoji-$hexFitzpatrick"
            }

            val emoticonUrl = "https://cdnjs.cloudflare.com/ajax/libs/twemoji/13.0.1/svg/$emojiHexSequence.svg"
            val emoticon = EmojiEmoticon(it.emoji.description ?: "emoji", emoticonUrl)

            message.addElement(emoticon)
        }

        message.text = transformedText
    }

    private fun toHex(emojiCharacters: String): String {
        return when (emojiCharacters.length) {
            1 -> emojiCharacters[0].toInt().toHexString()
            2 -> toCodePoint(emojiCharacters[0], emojiCharacters[1]).toHexString()
            else -> error("Unexpected emoji characters: '$emojiCharacters'")
        }
    }

}
