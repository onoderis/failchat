package failchat.twitch

data class EmotesResponse(
        val data: List<Data>
) {

    data class Data(
            val id: String,
            val name: String
    )
}
