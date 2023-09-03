package failchat.twitch

object TwitchEmoticonUrlFactory {
    fun create(id: String): String {
        return "https://static-cdn.jtvnw.net/emoticons/v2/$id/default/light/1.0"
    }
}
