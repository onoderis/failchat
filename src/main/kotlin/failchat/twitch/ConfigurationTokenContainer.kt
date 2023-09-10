package failchat.twitch

import failchat.ConfigKeys
import mu.KLogging
import org.apache.commons.configuration2.Configuration
import java.time.Instant

class ConfigurationTokenContainer(
        private val config: Configuration
) : HelixTokenContainer {

    private companion object : KLogging()

    override fun getToken(): HelixApiToken? {
        val expiresAt = Instant.ofEpochMilli(config.getLong(ConfigKeys.Twitch.expiresAt, 0))
        val now = Instant.now()
        if (now > expiresAt) {
            return null
        }

        return HelixApiToken(config.getString("twitch.bearer-token"), expiresAt)
    }

    override fun setToken(token: HelixApiToken) {
        config.setProperty(ConfigKeys.Twitch.token, token.value)
        config.setProperty(ConfigKeys.Twitch.expiresAt, token.expiresAt.toEpochMilli())
        logger.info("Helix token was saved to configuration")
    }
}
