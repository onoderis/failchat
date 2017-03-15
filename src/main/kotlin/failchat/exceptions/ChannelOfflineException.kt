package failchat.exceptions

import failchat.core.Origin

class ChannelOfflineException(
        val origin: Origin,
        val channel: String
) : Exception("origin: $origin, channel: $channel")
