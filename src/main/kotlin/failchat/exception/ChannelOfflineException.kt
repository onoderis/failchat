package failchat.exception

import failchat.Origin

class ChannelOfflineException(
        val origin: Origin,
        val channel: String
) : Exception("origin: $origin, channel: $channel")
