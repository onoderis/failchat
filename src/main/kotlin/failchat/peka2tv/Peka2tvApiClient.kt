package failchat.peka2tv

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import failchat.exceptions.UnexpectedResponseCodeException
import failchat.exceptions.UnexpectedResponseException
import failchat.utils.emptyBody
import failchat.utils.jsonMediaType
import failchat.utils.toFuture
import failchat.utils.withSuffix
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class Peka2tvApiClient(
        private val httpClient: OkHttpClient,
        apiUrl: String,
        private val objectMapper: ObjectMapper = ObjectMapper()
) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(Peka2tvApiClient::class.java)
    }

    private val apiUrl = apiUrl.withSuffix("/")

    fun findUser(name: String): CompletableFuture<Peka2tvUser> {
        val requestBody = objectMapper.createObjectNode()
                .put("name", name)
        return request("/user", requestBody)
                .thenApply { Peka2tvUser(name, it.get("id").asLong()) }
    }

    fun request(path: String, body: JsonNode? = null): CompletableFuture<JsonNode> {

        val request = Request.Builder()
                .url(apiUrl + path.removePrefix("/"))
                .post(body?.toRequestBody() ?: emptyBody)
                .header("User-Agent", "failchat client")
                .header("Accept", "application/json; version=1.0")
                .build()

        return httpClient.newCall(request)
                .toFuture()
                .thenApply {
                    if (it.code() != 200) throw UnexpectedResponseCodeException(it.code())
                    val responseBody = it.body() ?: throw UnexpectedResponseException("null body")
                    responseBody.use {
                        return@thenApply objectMapper.readTree(it.string())
                    }
                }
    }


    private fun JsonNode.toRequestBody() = RequestBody.create(jsonMediaType, this.toString())

}
