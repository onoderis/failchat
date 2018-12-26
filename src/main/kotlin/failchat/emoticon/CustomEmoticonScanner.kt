package failchat.emoticon

import failchat.chat.ImageFormat.RASTER
import failchat.chat.ImageFormat.VECTOR
import failchat.util.filterNotNull
import failchat.util.withSuffix
import mu.KLogging
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.regex.Pattern
import java.util.stream.Collectors

class CustomEmoticonScanner(
        private val emoticonsDirectory: Path,
        locationUrlPrefix: String
) {

    private val locationUrlPrefix = locationUrlPrefix.withSuffix("/")

    private companion object : KLogging() {
        val fileNamePattern: Pattern = Pattern.compile("""(?<code>.+)\.(?<format>jpe?g|png|gif|svg)$""", Pattern.CASE_INSENSITIVE)
    }

    fun scan(): List<Emoticon> {
        val t1 = Instant.now()
        val emoticons = Files.list(emoticonsDirectory)
                .map { it.fileName.toString() }
                .map { fileName ->
                    val m = fileNamePattern.matcher(fileName)
                    if (!m.matches()) {
                        logger.warn("Incorrect custom emoticon file was ignored: '{}'", fileName)
                        return@map null
                    }

                    Triple(fileName, m.group("code").toLowerCase(), m.group("format").toLowerCase())
                }
                .filterNotNull()
                .map { (fileName, code, formatStr) ->
                    val format = when (formatStr) {
                        "svg" -> VECTOR
                        else -> RASTER
                    }
                    CustomEmoticon(code, format, locationUrlPrefix + fileName)
                }
                .collect(Collectors.toList())

        val t2 = Instant.now()
        logger.debug { "Custom emoticons was scanned in ${Duration.between(t1, t2).toMillis()} ms" }

        return emoticons
    }

}
