package failchat.emoticon

import failchat.Origin
import failchat.Origin.BTTV_CHANNEL
import failchat.twitch.BttvApiClient
import failchat.twitch.BttvChannelNotFoundException
import failchat.twitch.FfzApiClient
import failchat.twitch.FfzChannelNotFoundException
import kotlinx.coroutines.future.await
import mu.KLogging

class ChannelEmoticonUpdater(
        private val emoticonStorage: EmoticonStorage,
        private val bttvApiClient: BttvApiClient,
        private val ffzApiClient: FfzApiClient
) {

    private companion object : KLogging()

    suspend fun updateBttvEmoticons(channelName: String) {
        val emoticons = try {
            bttvApiClient.loadChannelEmoticons(channelName).await()
        } catch (e: BttvChannelNotFoundException) {
            logger.info("BTTV emoticons not found for channel '{}'", channelName)
            return
        }

        val emoticonToId = emoticons.map { EmoticonAndId(it, it.code) }
        emoticonStorage.putMapping(BTTV_CHANNEL, emoticonToId)
        logger.info("BTTV emoticons loaded for channel '{}', count: {}", channelName, emoticonToId.size)
    }

    suspend fun updateFfzEmoticons(channelName: String) {
        val emoticons = try {
            ffzApiClient.requestEmoticons(channelName)
        } catch (e: FfzChannelNotFoundException) {
            logger.info("FrankerFaceZ emoticons not found for channel '{}'", channelName)
            return
        }

        val emoticonToId = emoticons.map { EmoticonAndId(it, it.code) }
        emoticonStorage.putMapping(Origin.FRANKERFASEZ, emoticonToId)
        logger.info("FrankerFaceZ emoticons loaded for channel '{}'", channelName)
    }
}
