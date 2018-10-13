package failchat.chat.handlers

import failchat.Origin
import failchat.chat.Author
import failchat.chat.ChatMessage
import failchat.chat.MessageFilter
import failchat.chatOrigins
import failchat.util.value
import mu.KLogging
import org.apache.commons.configuration2.Configuration
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern

/**
 * Фильтрует сообщения от пользователей в игнор-листе.
 * Баны хранятся в формате 'authorId#origin (optionalAuthorName)'.
 */
class IgnoreFilter(private val config: Configuration) : MessageFilter<ChatMessage> {

    private companion object : KLogging()

    private val ignoreStringPattern: Pattern = compilePattern()

    private var ignoreSet: AtomicReference<Set<Author>> = AtomicReference(emptySet())

    init {
        reloadConfig()
    }

    override fun filterMessage(message: ChatMessage): Boolean {
        val ignoreMessage = ignoreSet.value.asSequence()
                .filter { it.id == message.author.id && it.origin == message.author.origin }
                .any()

        if (ignoreMessage) logger.debug { "Message filtered by ignore filter: $message" }
        return ignoreMessage
    }

    fun reloadConfig() {
        ignoreSet.value = config.getList("ignore").asSequence()
                .map { ignoreStringPattern.matcher(it as String) }
                .filter {
                    it.find().also { found ->
                        if (!found) logger.debug("Ignore entry skipped: '{}'", it)
                    }
                }
                .map {
                    val id = it.group("id")
                    val name = it.group("name") ?: id
                    Author(name, Origin.byCommonName(it.group("origin")), id)
                }
                .toSet()
        logger.debug("IgnoreFilter reloaded a config")
    }

    private fun compilePattern(): Pattern {
        val originsPattern = chatOrigins
                .map { it.commonName }
                .joinToString(separator = "|", prefix = "(", postfix = ")")

        return Pattern.compile("""(?<id>.+)#(?<origin>$originsPattern)( \\((?<name>.*)\\))?""")
    }

}
