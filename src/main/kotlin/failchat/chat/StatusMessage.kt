package failchat.chat

import failchat.Origin
import java.time.Instant

data class StatusMessage(
        val origin: Origin,
        val status: OriginStatus,
        val timestamp: Instant = Instant.now()
)
