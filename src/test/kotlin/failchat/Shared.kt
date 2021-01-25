package failchat

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import failchat.twitch.TwitchEmoticonUrlFactory
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import org.apache.commons.configuration2.Configuration

val okHttpClient: OkHttpClient = OkHttpClient.Builder()
//        .addInterceptor(okhttp3.logging.HttpLoggingInterceptor().also { it.level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY })
        .build()

val ktorClient = HttpClient(OkHttp) {
    engine {
        preconfigured = okHttpClient
    }
}

val config: Configuration by lazy { loadConfig() }
val privateConfig: Configuration by lazy { loadPrivateConfig() }
val objectMapper: ObjectMapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(KotlinModule())

val twitchEmoticonUrlFactory = TwitchEmoticonUrlFactory(
        config.getString("twitch.emoticon-url-prefix"),
        config.getString("twitch.emoticon-url-suffix")
)
