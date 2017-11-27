package failchat.youtube

class BroadcastNotFoundException(resourse: String, channelId: String) : Exception(
        "Youtube broadcast not found. resource: '$resourse', channel id: '$channelId'"
)
