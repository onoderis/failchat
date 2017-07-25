package failchat.core.chat.handlers

import failchat.core.Origin
import failchat.core.chat.ChatMessage
import failchat.core.chat.MessageFilter
import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.regex.Pattern

/**
 * Фильтрует сообщения от пользователей в игнор-листе
 * Баны хранятся в формате username#origin
 */
class IgnoreFilter(private val config: Configuration) : MessageFilter<ChatMessage> {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(IgnoreFilter::class.java)
    }

    private val banFormat: Pattern

    private var ignoreSet: Set<String> = emptySet()

    init {
        val originsPattern = Origin.values()
                .map { it.name }
                .joinToString(separator = "|", prefix = "(", postfix = ")")

        banFormat = Pattern.compile(".+#" + originsPattern)

        reloadConfig()
    }

    override fun filterMessage(message: ChatMessage): Boolean {
        return ignoreSet.contains(message.author + "#" + message.origin.name)
    }

    fun reloadConfig() {
        ignoreSet = config.getList("ignore")
                .map { it as String }
                .filter { banFormat.matcher(it).find() }
                .toSet()
    }

}
