package failchat.core.chat

/**
 * Класс, сериализующийся в json для отправки к websocket клиентам.
 */
class Link(
        val fullUrl: String,
        val domain: String,
        val shortUrl: String
)
