package failchat.twitch

import failchat.core.Origin
import failchat.core.chat.MessageHandler
import failchat.core.emoticon.EmoticonFinder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TwitchEmoticonHandler(private val emoticonFinder: EmoticonFinder) : MessageHandler<TwitchMessage> {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(TwitchEmoticonLoader::class.java)
    }

    override fun handleMessage(message: TwitchMessage) {
        /*
        * Message example:
        * emotes tag = "25:0-4,10-14/1902:16-20"
        * text = "Kappa 123 Kappa Keepo"
        * */
        val emotesTag = message.emotesTag ?: return

        try {
            replaceEmoteCodes(emotesTag, message)
        } catch (e: Exception) {
            log.warn("Failed to replace emoticon codes with element labels. emotes tag: '{}', message text: '{}'",
                    emotesTag, message.text, e)
        }
    }

    fun replaceEmoteCodes(emotesTag: String, message: TwitchMessage) {
        var offset: Int = 0
        val textBuilder = StringBuilder(message.text)

        emotesTag
                .split("/")
                .flatMap { emoteWithPositions ->
                    val (idString, positionsString) = emoteWithPositions.split(":", limit = 2)
                    val id = idString.toLong()

                    val emoticon = emoticonFinder.findById(Origin.twitch, id)
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

                    textBuilder.replace(position.start + offset, position.endInclusive + 1 + offset, elementLabel)
                    // positive - to the right, negative - to the left
                    offset += labelLength - (position.endInclusive - position.start + 1)
                }

        message.text = textBuilder.toString()

    }

    class RangingEmoticon(val emoticon: TwitchEmoticon, val position: IntRange)

}
