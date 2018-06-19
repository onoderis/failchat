package failchat.twitch

import java.io.Serializable

class TwitchEmoticonUrlFactory(
        private val prefix: String,
        private val suffix: String
) : Serializable {

    fun create(id: Long) = prefix + id + suffix
}
