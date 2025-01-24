package failchat

import failchat.AppState.CHAT
import failchat.AppState.SETTINGS
import failchat.Origin.BTTV_CHANNEL
import failchat.Origin.FRANKERFASEZ
import failchat.Origin.GOODGAME
import failchat.Origin.TWITCH
import failchat.Origin.YOUTUBE
import failchat.chat.AppConfiguration
import failchat.chat.ChatClient
import failchat.exception.InvalidConfigurationException
import failchat.util.CoroutineExceptionLogger
import failchat.util.enumMap
import failchat.viewers.ViewersCountLoader
import failchat.viewers.ViewersCounter
import failchat.youtube.YoutubeViewersCountLoader
import javafx.application.Platform
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.commons.configuration2.Configuration
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class AppStateManager(private val deps: Dependencies) {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    private val messageIdGenerator = deps.messageIdGenerator
    private val twitchApiClient = deps.tokenAwareTwitchApiClient
    private val goodgameApiClient = deps.ggApiClient
    private val configLoader = deps.configLoader
    private val ignoreFilter = deps.ignoreFilter
    private val imageLinkHandler = deps.imageLinkHandler
    private val viewersCountWsHandler = deps.viewersCountWsHandler
    private val emoticonStorage = deps.emoticonStorage
    private val channelEmoticonUpdater = deps.channelEmoticonUpdater
    private val failchatEmoticonUpdater = deps.failchatEmoticonUpdater
    private val emoticonsDb = deps.emoticonsDb
    private val badgeManager = deps.badgeManager
    private val backgroundExecutorDispatcher = deps.backgroundExecutorService.asCoroutineDispatcher()
    private val originStatusManager = deps.originStatusManager
    private val deletedMessagePlaceholderFactory = deps.deletedMessagePlaceholderFactory
    private val messageSender = deps.chatMessageSender

    private val lock: Lock = ReentrantLock()
    private val appConfig: AppConfiguration = deps.appConfiguration
    private val config: Configuration = deps.configuration

    private var chatClients: Map<Origin, ChatClient> = emptyMap()
    private var viewersCounter: ViewersCounter? = null

    private var state: AppState = SETTINGS

    fun startChat(): Unit = lock.withLock {
        if (state != SETTINGS) {
            throw IllegalStateException("Expected: $SETTINGS, actual: $state")
        }
        state = CHAT

        val viewersCountLoaders: MutableList<ViewersCountLoader> = ArrayList()
        val initializedChatClients: MutableMap<Origin, ChatClient> = enumMap()
        val channelEmoticonsJobs: MutableList<Job> = ArrayList()

        // Twitch
        checkEnabled(TWITCH)?.let { channelName ->
            val chatClient = deps.twitchChatClient.invoke(channelName)
            initializedChatClients.put(TWITCH, chatClient)
            viewersCountLoaders.add(deps.twitchViewersCountLoader.invoke(channelName))

            val channelId = try {
                runBlocking { twitchApiClient.getUserId(channelName) }
            } catch (e: Exception) {
                logger.warn("Failed to get twitch channel info. channel name: {}", channelName, e)
                return@let
            }

            // load channel badges in background
            CoroutineScope(backgroundExecutorDispatcher + CoroutineName("TwitchBadgeLoader") + CoroutineExceptionLogger).launch {
                badgeManager.loadTwitchChannelBadges(channelId)
            }

            // load BTTV/FFZ/7tv channel emoticons in background
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

            channelEmoticonsJobs += CoroutineScope(backgroundExecutorDispatcher).launch {
                try {
                    channelEmoticonUpdater.update7tvEmoticons(channelId)
                } catch (t: Throwable) {
                    logger.error("Failed to load 7tv emoticons for channel '{}'", channelName, t)
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

            val chatClient = deps.ggChatClient.invoke(channel)

            initializedChatClients.put(GOODGAME, chatClient)

            val counter = deps.ggViewersCountLoader.invoke(channelName)
            viewersCountLoaders.add(counter)
        }


        // Youtube
        checkEnabled(YOUTUBE)?.let { videoId ->
            val chatClient = deps.youtubeChatClient.invoke(videoId)
            initializedChatClients.put(YOUTUBE, chatClient)
            viewersCountLoaders.add(YoutubeViewersCountLoader(videoId, deps.youtubeClient))
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
            deps.viewersCounter
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
        if (state != CHAT) {
            throw IllegalStateException("Expected: $CHAT, actual: $state")
        }
        state = SETTINGS

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

        try {
            deps.chatHistoryWriter.close()
        } catch (t: Throwable) {
            logger.error("Failed to close chat history writer", t)
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
