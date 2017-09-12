package failchat.chat

import failchat.Origin
import java.time.Instant

class StatusMessage(
        val origin: Origin,
        val status: OriginStatus,
        val timestamp: Instant = Instant.now()
)
