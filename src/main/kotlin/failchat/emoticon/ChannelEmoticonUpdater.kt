package failchat.emoticon

import failchat.Origin
import failchat.Origin.BTTV_CHANNEL
import failchat.twitch.BttvApiClient
import failchat.twitch.BttvChannelNotFoundException
import failchat.twitch.FfzApiClient
import failchat.twitch.FfzChannelNotFoundException
import failchat.twitch.SevenTvApiClient
import failchat.twitch.SevenTvChannelNotFoundException
import kotlinx.coroutines.future.await
import mu.KLogging

class ChannelEmoticonUpdater(
        private val emoticonStorage: EmoticonStorage,
        private val bttvApiClient: BttvApiClient,
        private val ffzApiClient: FfzApiClient,
        private val sevenTvApiClient: SevenTvApiClient
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

    suspend fun update7tvEmoticons(channelId: Long) {
        val emoticons = try {
            sevenTvApiClient.loadChannelEmoticons(channelId)
        } catch (e: SevenTvChannelNotFoundException) {
            logger.info("7tv emoticons not found for channel '{}'", channelId)
            return
        }

        val emoticonToId = emoticons.map { EmoticonAndId(it, it.code) }
        emoticonStorage.putMapping(Origin.SEVEN_TV_CHANNEL, emoticonToId)
        logger.info("7tv emoticons loaded for channel '{}'", channelId)
    }

}
