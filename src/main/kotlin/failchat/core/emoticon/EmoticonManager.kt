package failchat.core.emoticon

import failchat.core.Origin
import org.apache.commons.configuration.CompositeConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.EnumMap

class EmoticonManager(
        private val workingDirectory: Path,
        private val config: CompositeConfiguration,
        private val emoticonLoaders: List<EmoticonLoader<out Emoticon>>
) {

    private companion object {
        val logger: Logger = LoggerFactory.getLogger(EmoticonManager::class.java)
    }

    private val emoticonDirectory: Path = workingDirectory.resolve("emoticons")
    private val loadedEmoticons: MutableMap<Origin, Map<out Any, Emoticon>> = EnumMap(Origin::class.java)


    fun find(origin: Origin, key: Any): Emoticon? = loadedEmoticons.get(origin)?.get(key)

    /**
     * Load all emoticon lists in memory. Blocking call.
     * */
    fun loadEmoticons() {
        Files.createDirectories(emoticonDirectory)

        val now = Instant.now()
        val updateInterval = Duration.ofMillis(config.getLong("emoticons.updating-delay"))

        //todo load in parallel?
        emoticonLoaders.forEach { loader ->
            try {
                load(loader, now, updateInterval)
            } catch (e: Exception) {
                logger.warn("Unexpected error during loading emoticons for origin {}", loader.origin, e)
            }
        }
    }

    /**
     * Load emoticons for single origin.
     */
    private fun load(loader: EmoticonLoader<out Emoticon>, now: Instant, updateInterval: Duration) {
        val origin = loader.origin
        val filePath = emoticonDirectory.resolve("${origin.name}.ser")
        val fileExists = Files.exists(filePath)
        val lastUpdatedDate = Instant.ofEpochMilli(config.getLong("${origin.name}.emoticons.last-updated"))
        val emoticonsOutdated = lastUpdatedDate.plus(updateInterval).isBefore(now)

        // Deserialize actual emoticon list
        if (!emoticonsOutdated && fileExists) {
            deserialize<Map<Any, Emoticon>>(filePath)?.let {
                loadedEmoticons.put(origin, it)
                logger.info("Actual version of emoticon list deserialized from file. origin: {}, count: {}", origin.name, it.size)
                return
            }
        }

        // Load emoticon list. Here we have outdated cached emoticons or cache not exists
        val loadedEmoticonMap = try {
            val emoticonMap: Map<out Any, Emoticon> = loader.loadEmoticons().join()
            loadedEmoticons.put(origin, emoticonMap)
            logger.info("Emoticon list loaded from origin {}. count: {}", origin.name, emoticonMap.size)
            emoticonMap
        } catch (e: Exception) {
            logger.warn("Failed to load emoticon list for {}", origin.name, e)
            null
        }

        // Deserialize outdated list if load failed and continue
        if (loadedEmoticonMap == null && fileExists) {
            deserialize<Map<Any, Emoticon>>(filePath)?.let {
                loadedEmoticons.put(origin, it)
                logger.info("Outdated version of emoticon list deserialized from file. origin: {}, count: {}", origin.name, it.size)
                return
            }
        }

        // Serialize loaded emoticon list
        try {
            serialize(loadedEmoticonMap!!, filePath)
            config.setProperty("${origin.name}.emoticons.last-updated", now.toEpochMilli())
            logger.info("Updated emoticon list serialized for origin {}", origin.name)
        } catch (e: Exception) {
            logger.warn("Failed to serialize updated emoticon list for origin: {}", origin.name, e)
        }

    }

    /**
     * @return true if object successfully serialized, false otherwise.
     * */
    private fun serialize(obj: Any, filePath: Path): Boolean {
        try {
            filePath.toFile().outputStream().use { fileOutputStream ->
                ObjectOutputStream(fileOutputStream).use { objectOutputStream ->
                    objectOutputStream.writeObject(obj)
                    return true
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to serialize object {} to file {}", obj, filePath, e)
            return false
        }
    }

    /**
     * @return [T] if object successfully deserialized, null otherwise.
     * */
    private fun <T> deserialize(filePath: Path): T? {
        try {
            filePath.toFile().inputStream().use { fileInputStream ->
                ObjectInputStream(fileInputStream).use { objectInputStream ->
                    return objectInputStream.readObject() as? T
                }
            }
        } catch (e: Exception) {
            logger.warn("Failed to deserialize object from file {}", filePath, e)
            return null
        }
    }

}