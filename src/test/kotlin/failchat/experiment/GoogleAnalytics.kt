package failchat.experiment

import failchat.ConfigLoader
import failchat.reporter.EventAction
import failchat.reporter.EventCategory
import failchat.reporter.EventReporter
import okhttp3.OkHttpClient
import org.junit.Ignore
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Paths

@Ignore
class GoogleAnalytics {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GoogleAnalytics::class.java)
    }

    val reporter = EventReporter("test-id", OkHttpClient(), ConfigLoader(Paths.get("target/test-classes/config")))

    @Test
    fun testReport() {
        reporter.reportEvent(EventCategory.GENERAL, EventAction.APP_LAUNCH).join()
    }

}
