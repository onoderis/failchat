package failchat.emoticon

import failchat.ConfigKeys
import failchat.Origin
import failchat.emoticon.EmoticonLoadConfiguration.LoadType.BULK
import failchat.emoticon.EmoticonLoadConfiguration.LoadType.STREAM
import failchat.emoticon.EmoticonManager.LoadResult.Failure
import failchat.emoticon.EmoticonManager.LoadResult.Success
import kotlinx.coroutines.channels.map
import mu.KLogging
import org.apache.commons.configuration2.Configuration
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class EmoticonManager(
        private val config: Configuration,
        private val storage: EmoticonStorage,
        private val scheduledExecutorService: ScheduledExecutorService
) {

    private companion object : KLogging()

    private val updateInterval = Duration.ofMillis(config.getLong("emoticons.updating-delay"))

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
    fun <T : Emoticon> actualizeEmoticons(loadConfiguration: EmoticonLoadConfiguration<T>) {
        val now = Instant.now()
        val origin = loadConfiguration.origin
        val emoticonsInStorage = storage.getCount(origin)
        val cacheExists = emoticonsInStorage > 0

        // Actual emoticon list already loaded
        if (!isCacheOutdated(origin, now) && cacheExists) {
            logger.info("Actual version of emoticon already in storage. origin: {}, count: {}", origin, emoticonsInStorage)
            return
        }
        // else: outdated emoticons in cache or cache is empty (first run)


        // Load emoticon list via EmoticonBulkLoader or EmoticonStreamLoader and put it in the storage
        val loadResult = when (loadConfiguration.loadType) {
            BULK -> loadEmoticonBulk(loadConfiguration)
            STREAM -> loadEmoticonStream(loadConfiguration)
        }

        when (loadResult) {
            is Failure -> {
                logger.warn("Failed to load emoticon list for {}. Outdated list will be used, count: {}", origin, emoticonsInStorage)
            }
            is Success -> {
                config.setProperty(ConfigKeys.lastUpdatedEmoticons(origin), now.toEpochMilli())
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
                logger.warn("Failed to load emoticon list for {} via bulk loader {}", origin, e, bulkLoader)
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
                val emoticonsChannel = streamLoader.loadEmoticons()
                        .map {
                            count.incrementAndGet()
                            EmoticonAndId(it, idExtractor.extractId(it))
                        }

                storage.putChannel(origin, emoticonsChannel)
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

    private fun isCacheOutdated(origin: Origin, now: Instant): Boolean {
        val lastUpdatedDate = Instant.ofEpochMilli(config.getLong(ConfigKeys.lastUpdatedEmoticons(origin)))
        return lastUpdatedDate.plus(updateInterval).isBefore(now)
    }

    private sealed class LoadResult {
        class Success(val emoticonsLoaded: Int) : LoadResult()
        object Failure : LoadResult()
    }

}
