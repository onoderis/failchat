package failchat.emoticon

import java.util.regex.Pattern

object WordReplacer {

    val wordPattern: Pattern = Pattern.compile("""\b(.+?)\b""")

    inline fun replace(initialString: String, decisionMaker: (word: String) -> ReplaceDecision): String {
        // Can't use Matcher.appendReplacement() because it resets position when Matcher.find(start) invoked
        val matcher = wordPattern.matcher(initialString)
        val sb = StringBuilder()
        var cursor = 0

        while (matcher.find(cursor)) {
            val code = matcher.group(1)
            val decision = decisionMaker.invoke(code)

            val end = matcher.end()
            when (decision) {
                is ReplaceDecision.Replace -> {
                    sb.append(initialString, cursor, matcher.start())
                    sb.append(decision.replacement)
                }
                is ReplaceDecision.Skip -> {
                    sb.append(initialString, cursor, end)
                }
            }
            cursor = end
        }

        sb.append(initialString, cursor, initialString.length)

        return sb.toString()
    }

}
