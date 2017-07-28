package failchat.core.emoticon

import failchat.core.Origin
import failchat.exception.EmoticonLoadException
import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant

class EmoticonManager(
        private val workingDirectory: Path,
        private val config: Configuration
) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(EmoticonManager::class.java)
    }

    private val updateInterval = Duration.ofMillis(config.getLong("emoticons.updating-delay"))
    private val emoticonDirectory: Path = workingDirectory.resolve("emoticons")

    /**
     * Load emoticons and put it in storage. Also saves emoticons in cache file.
     * Blocking call.
     * */
    fun <T : Emoticon> loadInStorage(storage: EmoticonStorage, loader: EmoticonLoader<T>, options: EmoticonStoreOptions) {
        Files.createDirectories(emoticonDirectory)

        val now = Instant.now()
        val origin = loader.origin
        val cacheFile = emoticonDirectory.resolve("${origin.name}.ser")

        val (emoticons, loadedFrom) = load(loader, cacheFile, now)

        // Save to cache file if required
        if (loadedFrom == LoadSource.loader && isCacheOutdated(loader.origin, now)) {
            try {
                saveToCache(emoticons, cacheFile)
                config.setProperty("${origin.name}.emoticons.last-updated", now.toEpochMilli())
                log.info("Updated emoticon list saved to cache file for origin {}", origin.name)
            } catch (e: Exception) {
                log.warn("Failed to save updated emoticon list to cache file. origin {}", origin.name)
            }
        }

        // Put data in storage
        if (options.storeByCode) {
            val codeToEmoticon = emoticons
                    .map { it.code.toLowerCase() to it }
                    .toMap((HashMap()))
            storage.putCodeMapping(origin, codeToEmoticon)
        }
        if (options.storeById) {
            val idToEmoticon = emoticons
                    .map { loader.getId(it) to it }
                    .toMap((HashMap()))
            storage.putIdMapping(origin, idToEmoticon)
        }
    }

    /**
     * Load emoticons from cache file or via emoticon loader, depends on whether the list is outdated.
     * @return list of emoticons and load method.
     */
    private fun <T : Emoticon> load(loader: EmoticonLoader<T>, cacheFile: Path, now: Instant): Pair<List<T>, LoadSource> {
        val origin = loader.origin
        val fileExists = Files.exists(cacheFile)

        // Load from  cache file actual emoticon list
        if (!isCacheOutdated(origin, now) && fileExists) {
            try {
                val emoticons = loadFromCache<List<T>>(cacheFile)
                log.info("Actual version of emoticon list loaded from cache file. origin: {}, count: {}", origin.name, emoticons.size)
                return emoticons to LoadSource.cache
            } catch (e: Exception) {
                log.warn("Failed to load actual emoticon list from cache file. origin: {}", origin.name, e)
            }
        }
        // else: outdated emoticons in cache file or cache file not exists

        // Load emoticon list via EmoticonLoader
        try {
            val emoticons = loader.loadEmoticons().join()
            log.info("Emoticon list loaded from origin {}. count: {}", origin.name, emoticons.size)
            return emoticons to LoadSource.loader
        } catch (e: Exception) {
            log.warn("Failed to load emoticon list for {}", origin.name, e)
        }

        // Load outdated list from cache file if load via EmoticonLoader failed
        if (fileExists) {
            val emoticons = loadFromCache<List<T>>(cacheFile)
            log.info("Outdated version of emoticon list loaded from cache file. origin: {}, count: {}", origin.name, emoticons.size)
            return emoticons to LoadSource.cache
        }

        throw EmoticonLoadException("Failed to load emoticons for origin ${origin.name}. Cache file exists: $fileExists")
    }

    private fun isCacheOutdated(origin: Origin, now: Instant): Boolean {
        val lastUpdatedDate = Instant.ofEpochMilli(config.getLong("${origin.name}.emoticons.last-updated"))
        return lastUpdatedDate.plus(updateInterval).isBefore(now)
    }

    private fun saveToCache(obj: Any, filePath: Path) {
        filePath.toFile().outputStream().use { fileOutputStream ->
            ObjectOutputStream(fileOutputStream).use { objectOutputStream ->
                objectOutputStream.writeObject(obj)
            }
        }
    }

    private fun <T> loadFromCache(filePath: Path): T {
        filePath.toFile().inputStream().use { fileInputStream ->
            ObjectInputStream(fileInputStream).use { objectInputStream ->
                return objectInputStream.readObject() as T
            }
        }
    }

    private enum class LoadSource {
        loader, cache
    }

}
