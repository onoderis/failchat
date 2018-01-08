package failchat.youtube

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeScopes
import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Random

object YouTubeFactory {

    private val log: Logger = LoggerFactory.getLogger(YouTubeFactory::class.java)
    private val readOnlyScope: List<String> = listOf(YouTubeScopes.YOUTUBE_READONLY)
    private val random = Random()

    fun create(config: Configuration): YouTube {
        val serviceAccountNumber = random.nextInt(10)
        val resource = "/config/failchat-service-account-$serviceAccountNumber.json"
        log.info("Creating youtube credentials from resource '{}'", resource)

        val credential = GoogleCredential
                .fromStream(YouTubeFactory::class.java.getResourceAsStream(resource))
                .createScoped(readOnlyScope)

        return YouTube.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance(), credential)
                .setApplicationName("failchat ${config.getString("version")}")
                .build()
    }

}
