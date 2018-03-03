package failchat

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import org.apache.commons.configuration2.Configuration

val okHttpClient = OkHttpClient()
val config: Configuration by lazy { loadConfig() }
val privateConfig: Configuration by lazy { loadPrivateConfig() }
val objectMapper = ObjectMapper()
