package failchat.youtube

class YtChannelNotFoundException(channelId: String) : Exception(
        "Youtube channel not found. resource: 'channels', channel id: '$channelId'"
)
