package failchat.chat

import failchat.Origin

data class StatusUpdate(
        val origin: Origin,
        val status: OriginStatus
)
