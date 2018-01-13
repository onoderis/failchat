package failchat.twitch

import com.google.common.collect.EvictingQueue
import failchat.Origin
import failchat.Origin.TWITCH
import failchat.chat.ChatClient
import failchat.chat.ChatClientStatus
import failchat.chat.MessageHandler
import failchat.chat.MessageIdGenerator
import failchat.chat.OriginStatus.CONNECTED
import failchat.chat.OriginStatus.DISCONNECTED
import failchat.chat.StatusMessage
import failchat.chat.handlers.BraceEscaper
import failchat.chat.handlers.ElementLabelEscaper
import failchat.emoticon.EmoticonFinder
import failchat.util.notEmptyOrNull
import failchat.util.synchronized
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.ActionEvent
import org.pircbotx.hooks.events.ConnectEvent
import org.pircbotx.hooks.events.DisconnectEvent
import org.pircbotx.hooks.events.ListenerExceptionEvent
import org.pircbotx.hooks.events.MessageEvent
import org.pircbotx.hooks.events.UnknownEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import java.util.regex.Pattern
import kotlin.concurrent.thread

class TwitchChatClient(
        private val userName: String,
        ircAddress: String,
        ircPort: Int,
        botName: String,
        botPassword: String,
        emoticonFinder: EmoticonFinder,
        private val messageIdGenerator: MessageIdGenerator,
        private val bttvEmoticonHandler: BttvEmoticonHandler
) : ChatClient<TwitchMessage> {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(TwitchChatClient::class.java)
        val reconnectTimeout: Duration = Duration.ofSeconds(10)
        val banMessagePattern: Pattern = Pattern.compile("""^:tmi\.twitch\.tv CLEARCHAT #.+ :(.+)""")
    }

    override val origin = Origin.TWITCH
    override val status: ChatClientStatus get() = atomicStatus.get()

    override var onChatMessage: ((TwitchMessage) -> Unit)? = null
    override var onStatusMessage: ((StatusMessage) -> Unit)? = null
    override var onChatMessageDeleted: ((TwitchMessage) -> Unit)? = null


    private val twitchIrcClient: PircBotX
    private val serverEntries = listOf(Configuration.ServerEntry(ircAddress, ircPort))
    private val atomicStatus: AtomicReference<ChatClientStatus> = AtomicReference(ChatClientStatus.READY)
    private val messageHandlers: List<MessageHandler<TwitchMessage>> = listOf(
            ElementLabelEscaper(),
            TwitchEmoticonHandler(emoticonFinder),
            bttvEmoticonHandler,
            BraceEscaper(),
            TwitchHighlightHandler(userName)
    )
    private val history = EvictingQueue.create<TwitchMessage>(50).synchronized()


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
        val statusChanged = atomicStatus.compareAndSet(ChatClientStatus.READY, ChatClientStatus.CONNECTED)
        if (!statusChanged) throw IllegalStateException("Expected status: ${ChatClientStatus.READY}")

        thread(start = true, name = "TwitchIrcClientThread") {
            try {
                twitchIrcClient.startBot()
            } catch (e: Exception) {
                log.warn("Failed to start twitch irc client", e)
            }
        }
    }

    override fun stop() {
        atomicStatus.set(ChatClientStatus.OFFLINE)
        twitchIrcClient.close()
    }

    private inner class TwitchIrcListener : ListenerAdapter() {

        override fun onConnect(event: ConnectEvent) {
            log.info("Connected to irc channel: {}", userName)
            atomicStatus.set(ChatClientStatus.CONNECTED)
            twitchIrcClient.sendCAP().request("twitch.tv/tags")
            twitchIrcClient.sendCAP().request("twitch.tv/commands")
            onStatusMessage?.invoke(StatusMessage(TWITCH, CONNECTED))
        }

        override fun onDisconnect(event: DisconnectEvent) {
            when (atomicStatus.get()) {
                ChatClientStatus.OFFLINE,
                ChatClientStatus.ERROR -> return
                else -> {
                    atomicStatus.set(ChatClientStatus.CONNECTING)
                    log.info("Twitch irc client disconnected")
                    onStatusMessage?.invoke(StatusMessage(TWITCH, DISCONNECTED))
                }
            }
        }

        override fun onMessage(event: MessageEvent) {
            log.debug("twitch message {}", event.message)
            val message = parseMessage(event)
            messageHandlers.forEach { it.handleMessage(message) }
            history.add(message)
            onChatMessage?.invoke(message)
        }

        override fun onListenerException(event: ListenerExceptionEvent) {
            log.warn("Listener exception", event.exception)
        }

        /**
         * Handle "/me" messages.
         * */
        override fun onAction(event: ActionEvent) {
            val message = parseMessage(event)
            messageHandlers.forEach { it.handleMessage(message) }
            history.add(message)
            onChatMessage?.invoke(message)
        }

        override fun onUnknown(event: UnknownEvent) {
            log.debug("Unknown event: {}", event.line)
            val matcher = banMessagePattern.matcher(event.line)
            if (!matcher.find()) return

            val author = matcher.group(1)
            val messagesToDelete = history.filter { it.author.id.equals(author, ignoreCase = true) }

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
