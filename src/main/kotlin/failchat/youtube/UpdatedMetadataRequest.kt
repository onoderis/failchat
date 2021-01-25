package failchat.youtube

data class UpdatedMetadataRequest(
        val context: Context,
        val videoId: String
) {

    data class Context(
            val client: Client
    )

    data class Client(
            val clientName: String,
            val clientVersion: String
    )

}
