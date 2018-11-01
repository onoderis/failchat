package failchat.emoticon

import mu.KotlinLogging
import org.mapdb.DB
import org.mapdb.DBMaker
import java.nio.file.Files
import java.nio.file.Path

object EmoticonDbFactory {

    private val logger = KotlinLogging.logger {}

    fun create(dbPath: Path): DB {
        Files.createDirectories(dbPath.parent)

        val exception = try {
            return makeDb(dbPath)
        } catch (e: Exception) {
            e
        }

        val deleted = Files.deleteIfExists(dbPath)
        if (!deleted)
            throw exception

        logger.info("File '{}' deleted because of failure during initialization of the map DB. Trying to initialize " +
                "map DB again", dbPath)
        return makeDb(dbPath)
    }

    private fun makeDb(dbPath: Path): DB {
        return DBMaker
                .fileDB(dbPath.toFile())
                .closeOnJvmShutdown()
                .fileMmapEnable()
                .make()
                .also {
                    logger.info("DB was initialized at '{}'", dbPath)
                }
    }
}
