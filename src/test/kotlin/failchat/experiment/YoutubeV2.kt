package failchat.experiment


class YoutubeV2Test {
/*
    @Test
    fun test() = runBlocking<Unit> {
        val videoId = "3awohK49XUQ"


        val callbacks = ChatClientCallbacks(
                onChatMessage = {
                    println("${it.author.name}: ${it.text}")
                    println(it)
                    println()
                },
                onStatusUpdate = {},
                onChatMessageDeleted = {}
        )

        val ytClient = Youtube2ChatClient(callbacks, YoutubeClient2(ktorClient, objectMapper, YoutubeHtmlParser(objectMapper)), MessageIdGenerator(0), videoId)

        ytClient.start()

        delay(100000000)

    }
*/
//    private suspend fun sendLiveChatRequest(continuation: String, innertubeApiKey: String, sessionId: String): LiveChatResponse {
//        val messageRequestUrl = HttpUrl.parse(liveChatUrl)!!.newBuilder()
//                .addQueryParameter("commandMetadata", "[object Object]")
//                .addQueryParameter("continuation", continuation)
//                .addQueryParameter("pbj", "1")
//                .addQueryParameter("key", innertubeApiKey)
//                .build()
//
//
//        val requestBodyDto = LiveChatRequest(
//                context = LiveChatRequest.Context(
//                        request = LiveChatRequest.Request(sessionId = sessionId)
//                )
//        )
//
//
//        val liveChatRequestBody = RequestBody.create(MediaType.parse("application/json"), objectMapper.writeValueAsString(requestBodyDto))
//
//        val liveChatRequest = Request.Builder()
//                .post(liveChatRequestBody)
//                .url(messageRequestUrl)
//                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:79.0) Gecko/20100101 Firefox/79.0")
//                .addHeader("Accept", "*/*")
//                .addHeader("Accept-Language", "en-GB,en;q=0.5")
//                .addHeader("Referer", "https://www.youtube.com/live_chat?is_popout=1&v=iwBU6JG6ZyY")
//                .addHeader("X-Goog-Visitor-Id", "CgtvaTIycV9CTXMwSSjUiOP5BQ%3D%3D")
//                .addHeader("Origin", "https://www.youtube.com")
//                .addHeader("DNT", "1")
//                .addHeader("Connection", "keep-alive")
//                .addHeader("Cookie", "GPS=1; VISITOR_INFO1_LIVE=oi22q_BMs0I; YSC=VO8SubssLyM")
//                .addHeader("TE", "Trailers")
////                .addHeader("Content-Type", "application/json")
//                .build()
//
////        println("sending request")
//
//        val liveChatResponseString = okHttpClient.newCall(liveChatRequest)
//                .await()
//                .use {
//                    if (it.code() != 200) {
//                        error("code ${it.code()}")
//                    }
//                    it.body()!!.string()
//                }
//
//        return objectMapper.readValue(liveChatResponseString, LiveChatResponse::class.java)
//    }
}




