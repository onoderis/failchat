package failchat.youtube2

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.regex.Pattern

class YoutubeHtmlParser(
        private val objectMapper: ObjectMapper
) {

    private companion object {
        /** Most common format, should work for the most streams. */
        val innertubeContextPattern = Pattern.compile("""ytcfg\.set\("INNERTUBE_CONTEXT", (.+)\)\R""")!!
        /** This format might occur on streams without a recording. */
        val firstConfigStatementPattern = Pattern.compile("""ytcfg\.set\((\{.+\})\);""")!!
        val youtubeConfigPattern = Pattern.compile("""ytcfg\.set\((.+)\);\n""")!!
        val youtubeInitialDataPattern = Pattern.compile("""window\["ytInitialData"\] = (.+);\n""")!!
    }

    fun extractSessionId(pageHtml: String): String {
        return findSessionIdInInnertubeContextStatementOrNull(pageHtml)
                ?: findSessionIdInFirstConfigStatementNull(pageHtml)
                ?: throw YoutubeClientException("No sessionId in innertube context")
    }

    private fun findSessionIdInInnertubeContextStatementOrNull(pageHtml: String): String? {
        val matcher = innertubeContextPattern.matcher(pageHtml)
        val found = matcher.find()
        if (!found) {
            return null
        }
        val innertubeContext = objectMapper.readTree(matcher.group(1))
        return innertubeContext
                .get("request")
                ?.get("sessionId")
                ?.textValue()
    }

    private fun findSessionIdInFirstConfigStatementNull(pageHtml: String): String? {
        val matcher = firstConfigStatementPattern.matcher(pageHtml)
        val found = matcher.find()
        if (!found) {
            return null
        }
        val innertubeContext = objectMapper.readTree(matcher.group(1))
        return innertubeContext
                .get("INNERTUBE_CONTEXT")
                ?.get("request")
                ?.get("sessionId")
                ?.asText()
    }

    fun extractInnertubeApiKey(pageHtml: String): String {
        val matcher = youtubeConfigPattern.matcher(pageHtml)
        val found = matcher.find()
        if (!found) {
            throw YoutubeClientException("Failed to extract youtube main config from html page")
        }
        val youtubeConfig = try {
            objectMapper.readTree(matcher.group(1))
        } catch (e: JsonParseException) {
            throw YoutubeClientException("Failed to parse youtube main config as json", e)
        }

        return youtubeConfig.get("INNERTUBE_API_KEY")?.textValue()
                ?: throw YoutubeClientException("No INNERTUBE_API_KEY in youtube main config")
    }

    fun parseInitialData(pageHtml: String): JsonNode {
        val matcher = youtubeInitialDataPattern.matcher(pageHtml)
        val found = matcher.find()
        if (!found) {
            throw YoutubeClientException("Failed to extract youtube initial data from html page")
        }
        return objectMapper.readTree(matcher.group(1))
    }

}
