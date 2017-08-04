package failchat.youtube

import com.google.api.services.youtube.YouTube
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class YtApiClient(private val youTube: YouTube) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(YtApiClient::class.java)
    }

    /**
     * @return null if live broadcast not found.
     * */
    fun getLiveBroadcastId(channelId: String): String? = getBroadcastId(channelId, "live")

    /**
     * @return null if upcoming broadcast not found.
     * */
    fun getUpcomingBroadcastId(channelId: String): String? = getBroadcastId(channelId, "upcoming")

    /**
     * @return null if live chat not found.
     * */
    fun getLiveChatId(liveBroadcastId: String): String? {
        val videoListResponse = youTube.videos()
                .list("liveStreamingDetails")
//                .setFields("items/liveStreamingDetails/activeLiveChatId")
                .setId(liveBroadcastId)
                .execute()

        // items is empty if liveBroadcastId not found
        // activeLiveChatId отсутствует если стрим закончен (или не начат?)
        return videoListResponse.items.firstOrNull()?.liveStreamingDetails?.activeLiveChatId
    }

    fun getViewersCount(videoId: String): Int? {
        // https://developers.google.com/youtube/v3/docs/videos#liveStreamingDetails.concurrentViewers

        val videoListResponse = youTube.videos()
                .list("liveStreamingDetails")
                .setId(videoId)
                .execute()

        return videoListResponse.items.firstOrNull()?.liveStreamingDetails?.concurrentViewers?.intValueExact()
    }

    private fun getBroadcastId(channelId: String, eventType: String): String? {
        val searchResponse = youTube.search()
                .list("snippet")
                .setType("video")
                .setEventType(eventType)
                .setChannelId(channelId)
                .execute()

        return searchResponse.items.firstOrNull()?.id?.videoId
    }

}
