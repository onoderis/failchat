package failchat.twitch

import failchat.Origin
import failchat.chat.MessageHandler
import failchat.emoticon.EmoticonFinder
import mu.KLogging

//todo don't use EmoticonFinder
class TwitchEmoticonHandler(private val emoticonFinder: EmoticonFinder) : MessageHandler<TwitchMessage> {

    private companion object : KLogging()

    override fun handleMessage(message: TwitchMessage) {
        /*
        * Message example:
        * emotes tag = "25:0-4,10-14/1902:16-20"
        * text = "Kappa 123 Kappa Keepo"
        *
        * If there is not emotes, emotesTag == null
        * */
        val emotesTag = message.emotesTag ?: return

        try {
            replaceEmoteCodes(emotesTag, message)
        } catch (e: Exception) {
            logger.warn("Failed to replace emoticon codes with element labels. emotes tag: '{}', message text: '{}'",
                    emotesTag, message.text, e)
        }
    }

    fun replaceEmoteCodes(emotesTag: String, message: TwitchMessage) {
        var offset = 0
        val sb = StringBuilder(message.text)

        emotesTag
                .split("/")
                .flatMap { emoteWithPositions ->
                    val (idString, positionsString) = emoteWithPositions.split(":", limit = 2)
                    val id = idString.toLong()

                    val emoticon = emoticonFinder.findById(Origin.TWITCH, id.toString())
                            as? TwitchEmoticon
                            ?: return@flatMap emptyList<RangingEmoticon>()

                    val positions = positionsString
                            .split(",")
                            .map {
                                val (start, end) = it.split("-", limit = 2)
                                IntRange(start.toInt(), end.toInt())
                            }

                    return@flatMap positions.map { RangingEmoticon(emoticon, it) }
                }
                .sortedBy { it.position.start }
                .forEach { item ->
                    val elementLabel = message.addElement(item.emoticon)
                    val labelLength = elementLabel.length
                    val position = item.position

                    sb.replace(position.start + offset, position.endInclusive + 1 + offset, elementLabel)
                    // positive - to the right, negative - to the left
                    offset += labelLength - (position.endInclusive - position.start + 1)
                }

        message.text = sb.toString()

    }

    class RangingEmoticon(val emoticon: TwitchEmoticon, val position: IntRange)

}
