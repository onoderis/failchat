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

        return DBMaker
                .fileDB(dbPath.toFile())
                .closeOnJvmShutdown()
                .checksumHeaderBypass()
                .fileMmapEnable()
                .make()
                .also {
                    logger.info("DB was initialized at '{}'", dbPath)
                }
    }
}
