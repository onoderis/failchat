package failchat.youtube

class BroadcastNotFoundException(channelId: String) : Exception(
        "Youtube broadcast not found. resource: 'search', channel id: '$channelId'"
)
