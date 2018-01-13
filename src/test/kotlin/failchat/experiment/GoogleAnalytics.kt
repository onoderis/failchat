package failchat.experiment

import failchat.reporter.EventAction
import failchat.reporter.EventCategory
import failchat.reporter.EventReporter
import failchat.reporter.GAEventReporter
import failchat.util.okHttpClient
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Ignore
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Ignore
class GoogleAnalytics {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GoogleAnalytics::class.java)
    }

    val reporter: EventReporter = GAEventReporter(okHttpClient, "test-id", "2.2.0", "UA-73079300-3")

    @Test
    fun testReport() {
        runBlocking {
            reporter.report(EventCategory.GENERAL, EventAction.APP_LAUNCH)
        }
    }

}
