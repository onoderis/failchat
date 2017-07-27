package failchat.core.chat

enum class InfoMessageMode {
    EVERYWHERE,
    NATIVE_CLIENT,
    NOWHERE;

    val lowerCaseString: String = this.toString().toLowerCase()
}
