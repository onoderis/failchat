package failchat.youtube

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.YouTubeScopes
import org.apache.commons.configuration2.Configuration

object YouTubeFactory {

    private val readOnlyScope: List<String> = listOf(YouTubeScopes.YOUTUBE_READONLY)

    fun create(config: Configuration): YouTube {
        val credential = GoogleCredential
                .fromStream(YouTubeFactory::class.java.getResourceAsStream("/config/failchat-service-account.json"))
                .createScoped(readOnlyScope)


        return YouTube.Builder(NetHttpTransport(), JacksonFactory.getDefaultInstance(), credential)
                .setApplicationName("failchat ${config.getString("version")}")
                .build()
    }

}
