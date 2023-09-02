package failchat.twitch

import java.time.Instant

data class HelixApiToken(
        val value: String,
        val ttl: Instant
)
