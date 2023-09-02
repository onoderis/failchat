package failchat.twitch

import mu.KLogging
import org.apache.commons.configuration2.Configuration
import java.time.Instant

class ConfigurationTokenContainer(
        private val config: Configuration
) : HelixTokenContainer {

    private companion object : KLogging() {
        const val expiresAtKey = "twitch.bearer-token-expires-at"
        const val tokenKey = "twitch.bearer-token"
    }

    override fun getToken(): HelixApiToken? {
        val expiresAt = Instant.ofEpochMilli(config.getLong(expiresAtKey, 0))
        val now = Instant.now()
        if (now > expiresAt) {
            return null
        }

        return HelixApiToken(config.getString("twitch.bearer-token"), expiresAt)
    }

    override fun setToken(token: HelixApiToken) {
        config.setProperty(tokenKey, token.value)
        config.setProperty(expiresAtKey, token.ttl.toEpochMilli())
        logger.info("Helix token was saved to configuration at '$tokenKey'")
    }
}
