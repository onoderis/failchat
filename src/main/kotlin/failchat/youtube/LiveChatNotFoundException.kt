package failchat.youtube

class LiveChatNotFoundException(channelId: String, broadcastId: String) : Exception(
        "Youtube live chat not found. resource: 'videos', channel id: '$channelId', broadcast id: '$broadcastId'"
)
