package failchat.youtube

class ChannelNotFoundException(channelId: String) : Exception(
        "Youtube channel not found. resource: 'channels', channel id: '$channelId'"
)
