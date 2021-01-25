package failchat.youtube

data class LiveChatRequestParameters(
        val videoId: String,
        val channelName: String,
        val innertubeApiKey: String,
        val sessionId: String,
        val nextContinuation: String
)
