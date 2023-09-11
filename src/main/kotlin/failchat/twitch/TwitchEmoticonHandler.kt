package failchat.twitch

import failchat.chat.MessageHandler
import failchat.util.notEmptyOrNull
import mu.KotlinLogging

class TwitchEmoticonHandler(
        private val twitchEmotesTagParser: TwitchEmotesTagParser
) : MessageHandler<TwitchMessage> {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    override fun handleMessage(message: TwitchMessage) {
        val emotesTag = message.tags.get(TwitchIrcTags.emotes).notEmptyOrNull() ?: return

        try {
            replaceEmoteCodes(emotesTag, message)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to replace emoticon codes with element labels. emotes tag: '$emotesTag', " +
                    "message text: '${message.text}'" }
        }
    }

    private fun replaceEmoteCodes(emotesTag: String, message: TwitchMessage) {
        val rangedEmoticons = twitchEmotesTagParser.parse(emotesTag, message.text)

        var offset = 0
        val sb = StringBuilder(message.text)
        rangedEmoticons.forEach { item ->
            val elementLabel = message.addElement(item.emoticon)
            val labelLength = elementLabel.length
            val position = item.position

            sb.replace(position.start + offset, position.endInclusive + 1 + offset, elementLabel)
            // positive - to the right, negative - to the left
            offset += labelLength - (position.endInclusive - position.start + 1)
        }

        message.text = sb.toString()

    }

}
