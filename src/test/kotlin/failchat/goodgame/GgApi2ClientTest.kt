package failchat.goodgame

import failchat.config
import failchat.okHttpClient
import failchat.testObjectMapper
import kotlinx.coroutines.runBlocking

class GgApi2ClientTest {

    private val client = GgApi2Client(okHttpClient, testObjectMapper, config.getString("goodgame.api2-url"))

//    todo
//    @Test
    fun requestViewersCountTest() = runBlocking<Unit> {
        val count = client.requestViewersCount("Miker")
        println(count)
    }

}
