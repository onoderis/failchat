package failchat.reporter

import failchat.ConfigLoader
import failchat.exception.UnexpectedResponseCodeException
import failchat.util.completedFuture
import failchat.util.thenApplySafe
import failchat.util.toFuture
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.concurrent.CompletableFuture

class EventReporter(
        private val clientId: String,
        private val httpClient: OkHttpClient,
        private val configLoader: ConfigLoader
) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(EventReporter::class.java)
    }

    private val config: Configuration = configLoader.get()
    private val enabled = config.getBoolean("reporter.enabled")

    // parameters
    private val failchatVersion = config.getString("version")
    private val trackingId = config.getString("reporter.tracking-id")
    private val languageTag = Locale.getDefault().toLanguageTag()

    // User-Agent header
    private val userAgent = "failchat/$failchatVersion (${sp("os.name")} ${sp("os.arch")} ${sp("os.version")}) " +
            "java${sp("java.version")}"


    fun reportEvent(category: EventCategory, action: EventAction): CompletableFuture<Unit> {
        if (!enabled) {
            log.debug("Reporter disabled, event {}.{} ignored", category, action)
            return completedFuture(Unit)
        }

        val url = HttpUrl.Builder()
                .scheme("https")
                .host("www.google-analytics.com")
//                .addPathSegment("debug")
                .addPathSegment("collect")
                .addQueryParameter("v", "1") //Protocol Version
                .addQueryParameter("tid", trackingId) //Tracking ID
                .addQueryParameter("cid", clientId) //Client ID
                .addQueryParameter("t", "event") //Hit Type
                .addQueryParameter("ec", category.queryParamValue) //Event Category
                .addQueryParameter("ea", action.queryParamValue) //Event Action
                .addQueryParameter("ul", languageTag) //User Language
                .addQueryParameter("an", "failchat") //Application Name
                .addQueryParameter("av", failchatVersion) //Application Version
//                .addQueryParameter("ni", if (action == Heartbeat) "1" else "0") // Non-Interaction Hit
                .build()

        val request = Request.Builder()
                .url(url)
                .get()
                .addHeader("User-Agent", userAgent)
                .build()

        return httpClient
                .newCall(request)
                .toFuture()
                .thenApplySafe { response ->
                    val code = response.code()
                    if (code !in 200..299) throw UnexpectedResponseCodeException(code)
                    log.info("Event successfully reported: {}.{}", category, action)
                }
    }

    /**
     * Shortcut for [System.getProperty].
     * */
    private fun sp(key: String): String? = System.getProperty(key)

}
