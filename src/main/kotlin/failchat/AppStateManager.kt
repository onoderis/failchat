package failchat

import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.factory
import com.github.salomonbrys.kodein.instance
import either.Either
import failchat.AppState.CHAT
import failchat.AppState.SETTINGS
import failchat.Origin.BTTV_CHANNEL
import failchat.Origin.CYBERGAME
import failchat.Origin.GOODGAME
import failchat.Origin.PEKA2TV
import failchat.Origin.TWITCH
import failchat.Origin.YOUTUBE
import failchat.chat.ChatClient
import failchat.chat.ChatMessageRemover
import failchat.chat.ChatMessageSender
import failchat.chat.MessageIdGenerator
import failchat.chat.handlers.IgnoreFilter
import failchat.chat.handlers.ImageLinkHandler
import failchat.cybergame.CgApiClient
import failchat.cybergame.CgChatClient
import failchat.cybergame.CgViewersCountLoader
import failchat.emoticon.EmoticonStorage
import failchat.exception.InvalidConfigurationException
import failchat.goodgame.GgApiClient
import failchat.goodgame.GgChatClient
import failchat.peka2tv.Peka2tvApiClient
import failchat.peka2tv.Peka2tvChatClient
import failchat.twitch.BttvApiClient
import failchat.twitch.BttvChannelNotFoundException
import failchat.twitch.BttvEmoticonHandler
import failchat.twitch.TwitchChatClient
import failchat.twitch.TwitchViewersCountLoader
import failchat.util.completionCause
import failchat.util.error
import failchat.util.formatStackTraces
import failchat.util.hotspotThreads
import failchat.util.logException
import failchat.util.ls
import failchat.util.sleep
import failchat.viewers.ViewersCountLoader
import failchat.viewers.ViewersCountWsHandler
import failchat.viewers.ViewersCounter
import failchat.ws.server.WsServer
import failchat.youtube.ChannelId
import failchat.youtube.VideoId
import failchat.youtube.YoutubeUtils
import failchat.youtube.YtChatClient
import javafx.application.Platform
import kotlinx.coroutines.experimental.runBlocking
import okhttp3.OkHttpClient
import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

class AppStateManager(private val kodein: Kodein) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(AppStateManager::class.java)
        val shutdownTimeout: Duration = Duration.ofMillis(3500)
    }

    private val wsServer: WsServer = kodein.instance()
    private val messageIdGenerator: MessageIdGenerator = kodein.instance()
    private val chatMessageSender: ChatMessageSender = kodein.instance()
    private val chatMessageRemover: ChatMessageRemover = kodein.instance()
    private val peka2tvApiClient: Peka2tvApiClient = kodein.instance()
    private val goodgameApiClient: GgApiClient = kodein.instance()
    private val cybergameApiClient: CgApiClient = kodein.instance()
    private val configLoader: ConfigLoader = kodein.instance()
    private val ignoreFilter: IgnoreFilter = kodein.instance()
    private val imageLinkHandler: ImageLinkHandler = kodein.instance()
    private val okHttpClient: OkHttpClient = kodein.instance()
    private val viewersCountWsHandler: ViewersCountWsHandler = kodein.instance()
    private val youtubeExecutor: ScheduledExecutorService = kodein.instance("youtube")
    private val bttvEmoticonHandler: BttvEmoticonHandler = kodein.instance()
    private val bttvApiClient: BttvApiClient = kodein.instance()
    private val emoticonStorage: EmoticonStorage = kodein.instance()

    private val lock: Lock = ReentrantLock()
    private val config: Configuration = configLoader.get()

    private var chatClients: Map<Origin, ChatClient<*>> = emptyMap()
    private var viewersCounter: ViewersCounter? = null
    
    private var state: AppState = SETTINGS

    fun startChat() = lock.withLock {
        if (state != SETTINGS) IllegalStateException("Expected: $SETTINGS, actual: $state")

        val viewersCountLoaders: MutableList<ViewersCountLoader> = ArrayList()
        val initializedChatClients: MutableMap<Origin, ChatClient<*>> = HashMap()

        // Peka2tv chat client initialization
        checkEnabled(PEKA2TV)?.let { channelName ->
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

            initializedChatClients.put(PEKA2TV, chatClient)
            viewersCountLoaders.add(chatClient)
        }

        // Twitch
        checkEnabled(TWITCH)?.let { channelName ->
            val chatClient = kodein.factory<String, TwitchChatClient>()
                    .invoke(channelName)
                    .also { it.setCallbacks() }
            initializedChatClients.put(TWITCH, chatClient)
            viewersCountLoaders.add(kodein.factory<String, TwitchViewersCountLoader>().invoke(channelName))

            // load BTTV channel emoticons in background
            bttvApiClient.loadChannelEmoticons(channelName)
                    .thenApply<Unit> { emoticons ->
                        emoticonStorage.putCodeMapping(BTTV_CHANNEL, emoticons.map { it.code.toLowerCase() to it }.toMap())
                        emoticonStorage.putList(BTTV_CHANNEL, emoticons)
                        log.info("BTTV emoticons loaded for channel '{}', count: {}", channelName, emoticons.size)
                    }
                    .exceptionally { t ->
                        val completionCause = t.completionCause()
                        if (completionCause is BttvChannelNotFoundException) {
                            log.info("BTTV emoticons not found for channel '{}'", completionCause.channel)
                        } else {
                            log.error("Failed to load BTTV emoticons for channel '{}'", channelName, completionCause)
                        }
                    }
                    .logException()
        }


        // Goodgame
        checkEnabled(GOODGAME)?.let { channelName ->
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

            initializedChatClients.put(GOODGAME, chatClient)
            viewersCountLoaders.add(chatClient)
        }


        // Youtube
        checkEnabled(YOUTUBE)?.let { channelIdOrVideoId ->
            val eitherId = YoutubeUtils.determineId(channelIdOrVideoId)
            val chatClient = kodein.factory<Either<ChannelId, VideoId>, YtChatClient>()
                    .invoke(eitherId)
                    .also { it.setCallbacks() }

            initializedChatClients.put(YOUTUBE, chatClient)
            viewersCountLoaders.add(chatClient)
        }

        // Cybergame
        checkEnabled(CYBERGAME)?.let { channelName ->
            // get channel id by channel name
            val channelId = try {
                runBlocking { cybergameApiClient.requestChannelId(channelName) }
            } catch (e: Exception) {
                log.warn("Failed to get cybergame channel id. channel name: {}", channelName, e)
                return@let
            }

            val chatClient = kodein.factory<Pair<String, Long>, CgChatClient>()
                    .invoke(channelName to channelId)
                    .also { it.setCallbacks() }

            initializedChatClients.put(CYBERGAME, chatClient)
            viewersCountLoaders.add(kodein.factory<String, CgViewersCountLoader>().invoke(channelName))
        }


        ignoreFilter.reloadConfig()
        imageLinkHandler.reloadConfig()

        // Start chat clients
        chatClients = initializedChatClients
        chatClients.values.forEach {
            try {
                it.start()
            } catch (t: Throwable) {
                log.error("Failed to start ${it.origin} chat client", t)
            }
        }

        // Start viewers counter
        viewersCounter = try {
            kodein
                    .factory<List<ViewersCountLoader>, ViewersCounter>()
                    .invoke(viewersCountLoaders)
                    .also { it.start() }
        } catch (t: Throwable) {
            log.error("Failed to start viewers counter", t)
            null
        }

        viewersCountWsHandler.viewersCounter.set(viewersCounter)

        // Save config
        configLoader.save()
    }

    fun stopChat() = lock.withLock {
        if (state != CHAT) IllegalStateException("Expected: $CHAT, actual: $state")
        reset()

        // Save config
        configLoader.save()
    }

    fun shutDown() = lock.withLock {
        log.info("Shutting down")

        try {
            reset()
        } catch (t: Throwable) {
            log.error("Failed to reset {} during shutdown", this.javaClass.simpleName, t)
        }

        // Запуск в отдельном треде чтобы javafx thread мог завершиться и GUI закрывался сразу
        thread(start = true, name = "ShutdownThread") {
            config.setProperty("lastMessageId", messageIdGenerator.lastId)
            configLoader.save()

            wsServer.stop()
            log.info("Websocket server stopped")

            youtubeExecutor.shutdownNow()

            okHttpClient.dispatcher().executorService().shutdown()
            log.info("OkHttpClient thread pool shutdown completed")
            okHttpClient.connectionPool().evictAll()
            log.info("OkHttpClient connections evicted")
        }

        thread(start = true, name = "TerminationThread", isDaemon = true) {
            sleep(shutdownTimeout)

            val threadsToPrint = Thread.getAllStackTraces()
                    .filterNot { (thread, _) -> thread.isDaemon || hotspotThreads.contains(thread.name) }

            log.error {
                "Process terminated after ${shutdownTimeout.toMillis()} ms of shutDown() call. Verbose information:$ls" +
                        formatStackTraces(threadsToPrint)
            }
            System.exit(5)
        }

        Platform.exit()
    }

    private fun ChatClient<*>.setCallbacks() {
        onChatMessage = { chatMessageSender.send(it) }
        onStatusMessage = { chatMessageSender.send(it) }
        onChatMessageDeleted = { chatMessageRemover.remove(it) }
    }

    private fun reset() {
        viewersCountWsHandler.viewersCounter.set(null)

        // reset BTTV channel emoticons
        emoticonStorage.putList(BTTV_CHANNEL, emptyList())
        emoticonStorage.putCodeMapping(BTTV_CHANNEL, emptyMap())
        emoticonStorage.putIdMapping(BTTV_CHANNEL, emptyMap())
        bttvEmoticonHandler.resetChannelPattern()

        // stop chat clients
        chatClients.values.forEach {
            try {
                it.stop()
            } catch (t: Throwable) {
                log.error("Failed to stop ${it.origin} chat client", t)
            }
        }

        // Значение может быть null если вызваны shutDown() и stopChat() последовательно, в любой последовательности,
        // либо если приложение было закрыто без запуска чата.
        viewersCounter?.stop()
    }

    /**
     * @return channel name if chat client should be started, null otherwise.
     * */
    private fun checkEnabled(origin: Origin): String? {
        if (!config.getBoolean("${origin.commonName}.enabled")) return null

        val channel = config.getString("${origin.commonName}.channel")
                ?: throw InvalidConfigurationException("Channel is null. Origin: $origin")
        if (channel.isEmpty()) return null

        return channel
    }

}
