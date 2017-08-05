package failchat.twitch

import com.google.common.collect.EvictingQueue
import failchat.core.Origin
import failchat.core.Origin.twitch
import failchat.core.chat.ChatClient
import failchat.core.chat.ChatClientStatus
import failchat.core.chat.MessageHandler
import failchat.core.chat.MessageIdGenerator
import failchat.core.chat.OriginStatus.CONNECTED
import failchat.core.chat.OriginStatus.DISCONNECTED
import failchat.core.chat.StatusMessage
import failchat.core.chat.handlers.HtmlHandler
import failchat.core.chat.handlers.MessageObjectCleaner
import failchat.core.emoticon.EmoticonFinder
import failchat.util.notEmptyOrNull
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.ActionEvent
import org.pircbotx.hooks.events.ConnectEvent
import org.pircbotx.hooks.events.DisconnectEvent
import org.pircbotx.hooks.events.MessageEvent
import org.pircbotx.hooks.events.UnknownEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.time.Duration
import java.util.Queue
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import java.util.regex.Pattern
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

class TwitchChatClient(
        private val userName: String,
        ircAddress: String,
        ircPort: Int,
        botName: String,
        botPassword: String,
        emoticonFinder: EmoticonFinder,
        private val messageIdGenerator: MessageIdGenerator
) : ChatClient<TwitchMessage> {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(TwitchChatClient::class.java)
        val reconnectTimeout: Duration = Duration.ofSeconds(10)
        val banMessagePattern: Pattern = Pattern.compile(":tmi.twitch.tv CLEARCHAT #.+ :(.+)")
        const val historySize = 50
    }

    override val origin = Origin.twitch
    override val status: ChatClientStatus get() = atomicStatus.get()

    override var onChatMessage: ((TwitchMessage) -> Unit)? = null
    override var onStatusMessage: ((StatusMessage) -> Unit)? = null
    override var onChatMessageDeleted: ((TwitchMessage) -> Unit)? = null


    private val twitchIrcClient: PircBotX
    private val serverEntries = listOf(Configuration.ServerEntry(ircAddress, ircPort))
    private val atomicStatus: AtomicReference<ChatClientStatus> = AtomicReference(ChatClientStatus.ready)
    private val messageHandlers: List<MessageHandler<TwitchMessage>> = listOf(
            MessageObjectCleaner(), //todo fix bug: message with { and emoticons
            TwitchEmoticonHandler(emoticonFinder),
            HtmlHandler(),
            TwitchHighlightHandler(userName)
    )
    private val history: Queue<TwitchMessage> = EvictingQueue.create(historySize)
    private val historyLock: Lock = ReentrantLock()


    init {
        val configuration = Configuration.Builder()
                .setName(botName)
                .setServerPassword(botPassword)
                .setServers(serverEntries)
                .addAutoJoinChannel("#" + userName.toLowerCase())
                .addListener(TwitchIrcListener())
//                .addCapHandler() //todo try out
                .setAutoReconnect(false)
                .setAutoReconnectDelay(reconnectTimeout.toMillis().toInt())
                .setAutoReconnectAttempts(Int.MAX_VALUE) // bugged, 5 attempts todo retest
                .setEncoding(Charset.forName("UTF-8"))
                .setCapEnabled(false)
                .buildConfiguration()

        twitchIrcClient = PircBotX(configuration)
    }

    override fun start() {
        val statusChanged = atomicStatus.compareAndSet(ChatClientStatus.ready, ChatClientStatus.connected)
        if (!statusChanged) throw IllegalStateException("Expected status: ${ChatClientStatus.ready.name}")

        thread(start = true, name = "TwitchIrcClientThread") {
            try {
                twitchIrcClient.startBot()
            } catch (e: Exception) {
                log.warn("Failed to start twitch irc client", e)
            }
        }
    }

    override fun stop() {
        atomicStatus.set(ChatClientStatus.offline)
        twitchIrcClient.close()
    }

    private inner class TwitchIrcListener : ListenerAdapter() {

        override fun onConnect(event: ConnectEvent) {
            log.info("Connected to irc channel: {}", userName)
            atomicStatus.set(ChatClientStatus.connected)
            twitchIrcClient.sendCAP().request("twitch.tv/tags")
            twitchIrcClient.sendCAP().request("twitch.tv/commands")
            onStatusMessage?.invoke(StatusMessage(twitch, CONNECTED))
        }

        override fun onDisconnect(event: DisconnectEvent) {
            when (atomicStatus.get()) {
                ChatClientStatus.offline,
                ChatClientStatus.error -> return
                else -> {
                    atomicStatus.set(ChatClientStatus.connecting)
                    log.info("Twitch irc client disconnected")
                    onStatusMessage?.invoke(StatusMessage(twitch, DISCONNECTED))
                }
            }
        }

        override fun onMessage(event: MessageEvent) {
            val message = parseMessage(event)
            messageHandlers.forEach { it.handleMessage(message) }
            historyLock.withLock { history.add(message) }
            onChatMessage?.invoke(message)
        }

        /**
         * Handle "/me" messages.
         * */
        override fun onAction(event: ActionEvent) {
            val message = parseMessage(event)
            messageHandlers.forEach { it.handleMessage(message) }
            historyLock.withLock { history.add(message) }
            onChatMessage?.invoke(message)
        }

        override fun onUnknown(event: UnknownEvent) {
            val matcher = banMessagePattern.matcher(event.line)
            if (matcher.find()) return

            val author = matcher.group(1)
            val messagesToDelete = historyLock.withLock {
                history.filter { it.author.id.equals(author, ignoreCase = true) }
            }

            onChatMessageDeleted?.let { messagesToDelete.forEach(it) }
        }
    }

    private fun parseMessage(event: MessageEvent): TwitchMessage {
        val displayedName = event.v3Tags.get("display-name") //could return null (e.g. from twitchnotify)
        // Если пользователь не менял ник, то в v3tags пусто, ник capitalized
        val author: String = if (displayedName.isNullOrEmpty()) {
            event.userHostmask.nick.capitalize()
        } else {
            displayedName!!
        }

        return TwitchMessage(
                id = messageIdGenerator.generate(),
                author = author,
                text = event.message,
                emotesTag = event.v3Tags.get("emotes").notEmptyOrNull() //if message has no emoticons - tag is empty, not null
        )
    }

    private fun parseMessage(event: ActionEvent): TwitchMessage {
        return TwitchMessage(
                id = messageIdGenerator.generate(),
                author = event.userHostmask.nick,
                text = event.message,
                emotesTag = null
        )
    }

}
