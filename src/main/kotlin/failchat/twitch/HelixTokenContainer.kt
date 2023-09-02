package failchat.twitch

interface HelixTokenContainer {
    fun getToken(): HelixApiToken?
    fun setToken(token: HelixApiToken)
}
