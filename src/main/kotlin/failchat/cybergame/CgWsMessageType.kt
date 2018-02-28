package failchat.cybergame

/** Supported types of incoming websocket message. */
enum class CgWsMessageType(val jsonValue: String) {
    STATE("state"),
    MESSAGE("msg"),
    CLEAR("clear")
}