package failchat.youtube

import either.Either
import either.Left
import either.Right

object YoutubeUtils {

    fun determineId(value: String): Either<ChannelId, VideoId> {
        // channelId - 24 characters
        // videoId - 11 characters
        return if (value.length in 0..17) {
            Right(value)
        } else {
            Left(value)
        }
    }

}
