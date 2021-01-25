package failchat.youtube

import java.util.regex.Pattern

class YoutubeViewCountParser {

    private companion object {
        val viewCountPattern = Pattern.compile("""(.+) watching now""")!!
    }

    /**
     * @throws IllegalArgumentException on parse error.
     * */
    fun parse(viewCount: String): Int {
        val m = viewCountPattern.matcher(viewCount)
        require(m.matches()) { "Unexpected view count string: $viewCount" }

        val countWithCommas = m.group(1)
        return countWithCommas.replace(",", "").toInt()
    }

}
