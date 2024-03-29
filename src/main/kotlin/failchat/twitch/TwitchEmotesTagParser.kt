package failchat.twitch

class TwitchEmotesTagParser {

    fun parse(emotesTag: String, messageText: String): List<RangedEmoticon> {
        /*
        * Message example:
        * emotes tag = "25:0-4,10-14/1902:16-20"
        * text = "Kappa 123 Kappa Keepo"
        *
        * If there is no emotes, emotesTag == null
        * */
        return emotesTag
                .split("/")
                .flatMap { emoteWithPositions ->
                    val (emoticonId, positionsString) = emoteWithPositions.split(":", limit = 2)

                    val positions = positionsString
                            .split(",")
                            .map {
                                val (start, end) = it.split("-", limit = 2)
                                IntRange(start.toInt(), end.toInt())
                            }

                    positions.map {
                        val emoticon = TwitchEmoticon(
                                twitchId = emoticonId,
                                code = messageText.substring(it)
                        )

                        RangedEmoticon(emoticon, it)
                    }

                }
                .sortedBy { it.position.start }
    }
}

