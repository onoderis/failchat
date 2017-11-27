package failchat.youtube

class LiveChatNotFoundException : Exception {
    constructor(broadcastId: String, channelId: String) : super("Youtube live chat not found. resource: 'videos', " +
            "channel id: '$channelId', broadcast id: '$broadcastId'")
    constructor(broadcastId: String) : super("Youtube live chat not found. resource: 'videos', " +
            "broadcast id: '$broadcastId'")
}
