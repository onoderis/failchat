package failchat.github

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import failchat.exception.UnexpectedResponseCodeException
import failchat.exception.UnexpectedResponseException
import failchat.util.thenUse
import failchat.util.toFuture
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.CompletableFuture

class GithubClient(
        private val apiUrl: String,
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper
) {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    fun requestLatestRelease(userName: String, repository: String): CompletableFuture<Release> {
        val request = Request.Builder()
                .url("${apiUrl.removeSuffix("/")}/repos/$userName/$repository/releases")
                .get()
                .build()

        return httpClient.newCall(request)
                .toFuture()
                .thenUse {
                    if (it.code != 200) throw UnexpectedResponseCodeException(it.code)
                    val responseBody = it.body ?: throw UnexpectedResponseException("null body")
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
            logger.warn("Failed to parse release node, skip it", e)
            null
        }
    }

}
