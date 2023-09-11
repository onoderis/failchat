package failchat.reporter

import mu.KotlinLogging
import org.apache.commons.codec.binary.Hex
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.UUID

class UserIdManager(private val homeDirectory: Path) {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    private val userIdHomeFile: Path = homeDirectory.resolve("user-id")


    fun getUserId(): String {
        findUserId()?.let { return it }
        generateIdAndTrySave()?.let { return it }
        return generateBackupId()
    }

    private fun findUserId(): String? {
        if (!Files.exists(userIdHomeFile)) return null

        val userId = String(Files.readAllBytes(userIdHomeFile))
        logger.debug("User id '{}' was read from '{}'", userId, userIdHomeFile)
        return userId
    }

    /** @return user id if it saved successfully, null otherwise. */
    private fun generateIdAndTrySave(): String? {
        val generatedUserId = UUID.randomUUID().toString()
        logger.info("User id generated: '{}'", generatedUserId)

        try {
            Files.createDirectories(homeDirectory)
            Files.write(userIdHomeFile, generatedUserId.toByteArray())
            logger.info("User id '{}' saved to home file: '{}'", generatedUserId, userIdHomeFile)
            return generatedUserId
        } catch (e: Exception) {
            logger.warn("Failed to save user id '{}' to home file '{}'", generatedUserId, userIdHomeFile, e)
        }

        return null
    }

    /** Generate backup user-id from user.name hash. */
    private fun generateBackupId(): String {
        val userName: String = System.getProperty("user.name")

        val hash = MessageDigest.getInstance("SHA-256").digest(userName.toByteArray())
        val hashChars = Hex.encodeHex(hash, true)
        val backupUserId = "00000000-0000-0000-0000-" + String(hashChars.copyOfRange(0, 12))

        logger.info("Backup user id generated '{}'", backupUserId)
        return backupUserId
    }

}
