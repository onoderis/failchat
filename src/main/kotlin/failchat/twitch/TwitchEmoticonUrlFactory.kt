package failchat.twitch

class TwitchEmoticonUrlFactory(
        private val prefix: String,
        private val suffix: String
) {

    fun create(id: Long) = prefix + id + suffix
}
