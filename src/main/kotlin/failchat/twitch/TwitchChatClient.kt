package failchat.twitch

import failchat.Origin
import failchat.Origin.TWITCH
import failchat.chat.ChatClient
import failchat.chat.ChatClientCallbacks
import failchat.chat.ChatClientStatus
import failchat.chat.ChatMessage
import failchat.chat.ChatMessageHistory
import failchat.chat.MessageHandler
import failchat.chat.MessageIdGenerator
import failchat.chat.OriginStatus.CONNECTED
import failchat.chat.OriginStatus.DISCONNECTED
import failchat.chat.StatusUpdate
import failchat.chat.findTyped
import failchat.chat.handlers.BraceEscaper
import failchat.chat.handlers.ElementLabelEscaper
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.pircbotx.Configuration
import org.pircbotx.PircBotX
import org.pircbotx.hooks.ListenerAdapter
import org.pircbotx.hooks.events.ActionEvent
import org.pircbotx.hooks.events.ConnectEvent
import org.pircbotx.hooks.events.DisconnectEvent
import org.pircbotx.hooks.events.ListenerExceptionEvent
import org.pircbotx.hooks.events.MessageEvent
import org.pircbotx.hooks.events.UnknownEvent
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
        twitchEmoticonHandler: TwitchEmoticonHandler,
        private val messageIdGenerator: MessageIdGenerator,
        bttvEmoticonHandler: BttvEmoticonHandler,
        ffzEmoticonHandler: FfzEmoticonHandler,
        sevenTvGlobalEmoticonHandler: MessageHandler<ChatMessage>,
        sevenTvChannelEmoticonHandler: MessageHandler<ChatMessage>,
        twitchBadgeHandler: TwitchBadgeHandler,
        private val history: ChatMessageHistory,
        override val callbacks: ChatClientCallbacks
) : ChatClient {

    private companion object : KLogging() {
        val reconnectTimeout: Duration = Duration.ofSeconds(10)
        val banMessagePattern: Pattern = Pattern.compile("""^:tmi\.twitch\.tv CLEARCHAT #.+ :(.+)""")
    }

    override val origin = Origin.TWITCH
    override val status: ChatClientStatus get() = atomicStatus.get()


    private val twitchIrcClient: PircBotX
    private val serverEntries = listOf(Configuration.ServerEntry(ircAddress, ircPort))
    private val atomicStatus: AtomicReference<ChatClientStatus> = AtomicReference(ChatClientStatus.READY)
    private val messageHandlers: List<MessageHandler<TwitchMessage>> = listOf(
            ElementLabelEscaper(),
            twitchEmoticonHandler,
            bttvEmoticonHandler,
            ffzEmoticonHandler,
            sevenTvGlobalEmoticonHandler,
            sevenTvChannelEmoticonHandler,
            BraceEscaper(),
            TwitchHighlightHandler(userName),
            TwitchRewardHandler(),
            TwitchHighlightByPointsHandler(),
            twitchBadgeHandler,
            TwitchAuthorColorHandler()
    )


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
                logger.warn("Failed to start twitch irc client", e)
            }
        }
    }

    override fun stop() {
        atomicStatus.set(ChatClientStatus.OFFLINE)
        twitchIrcClient.close()
    }

    private inner class TwitchIrcListener : ListenerAdapter() {

        override fun onConnect(event: ConnectEvent) {
            logger.info("Connected to irc channel: {}", userName)
            atomicStatus.set(ChatClientStatus.CONNECTED)
            twitchIrcClient.sendCAP().request("twitch.tv/tags")
            twitchIrcClient.sendCAP().request("twitch.tv/commands")
            callbacks.onStatusUpdate(StatusUpdate(TWITCH, CONNECTED))
        }

        override fun onDisconnect(event: DisconnectEvent) {
            when (atomicStatus.get()) {
                ChatClientStatus.OFFLINE,
                ChatClientStatus.ERROR -> return
                else -> {
                    atomicStatus.set(ChatClientStatus.CONNECTING)
                    logger.info("Twitch irc client disconnected")
                    callbacks.onStatusUpdate(StatusUpdate(TWITCH, DISCONNECTED))
                }
            }
        }

        override fun onMessage(event: MessageEvent) {
            logger.debug {
                "Message was received from twitch. ${event.user}. Message: '${event.message}'. Tags: '${event.v3Tags}'"
            }

            val message = parseOrdinaryMessage(event)
            messageHandlers.forEach { it.handleMessage(message) }
            callbacks.onChatMessage(message)
        }

        override fun onListenerException(event: ListenerExceptionEvent) {
            logger.warn("Listener exception", event.exception)
        }

        /**
         * Handle "/me" messages.
         * */
        override fun onAction(event: ActionEvent) {
            val message = parseMeMessage(event)
            messageHandlers.forEach { it.handleMessage(message) }
            callbacks.onChatMessage(message)
        }

        override fun onUnknown(event: UnknownEvent) {
            logger.debug("Unknown event: {}", event.line)
            val matcher = banMessagePattern.matcher(event.line)
            if (!matcher.find()) return

            val author = matcher.group(1)
            val messagesToDelete = runBlocking {
                history.findTyped<TwitchMessage> { it.author.id.equals(author, ignoreCase = true) }
            }

            messagesToDelete.forEach {
                callbacks.onChatMessageDeleted(it)
            }
        }
    }

    private fun parseOrdinaryMessage(event: MessageEvent): TwitchMessage {
        val displayedName = event.v3Tags.get(TwitchIrcTags.displayName) //could return null (e.g. from twitchnotify)
        // Если пользователь не менял ник, то в v3tags пусто, ник capitalized
        val author: String = if (displayedName.isNullOrEmpty()) {
            event.userHostmask.nick.capitalize()
        } else {
            displayedName
        }

        return TwitchMessage(
                id = messageIdGenerator.generate(),
                author = author,
                text = event.message,
                tags = event.v3Tags
        )
    }

    private fun parseMeMessage(event: ActionEvent): TwitchMessage {
        // todo emoticons, color
        return TwitchMessage(
                id = messageIdGenerator.generate(),
                author = event.userHostmask.nick,
                text = event.message,
                tags = mapOf()
        )
    }

}
