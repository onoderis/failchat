package failchat.twitch

data class SevenTvEmoteSetResponse(
        val emotes: List<Emote> = listOf()
) {

    data class Emote(
            val id: String,
            val name: String,
            val data: Data
    )

    data class Data(
            val host: Host
    )

    data class Host(
            val url: String,
            val files: List<File>
    )

    data class File(
            val name: String,
            val format: String,
            val width: Int
    )
}
