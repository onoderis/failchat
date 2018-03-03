package failchat.chat

/**
 * Класс, сериализующийся в json для отправки к websocket клиентам.
 */
data class Link(
        val fullUrl: String,
        val domain: String,
        val shortUrl: String
) : MessageElement
