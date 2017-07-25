package failchat.core

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.factory
import com.github.salomonbrys.kodein.instance
import failchat.core.AppState.chat
import failchat.core.AppState.settings
import failchat.core.chat.ChatClient
import failchat.core.chat.ChatMessageRemover
import failchat.core.chat.ChatMessageSender
import failchat.core.chat.MessageIdGenerator
import failchat.core.chat.handlers.IgnoreFilter
import failchat.core.chat.handlers.ImageLinkHandler
import failchat.core.viewers.ViewersCountLoader
import failchat.core.viewers.ViewersCountWsHandler
import failchat.core.viewers.ViewersCounter
import failchat.core.ws.server.WsServer
import failchat.exceptions.InvalidConfigurationException
import failchat.goodgame.GgApiClient
import failchat.goodgame.GgChatClient
import failchat.peka2tv.Peka2tvApiClient
import failchat.peka2tv.Peka2tvChatClient
import failchat.twitch.TwitchChatClient
import failchat.twitch.TwitchViewersCountLoader
import failchat.utils.error
import failchat.utils.formatStackTraces
import failchat.utils.ls
import failchat.utils.sleep
import javafx.application.Platform
import okhttp3.OkHttpClient
import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

//todo refactor
class AppStateTransitionManager(private val kodein: Kodein) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(AppStateTransitionManager::class.java)
        val shutdownTimeout: Duration = Duration.ofSeconds(20)
    }

    private val wsServer: WsServer = kodein.instance()
    private val messageIdGenerator: MessageIdGenerator = kodein.instance()
    private val chatMessageSender: ChatMessageSender = kodein.instance()
    private val chatMessageRemover: ChatMessageRemover = kodein.instance()
    private val peka2tvApiClient: Peka2tvApiClient = kodein.instance()
    private val goodgameApiClient: GgApiClient = kodein.instance()
    private val configLoader: ConfigLoader = kodein.instance()
    private val ignoreFilter: IgnoreFilter = kodein.instance()
    private val imageLinkHandler: ImageLinkHandler = kodein.instance()
    private val okHttpClient: OkHttpClient = kodein.instance()
    private val viewersCountWsHandler: ViewersCountWsHandler = kodein.instance()

    private val lock: Lock = ReentrantLock()
    private val config: Configuration = configLoader.get()

    private var chatClients: Map<Origin, ChatClient<*>> = emptyMap()
    private var viewersCounter: ViewersCounter? = null
    
    private var state: AppState = settings

    fun startChat() = lock.withLock {
        if (state != settings) IllegalStateException("Expected: $settings, actual: $state")

        val viewersCountLoaders: MutableList<ViewersCountLoader> = ArrayList()
        val chatClientMap: MutableMap<Origin, ChatClient<*>> = HashMap() //todo rename

        // Peka2tv chat client initialization
        checkEnabled(Origin.peka2tv)?.let { channelName ->
            // get channel id by channel name
            val channelId = try {
                peka2tvApiClient.findUser(channelName).join().id
            } catch (e: Exception) {
                log.warn("Failed to get peka2tv channel id. channel name: {}", channelName, e)
                return@let
            }

            val chatClient = kodein.factory<Pair<String, Long>, Peka2tvChatClient>()
                    .invoke(channelName to channelId)
                    .also { it.setCallbacks() }

            chatClientMap.put(Origin.peka2tv, chatClient)
            viewersCountLoaders.add(chatClient)
        }

        // Twitch
        checkEnabled(Origin.twitch)?.let { channelName ->
            val chatClient = kodein.factory<String, TwitchChatClient>()
                    .invoke(channelName)
                    .also { it.setCallbacks() }
            chatClientMap.put(Origin.twitch, chatClient)
            viewersCountLoaders.add(kodein.factory<String, TwitchViewersCountLoader>().invoke(channelName))
        }


        //Goodgame
        checkEnabled(Origin.goodgame)?.let { channelName ->
            // get channel id by channel name
            val channelId = try {
                goodgameApiClient.requestChannelId(channelName).join()
            } catch (e: Exception) {
                log.warn("Failed to get goodgame channel id. channel name: {}", channelName, e)
                return@let
            }

            val chatClient = kodein.factory<Pair<String, Long>, GgChatClient>()
                    .invoke(channelName to channelId)
                    .also { it.setCallbacks() }

            chatClientMap.put(Origin.goodgame, chatClient)
            viewersCountLoaders.add(chatClient)
        }


        ignoreFilter.reloadConfig()
        imageLinkHandler.reloadConfig()

        //todo try catch
        // Start chat clients
        chatClientMap.values.forEach { it.start() }
        chatClients = chatClientMap

        //todo try catch
        // Start viewers counter
        viewersCounter = kodein
                .factory<List<ViewersCountLoader>, ViewersCounter>()
                .invoke(viewersCountLoaders)
                .apply { start() }

        viewersCountWsHandler.viewersCounter = viewersCounter
    }

    fun stopChat() = lock.withLock {
        if (state != chat) IllegalStateException("Expected: $chat, actual: $state")
        reset()
    }

    fun shutDown() = lock.withLock {
        log.info("Shutting down")
        reset()

        // Запуск в отдельном треде чтобы javafx thread мог завершиться и GUI закрывался сразу
        thread(start = true, name = "ShutdownThread") {
            config.setProperty("lastId", messageIdGenerator.lastId)
            configLoader.save()

            wsServer.stop()
            log.info("Websocket server stopped")

            okHttpClient.dispatcher().executorService().shutdown()
            log.info("OkHttpClient thread pool shutdown")
            okHttpClient.connectionPool().evictAll()
            log.info("OkHttpClient connections evicted")
        }

        thread(start = true, name = "TerminationThread", isDaemon = true) {
            sleep(shutdownTimeout)

            log.error {
                "Process terminated after ${shutdownTimeout.seconds} seconds of shutDown() call. Verbose information:$ls" +
                        formatStackTraces(Thread.getAllStackTraces())
            }
            System.exit(10)
        }

        Platform.exit()
    }

    private fun ChatClient<*>.setCallbacks() {
        onChatMessage { chatMessageSender.send(it) }
        onInfoMessage { chatMessageSender.send(it) }
        onChatMessageDeleted { chatMessageRemover.remove(it) }
    }

    private fun reset() {
        viewersCountWsHandler.viewersCounter = null

        chatClients.values.forEach { it.stop() }
        // Значение может быть null если вызваны shutDown() и stopChat() последовательно, в любой последовательности,
        // либо если приложение было закрыто без запуска чата.
        viewersCounter?.stop()
    }

    /**
     * @return channel name if chat client should be started, null otherwise.
     * */
    private fun checkEnabled(origin: Origin): String? {
        if (!config.getBoolean("${origin.name}.enabled")) return null

        val channel = config.getString("${origin.name}.channel")
                ?: throw InvalidConfigurationException("Channel is null. Origin: $origin")
        if (channel.isEmpty()) return null

        return channel
    }

}
