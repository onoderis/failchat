package failchat.chat

enum class StatusMessageMode(val jsonValue: String) {
    EVERYWHERE("everywhere"),
    NATIVE_CLIENT("native_client"),
    NOWHERE("nowhere");
}
