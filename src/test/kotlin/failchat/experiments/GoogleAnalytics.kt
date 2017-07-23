package failchat.experiments

import failchat.core.ConfigLoader
import failchat.core.reporter.EventAction
import failchat.core.reporter.EventCategory
import failchat.core.reporter.EventReporter
import okhttp3.OkHttpClient
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Paths

class GoogleAnalytics {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GoogleAnalytics::class.java)
    }

    val reporter = EventReporter(OkHttpClient(), ConfigLoader(Paths.get("target/test-classes/config")))

    @Test
    fun testReport() {
        reporter.reportEvent(EventCategory.general, EventAction.start).join()
    }

}