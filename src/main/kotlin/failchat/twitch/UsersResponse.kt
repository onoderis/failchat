package failchat.twitch

data class UsersResponse(
        val data: List<Data>
) {
    data class Data(
            val id: Long
    )
}
