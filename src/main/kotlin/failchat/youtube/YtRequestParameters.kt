package failchat.youtube

data class YtRequestParameters(
        val liveChatId: String,
        val nextPageToken: String?,
        val isFirstRequest: Boolean
)
