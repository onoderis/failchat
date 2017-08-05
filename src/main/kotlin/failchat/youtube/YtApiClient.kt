package failchat.youtube

import com.google.api.services.youtube.YouTube
import failchat.util.debug
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class YtApiClient(private val youTube: YouTube) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(YtApiClient::class.java)
    }

    fun getChannelTitle(channelId: String): String? {
        // https://developers.google.com/youtube/v3/docs/channels/list
        val request = youTube.channels()
                .list("snippet")
                .setId(channelId)
        log.debug { "Sending request: $request" }

        val response = request.execute()
        log.debug { "Got response: $response" }

        return response.items.firstOrNull()?.snippet?.title
    }

    /**
     * @return null if live broadcast not found.
     * */
    fun findLiveBroadcast(channelId: String): String? = findBroadcast(channelId, "live")

    /**
     * @return null if upcoming broadcast not found.
     * */
    fun findUpcomingBroadcast(channelId: String): String? = findBroadcast(channelId, "upcoming")

    /**
     * @return null if live chat not found.
     * */
    fun getLiveChatId(liveBroadcastId: String): String? {
        val request = youTube.videos()
                .list("liveStreamingDetails")
                .setId(liveBroadcastId)
        log.debug { "Sending request: $request" }

        val response = request.execute()
        log.debug { "Got response: $response" }


        // items is empty if liveBroadcastId not found
        // activeLiveChatId отсутствует если стрим закончен (или не начат?)
        return response.items.firstOrNull()?.liveStreamingDetails?.activeLiveChatId
    }

    fun getViewersCount(videoId: String): Int? {
        // https://developers.google.com/youtube/v3/docs/videos#liveStreamingDetails.concurrentViewers
        val request = youTube.videos()
                .list("liveStreamingDetails")
                .setId(videoId)
        log.debug { "Sending request: $request" }

        val response = request.execute()
        log.debug { "Got response: $response" }

        return response.items.firstOrNull()?.liveStreamingDetails?.concurrentViewers?.intValueExact()
    }

    /**
     * @return broadcast id.
     * */
    private fun findBroadcast(channelId: String, eventType: String): String? {
        val request = youTube.search()
                .list("snippet")
                .setType("video")
                .setEventType(eventType)
                .setChannelId(channelId)
        log.debug { "Sending request: $request" }

        val response = request.execute()
        log.debug { "Got response: $response" }

        return response.items.firstOrNull()?.id?.videoId
    }

}
