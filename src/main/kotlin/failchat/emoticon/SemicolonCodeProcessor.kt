package failchat.emoticon

import java.util.regex.Pattern

object SemicolonCodeProcessor {

    private val emoticonCodePattern: Pattern = Pattern.compile("""(?<pre>:(?<code>[\w-]+)):""")

    fun process(initialString: String, decisionMaker: (code: String) -> ReplaceDecision): String {
        // Can't use Matcher.appendReplacement() because it resets position when Matcher.find(start) invoked
        val matcher = emoticonCodePattern.matcher(initialString)
        val sb = StringBuilder()
        var cursor = 0

        while (matcher.find(cursor)) {
            val code = matcher.group("code")
            val decision = decisionMaker.invoke(code)

            val end = matcher.end()
            when (decision) {
                is ReplaceDecision.Replace -> {
                    sb.append(initialString, cursor, matcher.start())
                    sb.append(decision.replacement)
                    cursor = end
                }
                is ReplaceDecision.Skip -> {
                    val lastSemicolonPosition = if (end > 0) end - 1 else end
                    sb.append(initialString, cursor, lastSemicolonPosition)
                    cursor = lastSemicolonPosition
                }
            }
        }

        sb.append(initialString, cursor, initialString.length)

        return sb.toString()
    }

}
