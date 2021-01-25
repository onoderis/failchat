package failchat.youtube2

class YoutubeClientException(
        override val message: String? = null,
        override val cause: Throwable? = null
) : RuntimeException()
