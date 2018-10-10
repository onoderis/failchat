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
import failchat.chat.badge.BadgeManager
import failchat.chat.handlers.IgnoreFilter
import failchat.chat.handlers.ImageLinkHandler
import failchat.cybergame.CgApiClient
import failchat.cybergame.CgChatClient
import failchat.cybergame.CgViewersCountLoader
import failchat.emoticon.EmoticonStorage
import failchat.exception.InvalidConfigurationException
import failchat.goodgame.GgApiClient
import failchat.goodgame.GgChannel
import failchat.goodgame.GgChatClient
import failchat.peka2tv.Peka2tvApiClient
import failchat.peka2tv.Peka2tvChatClient
import failchat.twitch.BttvApiClient
import failchat.twitch.BttvChannelNotFoundException
import failchat.twitch.BttvEmoticonHandler
import failchat.twitch.TwitchApiClient
import failchat.twitch.TwitchChatClient
import failchat.twitch.TwitchViewersCountLoader
import failchat.util.CoroutineExceptionLogger
import failchat.util.completionCause
import failchat.util.formatStackTraces
import failchat.util.hotspotThreads
import failchat.util.logException
import failchat.util.ls
import failchat.util.sleep
import failchat.viewers.ViewersCountLoader
import failchat.viewers.ViewersCountWsHandler
import failchat.viewers.ViewersCounter
import failchat.youtube.ChannelId
import failchat.youtube.VideoId
import failchat.youtube.YoutubeUtils
import failchat.youtube.YtChatClient
import io.ktor.server.engine.ApplicationEngine
import javafx.application.Platform
import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.future.await
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import mu.KLogging
import okhttp3.OkHttpClient
import org.apache.commons.configuration2.Configuration
import java.time.Duration
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

class AppStateManager(private val kodein: Kodein) {

    private companion object : KLogging() {
        val shutdownTimeout: Duration = Duration.ofMillis(3500)
    }

    private val messageIdGenerator: MessageIdGenerator = kodein.instance()
    private val chatMessageSender: ChatMessageSender = kodein.instance()
    private val chatMessageRemover: ChatMessageRemover = kodein.instance()
    private val peka2tvApiClient: Peka2tvApiClient = kodein.instance()
    private val twitchApiClient: TwitchApiClient = kodein.instance()
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
    private val badgeManager: BadgeManager = kodein.instance()
    private val backroundExecutorDispatcher = kodein.instance<ScheduledExecutorService>("background").asCoroutineDispatcher()

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
                logger.warn("Failed to get peka2tv channel id. channel name: {}", channelName, e)
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

            // load channel badges in background
            launch(backroundExecutorDispatcher + CoroutineName("TwitchBadgeLoader") + CoroutineExceptionLogger) {
                val channelId: Long = twitchApiClient.requestUserId(channelName).await()
                badgeManager.loadTwitchChannelBadges(channelId)
            }

            // load BTTV channel emoticons in background
            bttvApiClient.loadChannelEmoticons(channelName)
                    .thenApply<Unit> { emoticons ->
                        emoticonStorage.putCodeMapping(BTTV_CHANNEL, emoticons.map { it.code.toLowerCase() to it }.toMap())
                        emoticonStorage.putList(BTTV_CHANNEL, emoticons)
                        logger.info("BTTV emoticons loaded for channel '{}', count: {}", channelName, emoticons.size)
                    }
                    .exceptionally { t ->
                        val completionCause = t.completionCause()
                        if (completionCause is BttvChannelNotFoundException) {
                            logger.info("BTTV emoticons not found for channel '{}'", completionCause.channel)
                        } else {
                            logger.error("Failed to load BTTV emoticons for channel '{}'", channelName, completionCause)
                        }
                    }
                    .logException()
        }


        // Goodgame
        checkEnabled(GOODGAME)?.let { channelName ->
            // get channel id by channel name
            val channel = try {
                runBlocking { goodgameApiClient.requestChannelInfo(channelName) }
            } catch (e: Exception) {
                logger.warn("Failed to get goodgame channel info. channel name: {}", channelName, e)
                return@let
            }

            val chatClient = kodein.factory<GgChannel, GgChatClient>()
                    .invoke(channel)
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
                logger.warn("Failed to get cybergame channel id. channel name: {}", channelName, e)
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
                logger.error("Failed to start ${it.origin} chat client", t)
            }
        }

        // Start viewers counter
        viewersCounter = try {
            kodein
                    .factory<List<ViewersCountLoader>, ViewersCounter>()
                    .invoke(viewersCountLoaders)
                    .also { it.start() }
        } catch (t: Throwable) {
            logger.error("Failed to start viewers counter", t)
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
        logger.info("Shutting down")

        try {
            reset()
        } catch (t: Throwable) {
            logger.error("Failed to reset {} during a shutdown", this.javaClass.simpleName, t)
        }

        // Запуск в отдельном треде чтобы javafx thread мог завершиться и GUI закрывался сразу
        thread(start = true, name = "ShutdownThread") {
            config.setProperty("lastMessageId", messageIdGenerator.lastId)
            configLoader.save()

            kodein.instance<ApplicationEngine>().stop(0, 0, TimeUnit.SECONDS)
            logger.info("Http/websocket server was stopped")

            youtubeExecutor.shutdownNow()
            logger.info("Youtube executor was stopped")

            okHttpClient.dispatcher().executorService().shutdown()
            logger.info("OkHttpClient thread pool shutdown was completed")
            okHttpClient.connectionPool().evictAll()
            logger.info("OkHttpClient connections was evicted")
        }

        thread(start = true, name = "TerminationThread", isDaemon = true) {
            sleep(shutdownTimeout)

            val threadsToPrint = Thread.getAllStackTraces()
                    .filterNot { (thread, _) -> thread.isDaemon || hotspotThreads.contains(thread.name) }

            logger.error {
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

        badgeManager.resetChannelBadges()

        // stop chat clients
        chatClients.values.forEach {
            try {
                it.stop()
            } catch (t: Throwable) {
                logger.error("Failed to stop ${it.origin} chat client", t)
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
