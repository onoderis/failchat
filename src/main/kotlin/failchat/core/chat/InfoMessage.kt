package failchat.core.chat

import failchat.core.Origin
import java.time.Instant

class InfoMessage(
        val id: Long,
        val origin: Origin,
        val text: String,
        val timestamp: Instant = Instant.now()
)
