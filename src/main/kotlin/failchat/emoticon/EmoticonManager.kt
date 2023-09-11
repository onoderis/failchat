package failchat.emoticon

import failchat.emoticon.EmoticonLoadConfiguration.LoadType.BULK
import failchat.emoticon.EmoticonLoadConfiguration.LoadType.STREAM
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.map
import mu.KotlinLogging
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class EmoticonManager(
        private val storage: EmoticonStorage,
        private val scheduledExecutorService: ScheduledExecutorService
) {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    /**
     * Load emoticons by the specified configurations and put them into the storage. Blocking call
     * Never throws [Exception].
     * */
    fun actualizeEmoticons(loadConfigurations: List<EmoticonLoadConfiguration<out Emoticon>>) {
        loadConfigurations.forEach {
            try {
                actualizeEmoticons(it)
            } catch (e: Exception) {
                logger.warn("Exception during loading emoticons for {}", it.origin, e)
            }
        }
    }

    /**
     * Load emoticons by specified configuration and put them into the storage. Blocking call
     * */
    private fun <T : Emoticon> actualizeEmoticons(loadConfiguration: EmoticonLoadConfiguration<T>) {
        val origin = loadConfiguration.origin
        val emoticonsInStorage = storage.getCount(origin)

        // Load emoticon list via EmoticonBulkLoader or EmoticonStreamLoader and put it in the storage
        val loadResult = when (loadConfiguration.loadType) {
            BULK -> loadEmoticonBulk(loadConfiguration)
            STREAM -> loadEmoticonStream(loadConfiguration)
        }

        when (loadResult) {
            is LoadResult.Failure -> {
                logger.warn("Failed to load emoticon list for {}. Outdated list will be used, count: {}", origin, emoticonsInStorage)
            }
            is LoadResult.Success -> {
                logger.info("Emoticon list loaded for {}, count: {}", origin, loadResult.emoticonsLoaded)
            }
        }
    }

    private fun <T : Emoticon> loadEmoticonBulk(loadConfiguration: EmoticonLoadConfiguration<T>): LoadResult {
        val origin = loadConfiguration.origin

        var loadedSuccessfully = false
        var emoticons: List<T> = emptyList()

        for (bulkLoader in loadConfiguration.bulkLoaders) {
            try {
                emoticons = bulkLoader.loadEmoticons().join()
                loadedSuccessfully = true
                break
            } catch (e: Exception) {
                logger.warn(e) { "Failed to load emoticon list for $origin via bulk loader $bulkLoader" }
            }
        }

        if (!loadedSuccessfully)
            return LoadResult.Failure

        // Put data in storage
        val emoticonAndIdMapping = emoticons
                .map { EmoticonAndId(it, loadConfiguration.idExtractor.extractId(it)) }
        storage.putMapping(origin, emoticonAndIdMapping)

        return LoadResult.Success(emoticons.size)
    }

    private fun <T : Emoticon> loadEmoticonStream(loadConfiguration: EmoticonLoadConfiguration<T>): LoadResult {
        val origin = loadConfiguration.origin
        val idExtractor = loadConfiguration.idExtractor

        var loadedSuccessfully = false
        val count = AtomicInteger()

        for (streamLoader in loadConfiguration.streamLoaders) {
            count.set(0)
            val loggingTask = scheduledExecutorService.scheduleAtFixedRate({
                logger.info("Loading {} emoticons, loaded: {}", origin, count.get())
            }, 5, 5, TimeUnit.SECONDS)

            try {
                val emoticonsFlow = streamLoader.loadEmoticons()
                        .consumeAsFlow()
                        .map {
                            count.incrementAndGet()
                            EmoticonAndId(it, idExtractor.extractId(it))
                        }

                storage.putChannel(origin, emoticonsFlow)
                loadedSuccessfully = true
                break
            } catch (e: Exception) {
                logger.warn("Failed to load emoticons for {} via stream loader {}", origin, streamLoader, e)
            } finally {
                loggingTask.cancel(false)
            }
        }

        if (!loadedSuccessfully)
            return LoadResult.Failure

        return LoadResult.Success(count.get())
    }

    private sealed class LoadResult {
        class Success(val emoticonsLoaded: Int) : LoadResult()
        object Failure : LoadResult()
    }

}
