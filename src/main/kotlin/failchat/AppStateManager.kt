package failchat

import either.Either
import failchat.AppState.CHAT
import failchat.AppState.SETTINGS
import failchat.Origin.BTTV_CHANNEL
import failchat.Origin.FRANKERFASEZ
import failchat.Origin.GOODGAME
import failchat.Origin.PEKA2TV
import failchat.Origin.TWITCH
import failchat.Origin.YOUTUBE
import failchat.chat.AppConfiguration
import failchat.chat.ChatClient
import failchat.chat.ChatMessageSender
import failchat.chat.MessageIdGenerator
import failchat.chat.OriginStatusManager
import failchat.chat.badge.BadgeManager
import failchat.chat.handlers.IgnoreFilter
import failchat.chat.handlers.ImageLinkHandler
import failchat.emoticon.ChannelEmoticonUpdater
import failchat.emoticon.DeletedMessagePlaceholderFactory
import failchat.emoticon.EmoticonStorage
import failchat.emoticon.FailchatEmoticonUpdater
import failchat.exception.InvalidConfigurationException
import failchat.goodgame.GgApiClient
import failchat.goodgame.GgChannel
import failchat.goodgame.GgChatClient
import failchat.goodgame.GgViewersCountLoader
import failchat.peka2tv.Peka2tvApiClient
import failchat.peka2tv.Peka2tvChatClient
import failchat.twitch.TwitchApiClient
import failchat.twitch.TwitchChatClient
import failchat.twitch.TwitchViewersCountLoader
import failchat.util.CoroutineExceptionLogger
import failchat.util.enumMap
import failchat.viewers.ViewersCountLoader
import failchat.viewers.ViewersCountWsHandler
import failchat.viewers.ViewersCounter
import failchat.youtube.ChannelId
import failchat.youtube.VideoId
import failchat.youtube.YoutubeUtils
import failchat.youtube.YtChatClient
import javafx.application.Platform
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KLogging
import org.apache.commons.configuration2.Configuration
import org.kodein.di.DKodein
import org.kodein.di.generic.factory
import org.kodein.di.generic.instance
import org.mapdb.DB
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class AppStateManager(private val kodein: DKodein) {

    private companion object : KLogging()

    private val messageIdGenerator: MessageIdGenerator = kodein.instance()
    private val peka2tvApiClient: Peka2tvApiClient = kodein.instance()
    private val twitchApiClient: TwitchApiClient = kodein.instance()
    private val goodgameApiClient: GgApiClient = kodein.instance()
    private val configLoader: ConfigLoader = kodein.instance()
    private val ignoreFilter: IgnoreFilter = kodein.instance()
    private val imageLinkHandler: ImageLinkHandler = kodein.instance()
    private val viewersCountWsHandler: ViewersCountWsHandler = kodein.instance()
    private val emoticonStorage: EmoticonStorage = kodein.instance()
    private val channelEmoticonUpdater: ChannelEmoticonUpdater = kodein.instance()
    private val failchatEmoticonUpdater: FailchatEmoticonUpdater = kodein.instance()
    private val emoticonsDb: DB = kodein.instance("emoticons")
    private val badgeManager: BadgeManager = kodein.instance()
    private val backgroundExecutorDispatcher = kodein.instance<ScheduledExecutorService>("background").asCoroutineDispatcher()
    private val originStatusManager: OriginStatusManager = kodein.instance()
    private val deletedMessagePlaceholderFactory: DeletedMessagePlaceholderFactory = kodein.instance()
    private val messageSender: ChatMessageSender = kodein.instance()

    private val lock: Lock = ReentrantLock()
    private val appConfig: AppConfiguration = kodein.instance()
    private val config: Configuration = kodein.instance()

    private var chatClients: Map<Origin, ChatClient> = emptyMap()
    private var viewersCounter: ViewersCounter? = null
    
    private var state: AppState = SETTINGS

    fun startChat(): Unit = lock.withLock {
        if (state != SETTINGS) IllegalStateException("Expected: $SETTINGS, actual: $state")

        val viewersCountLoaders: MutableList<ViewersCountLoader> = ArrayList()
        val initializedChatClients: MutableMap<Origin, ChatClient> = enumMap()
        val channelEmoticonsJobs: MutableList<Job> = ArrayList()

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

            initializedChatClients.put(PEKA2TV, chatClient)
            viewersCountLoaders.add(chatClient)
        }


        // Twitch
        checkEnabled(TWITCH)?.let { channelName ->
            val chatClient = kodein.factory<String, TwitchChatClient>()
                    .invoke(channelName)
            initializedChatClients.put(TWITCH, chatClient)
            viewersCountLoaders.add(kodein.factory<String, TwitchViewersCountLoader>().invoke(channelName))

            // load channel badges in background
            CoroutineScope(backgroundExecutorDispatcher + CoroutineName("TwitchBadgeLoader") + CoroutineExceptionLogger).launch {
                val channelId: Long = twitchApiClient.requestUserId(channelName).await()
                badgeManager.loadTwitchChannelBadges(channelId)
            }

            // load BTTV and FFZ channel emoticons in background
            channelEmoticonsJobs += CoroutineScope(backgroundExecutorDispatcher).launch {
                try {
                    channelEmoticonUpdater.updateBttvEmoticons(channelName)
                } catch (t: Throwable) {
                    logger.error("Failed to load BTTV emoticons for channel '{}'", channelName, t)
                }
            }

            channelEmoticonsJobs += CoroutineScope(backgroundExecutorDispatcher).launch {
                try {
                    channelEmoticonUpdater.updateFfzEmoticons(channelName)
                } catch (t: Throwable) {
                    logger.error("Failed to load FrankerFaceZ emoticons for channel '{}'", channelName, t)
                }
            }
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

            initializedChatClients.put(GOODGAME, chatClient)

            val counter = kodein.factory<String, GgViewersCountLoader>()
                    .invoke(channelName)
            viewersCountLoaders.add(counter)
        }


        // Youtube
        checkEnabled(YOUTUBE)?.let { channelIdOrVideoId ->
            val eitherId = YoutubeUtils.determineId(channelIdOrVideoId)
            val chatClient = kodein.factory<Either<ChannelId, VideoId>, YtChatClient>()
                    .invoke(eitherId)

            initializedChatClients.put(YOUTUBE, chatClient)
            viewersCountLoaders.add(chatClient)
        }


        ignoreFilter.reloadConfig()
        imageLinkHandler.replaceImageLinks = config.getBoolean(ConfigKeys.showImages)

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

        viewersCountWsHandler.viewersCounter = viewersCounter

        // Save config
        configLoader.save()

        updateDeletedMessagePlaceholder(channelEmoticonsJobs)
    }

    fun stopChat(): Unit = lock.withLock {
        if (state != CHAT) IllegalStateException("Expected: $CHAT, actual: $state")
        reset()

        // Save config
        configLoader.save()
    }

    fun shutDown(guiEnabled: Boolean): Unit = lock.withLock {
        logger.info("Shutting down")

        try {
            emoticonsDb.close()
            logger.info("Emoticons db was closed")
        } catch (t: Throwable) {
            logger.error("Failed to close emoticons db during a shutdown", t)
        }

        try {
            config.setProperty("lastMessageId", messageIdGenerator.lastId)
            configLoader.save()
        } catch (t: Throwable) {
            logger.error("Failed to save config during a shutdown", t)
        }

        // prevent shutdown hook from locking on System.exit()
        if (guiEnabled) {
            Platform.exit()
            System.exit(0)
        }
    }

    private fun reset() {
        viewersCountWsHandler.viewersCounter = null
        originStatusManager.reset()

        // reset BTTV and FFZ channel emoticons
        emoticonStorage.clear(BTTV_CHANNEL)
        emoticonStorage.clear(FRANKERFASEZ)

        badgeManager.resetChannelBadges()

        // stop chat clients
        chatClients.values.forEach {
            try {
                it.stop()
            } catch (t: Throwable) {
                logger.error("Failed to stop ${it.origin} chat client", t)
            }
        }

        // Значение может быть null если вызваны handleShutDown() и handleStopChat() последовательно, в любой последовательности,
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

    private fun updateDeletedMessagePlaceholder(channelEmoticonsJobs: List<Job>) {
        CoroutineScope(backgroundExecutorDispatcher + CoroutineExceptionLogger).launch {
            try {
                failchatEmoticonUpdater.update()
                channelEmoticonsJobs.forEach { it.join() }
            } catch (t: Throwable) {
                logger.error("Error while waiting for channel emoticons to update", t)
            }

            appConfig.deletedMessagePlaceholder = deletedMessagePlaceholderFactory.create()
            messageSender.sendClientConfiguration()
        }
    }

}
