package failchat.core.reporter

import failchat.core.ConfigLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.UUID

class UserIdLoader(private val configLoader: ConfigLoader) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(UserIdLoader::class.java)
        const val idKey = "reporter.user-uuid"
    }

    private val homeAppDirectory: Path = Paths.get(System.getProperty("user.home")).resolve(".failchat")
    private val userIdHomeFile: Path = homeAppDirectory.resolve("user-id")

    /**
     * Find user id in config/home directory or generate it.
     * */
    fun getUserId(): String {
        val config = configLoader.get()
        val configUserId = config.getString(idKey)
        if (!configUserId.isNullOrEmpty()) {
            log.debug("User id was read from config: '{}'", configUserId)
            return configUserId
        }

        if (Files.exists(userIdHomeFile)) {
            try {
                val homeUserId = String(Files.readAllBytes(userIdHomeFile))
                log.debug("User id '{}' was read from home file", homeUserId)
                return homeUserId
            } catch (e: Exception) {
                log.warn("Failed to load user id from home file '{}'", userIdHomeFile, e)
            }
        }

        val generatedUserId = UUID.randomUUID().toString()
        log.info("User id generated: '{}'", generatedUserId)
        return generatedUserId
    }

    /**
     * Save user id to config and home directory.
     * */
    fun saveUserId(userId: String) {
        // save to config
        val config = configLoader.get()
        if (config.getString(idKey).isNullOrEmpty()) {
            config.setProperty(idKey, userId)
            configLoader.save()
            log.info("User id '{}' saved to config", userId)
        } else {
            log.debug("User id config property exists, save skipped")
        }


        // save to home file
        if (Files.exists(userIdHomeFile)) {
            log.debug("User id home file exists, save skipped")
            return
        }
        try {
            Files.createDirectories(homeAppDirectory)
            Files.write(userIdHomeFile, userId.toByteArray())
            log.info("User id '{}' saved to home file: '{}'", userId, userIdHomeFile)
        } catch (e: Exception) {
            log.warn("Failed to save user id '{}' to home file '{}'", userId, userIdHomeFile, e)
        }
    }

}
