package failchat.emoticon

import java.util.regex.Pattern

object WordReplacer {

    val wordPattern: Pattern = Pattern.compile("""(?<=\s|^)(.+?)(?=\s|$)""")

    inline fun replace(initialString: String, decisionMaker: (word: String) -> ReplaceDecision): String {
        // Can't use Matcher.appendReplacement() because it resets position when Matcher.find(start) invoked
        val matcher = wordPattern.matcher(initialString)
        val sb = lazy(LazyThreadSafetyMode.NONE) { StringBuilder() }
        var cursor = 0

        while (matcher.find(cursor)) {
            val code = matcher.group(1)
            val decision = decisionMaker.invoke(code)

            val end = matcher.end()
            when (decision) {
                is ReplaceDecision.Replace -> {
                    val appendFrom = if (sb.isInitialized()) cursor else 0
                    sb.value.append(initialString, appendFrom, matcher.start())
                    sb.value.append(decision.replacement)
                }
                is ReplaceDecision.Skip -> {
                    if (sb.isInitialized()) {
                        sb.value.append(initialString, cursor, end)
                    }
                }
            }
            cursor = end
        }

        if (!sb.isInitialized()) return initialString

        sb.value.append(initialString, cursor, initialString.length)
        return sb.toString()
    }

}
