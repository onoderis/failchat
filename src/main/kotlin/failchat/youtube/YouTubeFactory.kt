package failchat.youtube

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeScopes
import mu.KotlinLogging
import org.apache.commons.configuration2.Configuration
import java.util.Random

object YouTubeFactory {

    private val logger = KotlinLogging.logger {}
    private val readOnlyScope: List<String> = listOf(YouTubeScopes.YOUTUBE_READONLY)
    private val random = Random()

    fun create(config: Configuration): YouTube {
        val serviceAccountNumber = random.nextInt(9) + 1
        val resource = "/config/youtube/failchat-service-account-$serviceAccountNumber.json"
        logger.info("Creating youtube credentials from resource '{}'", resource)

        val credential = GoogleCredential
                .fromStream(YouTubeFactory::class.java.getResourceAsStream(resource))
                .createScoped(readOnlyScope)

        return YouTube.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance(), credential)
                .setApplicationName("failchat ${config.getString("version")}")
                .build()
    }

}
