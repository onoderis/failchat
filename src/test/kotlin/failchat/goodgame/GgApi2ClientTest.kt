package failchat.goodgame

import failchat.exception.ChannelOfflineException
import failchat.okHttpClient
import failchat.testObjectMapper
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import kotlin.test.Test

class GgApi2ClientTest {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    private val client = GgApi2Client(okHttpClient, testObjectMapper)

    @Test
    fun requestViewersCountTest() = runBlocking<Unit> {
        try {
            val count = client.requestViewersCount("Fotos")
            logger.debug("gg viewers count: {}", count)
        } catch (ignored: ChannelOfflineException) {
            logger.debug("gg channel is offline")
        }
    }

}
