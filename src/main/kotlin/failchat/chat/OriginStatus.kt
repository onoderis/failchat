package failchat.chat

enum class OriginStatus {
    CONNECTED,
    DISCONNECTED;

    val lowerCaseString: String = this.name.toLowerCase()
}
