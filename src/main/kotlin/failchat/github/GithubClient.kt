package failchat.github

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import failchat.exception.NoReleasesFoundException
import failchat.exception.UnexpectedResponseCodeException
import failchat.exception.UnexpectedResponseException
import failchat.util.isEmpty
import failchat.util.thenApplySafe
import failchat.util.toFuture
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class GithubClient(
        private val apiUrl: String,
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper
) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GithubClient::class.java)
    }

    fun requestLatestRelease(userName: String, repository: String): CompletableFuture<Release> {
        val request = Request.Builder()
                .url("${apiUrl.removeSuffix("/")}/repos/$userName/$repository/releases")
                .get()
                .build()

        return httpClient.newCall(request)
                .toFuture()
                .thenApplySafe {
                    if (it.code() != 200) throw UnexpectedResponseCodeException(it.code())
                    val responseBody = it.body() ?: throw UnexpectedResponseException("null body")
                    val releasesNode = objectMapper.readTree(responseBody.string())
                    findLatestRelease(releasesNode) ?: throw NoReleasesFoundException()
                }
    }

    private fun findLatestRelease(releasesNode: JsonNode): Release? {
        return releasesNode.asSequence()
                .filter { !it.get("draft").asBoolean() }
                .filter { !it.get("prerelease").asBoolean() }
                .filter { !it.get("assets").isEmpty() }
                .map { parseRelease(it) }
                .filterNotNull()
                .firstOrNull()
    }
    
    private fun parseRelease(releaseNode: JsonNode): Release? {
        return try {
            Release(
                    Version.parse(releaseNode.get("tag_name").asText()),
                    releaseNode.get("html_url").asText(),
                    releaseNode.get("assets").get(0).get("browser_download_url").asText()
            )
        } catch (e: Exception) {
            log.warn("Failed to parse release node, skip it", e)
            null
        }
    }

}
