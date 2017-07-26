package failchat.core.reporter

import failchat.core.ConfigLoader
import failchat.core.reporter.EventAction.Heartbeat
import failchat.exceptions.UnexpectedResponseCodeException
import failchat.utils.completedFuture
import failchat.utils.thenApplySafe
import failchat.utils.toFuture
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Locale
import java.util.UUID
import java.util.concurrent.CompletableFuture

class EventReporter(
        private val httpClient: OkHttpClient,
        private val configLoader: ConfigLoader
) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(EventReporter::class.java)
        const val idKey = "reporter.user-uuid"
    }

    private val config: Configuration = configLoader.get()
    private val enabled = config.getBoolean("reporter.enabled")

    // parameters
    private val failchatVersion = config.getString("version")
    private val trackingId = config.getString("reporter.tracking-id")
    private val languageTag = Locale.getDefault().toLanguageTag()
    private val clientId = getClientId()

    // User-Agent header
    private val userAgent = "failchat/$failchatVersion (${sp("os.name")} ${sp("os.arch")} ${sp("os.version")}) " +
            "java${sp("java.version")}"


    fun reportEvent(category: EventCategory, action: EventAction): CompletableFuture<Unit> {
        if (!enabled) {
            log.debug("Reporter disabled, event {}.{} ignored", category.name, action.name)
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
                .addQueryParameter("ec", category.name) //Event Category
                .addQueryParameter("ea", action.name) //Event Action
                .addQueryParameter("ul", languageTag) //User Language
                .addQueryParameter("an", "failchat") //Application Name
                .addQueryParameter("av", failchatVersion) //Application Version
                .addQueryParameter("ni", if (action == Heartbeat) "1" else "0") // Non-Interaction Hit
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
                    log.info("Event successfully reported: {}.{}", category.name, action.name)
                }
    }

    private fun getClientId(): String {
        val config = configLoader.get()

        var userId = config.getString(idKey)
        if (userId.isNullOrEmpty()) {
            userId = UUID.randomUUID().toString()
            config.setProperty(idKey, userId.toString())
            configLoader.save()
            log.info("User id generated: {}", userId)
        } else {
            log.info("User id loader from config: {}", userId)
        }

        return userId
    }

    /**
     * Shortcut for [System.getProperty].
     * */
    private fun sp(key: String): String? = System.getProperty(key)

}
