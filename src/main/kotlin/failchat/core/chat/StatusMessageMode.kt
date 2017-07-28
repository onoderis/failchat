package failchat.core.chat

enum class StatusMessageMode {
    EVERYWHERE,
    NATIVE_CLIENT,
    NOWHERE;

    val lowerCaseString: String = this.toString().toLowerCase()
}
