package failchat.core.chat.handlers

import failchat.core.Origin
import failchat.core.chat.ChatMessage
import failchat.core.chat.MessageFilter
import failchat.util.debug
import failchat.util.value
import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern

/**
 * Фильтрует сообщения от пользователей в игнор-листе.
 * Баны хранятся в формате authorId#origin.
 */
class IgnoreFilter(private val config: Configuration) : MessageFilter<ChatMessage> {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(IgnoreFilter::class.java)
    }

    private val banFormat: Pattern

    private var atomicIgnoreSet: AtomicReference<Set<String>> = AtomicReference(emptySet())

    init {
        val originsPattern = Origin.values()
                .map { it.name }
                .joinToString(separator = "|", prefix = "(", postfix = ")")

        banFormat = Pattern.compile(".+#" + originsPattern)

        reloadConfig()
    }

    override fun filterMessage(message: ChatMessage): Boolean {
        val ignoreMessage = atomicIgnoreSet.value.contains(message.author.id + "#" + message.origin.name)
        if (ignoreMessage) log.debug { "Message filtered by ignore filter: $message" }
        return ignoreMessage
    }

    fun reloadConfig() {
        atomicIgnoreSet.value = config.getList("ignore")
                .map { it as String }
                .filter { banFormat.matcher(it).find() }
                .toSet()
        log.debug("IgnoreFilter reloaded a config")
    }

}
