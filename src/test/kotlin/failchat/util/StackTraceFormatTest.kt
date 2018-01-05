package failchat.util

import org.junit.Ignore
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Ignore
class StackTraceFormatTest {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(StackTraceFormatTest::class.java)
    }

    @Test
    fun manualTest() {
        log.info(formatStackTraces(Thread.getAllStackTraces()))
    }

}
