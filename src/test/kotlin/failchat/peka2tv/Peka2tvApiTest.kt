package failchat.peka2tv

import failchat.config
import failchat.okHttpClient
import org.junit.Test
import kotlin.test.assertTrue

class Peka2tvApiTest {

    private val apiClient = Peka2tvApiClient(okHttpClient, config.getString("peka2tv.api-url"))

    @Test
    fun emoticonApiTest() {
        apiClient
                .request("/smile")
                .thenAccept {
                    it.forEach { emoticonNode ->
                        assertTrue { emoticonNode.get("code").isTextual }
                        assertTrue { emoticonNode.get("url").isTextual }
                    }
                }
                .join()
    }

    @Test
    fun userApiTest() {
        apiClient
                .findUser("akudji")
                .join()
    }

}
