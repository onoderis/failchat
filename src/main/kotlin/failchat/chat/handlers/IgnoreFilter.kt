package failchat.chat.handlers

import failchat.Origin
import failchat.chat.ChatMessage
import failchat.chat.MessageFilter
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
                .map { it.commonName }
                .joinToString(separator = "|", prefix = "(", postfix = ")")

        banFormat = Pattern.compile(".+#" + originsPattern)

        reloadConfig()
    }

    override fun filterMessage(message: ChatMessage): Boolean {
        val ignoreMessage = atomicIgnoreSet.value.contains(message.author.id + "#" + message.origin.commonName)
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
