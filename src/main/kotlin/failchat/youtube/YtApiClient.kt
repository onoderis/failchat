package failchat.youtube

import com.google.api.client.json.GenericJson
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeRequest
import com.google.api.services.youtube.model.LiveChatMessageListResponse
import mu.KLogging

class YtApiClient(private val youTube: YouTube) {

    private companion object : KLogging()

    fun getChannelTitle(channelId: String): String? {
        // https://developers.google.com/youtube/v3/docs/channels/list
        val request = youTube.channels()
                .list("snippet")
                .setId(channelId)
                .also { it.log() }

        val response = request.execute()
                .also { it.log() }

        return response.items.firstOrNull()?.snippet?.title
    }

    fun getChannelId(liveBroadcastId: String): String? {
        val request = youTube.videos()
                .list("snippet")
                .setId(liveBroadcastId)
                .also { it.log() }

        val response = request.execute()
                .also { it.log() }

        return response.items.firstOrNull()?.snippet?.channelId
    }

    /**
     * @return null if live broadcast not found.
     * */
    fun findFirstLiveBroadcast(channelId: String): String? = findBroadcast(channelId, "live")

    /**
     * @return null if upcoming broadcast not found.
     * */
    fun findFirstUpcomingBroadcast(channelId: String): String? = findBroadcast(channelId, "upcoming")

    /**
     * @return null if live chat not found.
     * */
    fun getLiveChatId(liveBroadcastId: String): String? {
        val request = youTube.videos()
                .list("liveStreamingDetails")
                .setId(liveBroadcastId)
                .also { it.log() }

        val response = request.execute()
                .also { it.log() }

        // items is empty if liveBroadcastId not found
        // activeLiveChatId отсутствует если стрим закончен (или не начат?)
        return response.items.firstOrNull()?.liveStreamingDetails?.activeLiveChatId
    }

    fun getViewersCount(videoId: String): Int? {
        // https://developers.google.com/youtube/v3/docs/videos#liveStreamingDetails.concurrentViewers
        val request = youTube.videos()
                .list("liveStreamingDetails")
                .setId(videoId)
                .also { it.log() }

        val response = request.execute()
                .also { it.log() }

        return response.items.firstOrNull()?.liveStreamingDetails?.concurrentViewers?.intValueExact()
    }

    fun getLiveChatMessages(liveChatId: String, nextPageToken: String? = null): LiveChatMessageListResponse {
        val request = youTube.LiveChatMessages()
                .list(liveChatId, "id, snippet, authorDetails") //не кидает исключение если невалидный id
                .setPageToken(nextPageToken)

        return request.execute()
                .also { it.log() }
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
                .also { it.log() }

        val response = request.execute()
                .also { it.log() }

        return response.items.firstOrNull()?.id?.videoId
    }

    private fun YouTubeRequest<*>.log() = logger.debug { "Sending request: $this" }
    private fun GenericJson.log() = logger.debug { "Got response: $this" }

}
