package failchat.emoticon

import failchat.Origin
import failchat.Origin.BTTV_CHANNEL
import failchat.twitch.BttvApiClient
import failchat.twitch.BttvChannelNotFoundException
import failchat.twitch.FfzApiClient
import failchat.util.CoroutineExceptionLogger
import failchat.util.completionCause
import failchat.util.logException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import mu.KLogging
import java.util.concurrent.ExecutorService

class ChannelEmoticonUpdater(
        private val emoticonStorage: EmoticonStorage,
        private val bttvApiClient: BttvApiClient,
        private val ffzApiClient: FfzApiClient,
        backgroundExecutor: ExecutorService
) {

    private companion object : KLogging()

    private val backgroundExecutorDispatcher = backgroundExecutor.asCoroutineDispatcher()

    fun update(twitchChannelName: String) {
        loadBttvEmoticonsAsync(twitchChannelName)
        loadFfzEmoticonsAsync(twitchChannelName)
    }

    private fun loadBttvEmoticonsAsync(channelName: String) {
        bttvApiClient.loadChannelEmoticons(channelName)
                .thenApply<Unit> { emoticons ->
                    val emoticonAndIdMapping = emoticons
                            .map { EmoticonAndId(it, it.bttvId) }
                    emoticonStorage.putMapping(BTTV_CHANNEL, emoticonAndIdMapping)
                    logger.info("BTTV emoticons loaded for channel '{}', count: {}", channelName, emoticonAndIdMapping.size)
                }
                .exceptionally { t ->
                    val completionCause = t.completionCause()
                    if (completionCause is BttvChannelNotFoundException) {
                        logger.info("BTTV emoticons not found for channel '{}'", completionCause.channel)
                    } else {
                        logger.error("Failed to load BTTV emoticons for channel '{}'", channelName, completionCause)
                    }
                }
                .logException()
    }

    private fun loadFfzEmoticonsAsync(channelName: String) {
        CoroutineScope(backgroundExecutorDispatcher + CoroutineExceptionLogger).launch {
            val emoticonsMapping = ffzApiClient.requestEmoticons(channelName)
                    .map { EmoticonAndId(it, it.code) }
            emoticonStorage.putMapping(Origin.FRANKERFASEZ, emoticonsMapping)
            logger.info("FrankerFaceZ emoticons loaded for channel '{}'", channelName)
        }
    }
}
