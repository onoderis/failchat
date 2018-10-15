package failchat.emoticon

import failchat.Origin.FAILCHAT
import failchat.chat.ImageFormat
import failchat.chat.ImageFormat.RASTER
import failchat.chat.ImageFormat.VECTOR
import failchat.util.filterNotNull
import failchat.util.withSuffix
import mu.KLogging
import java.nio.file.Files
import java.nio.file.Path
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

    fun scan(): Map<String, Emoticon> {
        return Files.list(emoticonsDirectory)
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
                .collect(Collectors.toMap({ it.code }, { it }))
    }

    private class CustomEmoticon(
            code: String,
            format: ImageFormat,
            override val url: String
    ) : Emoticon(FAILCHAT, code, format)
}
