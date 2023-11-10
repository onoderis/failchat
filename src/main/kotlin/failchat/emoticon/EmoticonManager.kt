package failchat.emoticon

import mu.KotlinLogging

class EmoticonManager(
        private val storage: EmoticonStorage
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

        val loadResult = loadEmoticons(loadConfiguration)

        when (loadResult) {
            is LoadResult.Failure -> {
                logger.warn {"Failed to load emoticon list for $origin. Outdated list will be used, count: $emoticonsInStorage" }
            }
            is LoadResult.Success -> {
                logger.info { "Emoticon list loaded for $origin, count: ${loadResult.emoticonsLoaded}" }
            }
        }
    }

    private fun <T : Emoticon> loadEmoticons(loadConfiguration: EmoticonLoadConfiguration<T>): LoadResult {
        val origin = loadConfiguration.origin

        val emoticons = try {
            loadConfiguration.loader.loadEmoticons().join()
        } catch (e: Exception) {
            logger.warn(e) { "Failed to load emoticon list for $origin via bulk loader ${loadConfiguration.loader}" }
            return LoadResult.Failure
        }

        // Put data in storage
        val emoticonAndIdMapping = emoticons
                .map { EmoticonAndId(it, loadConfiguration.idExtractor.extractId(it)) }
        storage.clear(origin)
        storage.putMapping(origin, emoticonAndIdMapping)

        return LoadResult.Success(emoticons.size)
    }

    private sealed class LoadResult {
        class Success(val emoticonsLoaded: Int) : LoadResult()
        object Failure : LoadResult()
    }

}
