package failchat

import com.fasterxml.jackson.databind.ObjectMapper
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


val userHomeConfig: Configuration by lazy { ConfigLoader(getFailchatHomePath()).load() }
val defaultConfig: Configuration by lazy { loadDefaultConfig() }
val testObjectMapper: ObjectMapper = objectMapper()
