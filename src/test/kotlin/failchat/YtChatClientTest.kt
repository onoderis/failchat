package failchat

class YtChatClientTest {

    /*private companion object {
        val log: Logger = LoggerFactory.getLogger(YtChatClient::class.java)
    }

    val scopes = Lists.newArrayList(YouTubeScopes.YOUTUBE_READONLY)

    val credential = GoogleCredential
            .fromStream(YtChatClientTest::class.java.getResource("/config/failchat-service-account.json").openStream())
            .createScoped(scopes)

    val youTube = YouTube.Builder(NetHttpTransport(), JacksonFactory(), credential)
            .setApplicationName("failchat-test")
            .build()

    val ytApiClient = YtApiClient(youTube)

    @Ignore
    @Test
    fun getVideoIdTest() {
        val channelId = "UCFaL0aOHjRDKtbq0IGlFoxQ"

        val videoId = ytApiClient.getLiveBroadcastId(channelId)

        log.info("video id: {}", videoId)
    }

    @Ignore
    @Test
    fun tryIt() {
        val videoId = "DoDMk0KJOzw"

        val videoList = youTube.videos()
                .list("liveStreamingDetails")
                .setFields("items/liveStreamingDetails/activeLiveChatId")
                .setId(videoId)
        val response = videoList.execute()


        val liveChatId = response.getItems().get(0).getLiveStreamingDetails().getActiveLiveChatId()
        println("liveChatId: $liveChatId")


        val executor = Executors.newSingleThreadScheduledExecutor()

        val ytChatClient = YtChatClient(liveChatId, youTube, executor, MessageIdGenerator(0))

        ytChatClient.onChatMessage = { println("${it.author}: ${it.text}")}
        ytChatClient.onStatusMessage = { println("${it.status.name}")}

        ytChatClient.start()

        Thread.sleep(10000000)

    }*/

}
