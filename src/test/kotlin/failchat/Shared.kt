package failchat

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.twitch.TwitchEmoticonUrlFactory
import failchat.util.objectMapper
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import org.apache.commons.configuration2.Configuration

val okHttpClient: OkHttpClient = OkHttpClient.Builder()
//        .addInterceptor(HttpLoggingInterceptor().also { it.level = HttpLoggingInterceptor.Level.BODY })
        .build()

val ktorClient = HttpClient(OkHttp) {
    engine {
        preconfigured = okHttpClient
    }
}

val config: Configuration by lazy { loadConfig() }
val privateConfig: Configuration by lazy { loadPrivateConfig() }
val testObjectMapper: ObjectMapper = objectMapper()

val twitchEmoticonUrlFactory = TwitchEmoticonUrlFactory(
        config.getString("twitch.emoticon-url-prefix"),
        config.getString("twitch.emoticon-url-suffix")
)
