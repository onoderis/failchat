package failchat

import failchat.chat.AppConfiguration
import failchat.chat.ChatClientCallbacks
import failchat.chat.ChatMessage
import failchat.chat.ChatMessageHistory
import failchat.chat.ChatMessageRemover
import failchat.chat.ChatMessageSender
import failchat.chat.MessageFilter
import failchat.chat.MessageIdGenerator
import failchat.chat.OnChatMessageCallback
import failchat.chat.OnChatMessageDeletedCallback
import failchat.chat.OnStatusUpdateCallback
import failchat.chat.OriginStatusManager
import failchat.chat.badge.BadgeFinder
import failchat.chat.badge.BadgeManager
import failchat.chat.badge.BadgeStorage
import failchat.chat.handlers.ChatHistoryLogger
import failchat.chat.handlers.EmojiHandler
import failchat.chat.handlers.FailchatEmoticonHandler
import failchat.chat.handlers.IgnoreFilter
import failchat.chat.handlers.ImageLinkHandler
import failchat.chat.handlers.LinkHandler
import failchat.chat.handlers.OriginsStatusHandler
import failchat.chat.handlers.SpaceSeparatedEmoticonHandler
import failchat.emoticon.ChannelEmoticonUpdater
import failchat.emoticon.DeletedMessagePlaceholderFactory
import failchat.emoticon.Emoticon
import failchat.emoticon.EmoticonFinder
import failchat.emoticon.EmoticonLoadConfiguration
import failchat.emoticon.EmoticonManager
import failchat.emoticon.EmoticonStorage
import failchat.emoticon.FailchatEmoticonScanner
import failchat.emoticon.FailchatEmoticonUpdater
import failchat.emoticon.GlobalEmoticonUpdater
import failchat.emoticon.MapdbFactory
import failchat.emoticon.TwitchEmoticonFactory
import failchat.github.GithubClient
import failchat.github.ReleaseChecker
import failchat.goodgame.GgApi2Client
import failchat.goodgame.GgApiClient
import failchat.goodgame.GgBadgeHandler
import failchat.goodgame.GgChannel
import failchat.goodgame.GgChatClient
import failchat.goodgame.GgEmoticonHandler
import failchat.goodgame.GgEmoticonLoadConfiguration
import failchat.goodgame.GgEmoticonLoader
import failchat.goodgame.GgViewersCountLoader
import failchat.gui.ChatGuiEventHandler
import failchat.gui.FullGuiEventHandler
import failchat.gui.GuiMode
import failchat.peka2tv.Peka2TvEmoticonLoader
import failchat.peka2tv.Peka2tvApiClient
import failchat.peka2tv.Peka2tvBadgeHandler
import failchat.peka2tv.Peka2tvChatClient
import failchat.peka2tv.Peka2tvEmoticonHandler
import failchat.peka2tv.Peka2tvEmoticonLoadConfiguration
import failchat.skin.SkinScanner
import failchat.twitch.BttvApiClient
import failchat.twitch.BttvEmoticonHandler
import failchat.twitch.BttvGlobalEmoticonLoadConfiguration
import failchat.twitch.BttvGlobalEmoticonLoader
import failchat.twitch.ConfigurationTokenContainer
import failchat.twitch.FfzApiClient
import failchat.twitch.FfzEmoticonHandler
import failchat.twitch.SevenTvApiClient
import failchat.twitch.SevenTvGlobalEmoticonLoadConfiguration
import failchat.twitch.SevenTvGlobalEmoticonLoader
import failchat.twitch.TokenAwareTwitchApiClient
import failchat.twitch.TwitchApiClient
import failchat.twitch.TwitchBadgeHandler
import failchat.twitch.TwitchChatClient
import failchat.twitch.TwitchEmotesTagParser
import failchat.twitch.TwitchEmoticonHandler
import failchat.twitch.TwitchEmoticonLoadConfiguration
import failchat.twitch.TwitchGlobalEmoticonLoader
import failchat.twitch.TwitchViewersCountLoader
import failchat.util.objectMapper
import failchat.viewers.ViewersCountLoader
import failchat.viewers.ViewersCountWsHandler
import failchat.viewers.ViewersCounter
import failchat.ws.server.ClientConfigurationWsHandler
import failchat.ws.server.DeleteWsMessageHandler
import failchat.ws.server.IgnoreWsMessageHandler
import failchat.ws.server.WsFrameSender
import failchat.ws.server.WsMessageDispatcher
import failchat.youtube.YoutubeChatClient
import failchat.youtube.YoutubeClient
import failchat.youtube.YoutubeHtmlParser
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.OkHttpClient
import java.io.BufferedWriter
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicInteger

@Suppress("MemberVisibilityCanBePrivate")
class Dependencies {

    // General purpose dependencies
    val objectMapper = objectMapper()
    val okHttpClient = OkHttpClient.Builder()
//            .addInterceptor(okhttp3.logging.HttpLoggingInterceptor(failchat.util.OkHttpLogger)
//            .also { it.level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY })
            .build()
    val httpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = okHttpClient
        }
    }

    val backgroundExecutorService: ScheduledExecutorService = run {
        val threadNumber = AtomicInteger()
        Executors.newScheduledThreadPool(4) {
            Thread(it, "BackgroundExecutor-${threadNumber.getAndIncrement()}").apply {
                isDaemon = true
                priority = 3
            }
        }
    }


    // Configuration
    val configLoader = ConfigLoader(failchatHomePath)
    val configuration = configLoader.load()
    val appConfiguration = AppConfiguration(configuration)


    // Core dependencies
    val messageIdGenerator = MessageIdGenerator(configuration.getLong("lastMessageId"))
    val skinList = SkinScanner(workingDirectory).scan()

    val wsFrameSender = WsFrameSender()
    val chatMessageSender = ChatMessageSender(
            wsFrameSender,
            appConfiguration,
            objectMapper
    )
    val viewersCountWsHandler = ViewersCountWsHandler(configuration)
    val originStatusManager = OriginStatusManager(chatMessageSender)
    val chatMessageRemover = ChatMessageRemover(chatMessageSender)
    val viewersCounter = { vcLoaders: List<ViewersCountLoader> ->
        ViewersCounter(vcLoaders, chatMessageSender)
    }
    val chatMessageHistory = ChatMessageHistory(50)
    val ignoreFilter = IgnoreFilter(configuration)
    val originsStatusHandler = OriginsStatusHandler(
            originStatusManager,
            chatMessageSender
    )
    val imageLinkHandler = ImageLinkHandler()
    val badgeStorage = BadgeStorage()
    val badgeFinder: BadgeFinder = badgeStorage

    val wsMessageDispatcher = WsMessageDispatcher(
            objectMapper,
            listOf(
                    ClientConfigurationWsHandler(chatMessageSender),
                    viewersCountWsHandler,
                    DeleteWsMessageHandler(chatMessageRemover),
                    IgnoreWsMessageHandler(ignoreFilter, configuration),
                    originsStatusHandler
            )
    )


    // Http/websocket server
    val applicationEngine = createHttpServer(wsMessageDispatcher, wsFrameSender)


    // Emoticons
    val emoticonsDb = MapdbFactory.create(emoticonDbFile)
    val emoticonStorage = EmoticonStorage()
    val emoticonFinder: EmoticonFinder = emoticonStorage
    val failchatEmoticonHandler = FailchatEmoticonHandler(emoticonFinder)
    val emoticonManager = EmoticonManager(emoticonStorage)
    val deletedMessagePlaceholderFactory = DeletedMessagePlaceholderFactory(
            emoticonFinder,
            configuration
    )
    val failchatEmoticonScanner = FailchatEmoticonScanner(
            failchatEmoticonsDirectory,
            failchatEmoticonsUrl
    )
    val failchatEmoticonUpdater = FailchatEmoticonUpdater(
            emoticonStorage,
            failchatEmoticonScanner
    )


    // Chat history logger
    val chatHistoryWriter: BufferedWriter = run {
        val chatHistoryFilePath = workingDirectory.resolve("history").resolve("history.txt")
        Files.createDirectories(chatHistoryFilePath.parent)
        Files.newBufferedWriter(chatHistoryFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }
    val chatHistoryLogger = ChatHistoryLogger(chatHistoryWriter)


    // Chat client callbacks
    val onChatMessageCallback = {
        //todo build handler pipeline for each start
        val handlers = mutableListOf(
                LinkHandler(),
                imageLinkHandler,
                EmojiHandler(),
                failchatEmoticonHandler
        )
        if (configuration.getBoolean(ConfigKeys.saveMessageHistory)) {
            handlers += chatHistoryLogger
        }
        OnChatMessageCallback(
                listOf<MessageFilter<ChatMessage>>(ignoreFilter),
                handlers,
                chatMessageHistory,
                chatMessageSender
        )
    }
    val onStatusUpdateCallback = OnStatusUpdateCallback(originStatusManager)
    val onChatMessageDeletedCallback = OnChatMessageDeletedCallback(chatMessageRemover)
    val chatClientCallbacks = {
        ChatClientCallbacks(
                onChatMessageCallback.invoke(),
                onStatusUpdateCallback,
                onChatMessageDeletedCallback
        )
    }

    // Release checker
    val githubClient = GithubClient(
            configuration.getString("github.api-url"),
            okHttpClient,
            objectMapper
    )
    val releaseChecker = ReleaseChecker(githubClient, configuration)


    // Origin specific dependencies

    // Peka2tv
    val peka2tvApiClient = Peka2tvApiClient(
            okHttpClient,
            objectMapper,
            configuration.getString("peka2tv.api-url")
    )
    val peka2tvEmoticonBulkLoader = Peka2TvEmoticonLoader(peka2tvApiClient)
    val peka2tvEmoticonLoadConfiguration = Peka2tvEmoticonLoadConfiguration(peka2tvEmoticonBulkLoader)
    val peka2tvBadgeHandler = Peka2tvBadgeHandler(badgeFinder)
    val peka2tvEmoticonHandler = Peka2tvEmoticonHandler(emoticonFinder)
    val peka2tvChatClient = { channelNameAndId: Pair<String, Long> ->
        Peka2tvChatClient(
                channelName = channelNameAndId.first,
                channelId = channelNameAndId.second,
                socketIoUrl = configuration.getString("peka2tv.socketio-url"),
                okHttpClient = okHttpClient,
                messageIdGenerator = messageIdGenerator,
                emoticonHandler = peka2tvEmoticonHandler,
                badgeHandler = peka2tvBadgeHandler,
                history = chatMessageHistory,
                callbacks = chatClientCallbacks.invoke()
        )
    }

    // BTTV
    val bttvApiClient = BttvApiClient(
            httpClient = okHttpClient,
            apiUrl = configuration.getString("bttv.api-url"),
            objectMapper = objectMapper
    )
    val bttvEmoticonHandler = BttvEmoticonHandler(emoticonFinder)
    val bttvGlobalEmoticonBulkLoader = BttvGlobalEmoticonLoader(bttvApiClient)
    val bttvGlobalEmoticonLoadConfiguration = BttvGlobalEmoticonLoadConfiguration(bttvGlobalEmoticonBulkLoader)

    // FFZ
    val ffzApiClient = FfzApiClient(
            httpClient = okHttpClient,
            apiUrl = configuration.getString(ConfigKeys.frankerfacezApiUrl),
            objectMapper = objectMapper
    )
    val ffzEmoticonHandler = FfzEmoticonHandler(emoticonFinder)

    // 7tv
    val sevenTvApiClient =
            SevenTvApiClient(
                    httpClient = okHttpClient,
                    objectMapper = objectMapper
            )
    val sevenTvGlobalMessageHandler = SpaceSeparatedEmoticonHandler(Origin.SEVEN_TV_GLOBAL, emoticonFinder)
    val sevenTvChannelMessageHandler = SpaceSeparatedEmoticonHandler(Origin.SEVEN_TV_CHANNEL, emoticonFinder)
    val sevenTvGlobalEmoticonLoader = SevenTvGlobalEmoticonLoader(sevenTvApiClient)
    val sevenTvGlobalEmoticonLoadConfiguration = SevenTvGlobalEmoticonLoadConfiguration(sevenTvGlobalEmoticonLoader)

    // Twitch
    val twitchEmotesTagParser = TwitchEmotesTagParser()
    val twitchEmoticonFactory = TwitchEmoticonFactory()
    val twitchBadgeHandler = TwitchBadgeHandler(badgeFinder)
    val twitchEmoticonHandler = TwitchEmoticonHandler(twitchEmotesTagParser)
    val twitchApiClient = TwitchApiClient(
            httpClient = okHttpClient,
            objectMapper = objectMapper,
            clientId = configuration.getString(ConfigKeys.Twitch.clientId)
    )
    val tokenAwareTwitchApiClient = TokenAwareTwitchApiClient(
            twitchApiClient = twitchApiClient,
            clientSecret = configuration.getString(ConfigKeys.Twitch.clientSecret),
            tokenContainer = ConfigurationTokenContainer(configuration)
    )
    val twitchGlobalEmoticonLoader = TwitchGlobalEmoticonLoader(tokenAwareTwitchApiClient)
    val twitchEmoticonLoadConfiguration = TwitchEmoticonLoadConfiguration(
            twitchGlobalEmoticonLoader
    )
    val twitchChatClient = { channelName: String ->
        TwitchChatClient(
                userName = channelName,
                ircAddress = configuration.getString("twitch.irc-address"),
                ircPort = configuration.getInt("twitch.irc-port"),
                botName = configuration.getString("twitch.bot-name"),
                botPassword = configuration.getString("twitch.bot-password"),
                twitchEmoticonHandler = twitchEmoticonHandler,
                messageIdGenerator = messageIdGenerator,
                bttvEmoticonHandler = bttvEmoticonHandler,
                ffzEmoticonHandler = ffzEmoticonHandler,
                sevenTvGlobalEmoticonHandler = sevenTvGlobalMessageHandler,
                sevenTvChannelEmoticonHandler = sevenTvChannelMessageHandler,
                twitchBadgeHandler = twitchBadgeHandler,
                history = chatMessageHistory,
                callbacks = chatClientCallbacks.invoke()
        )
    }
    val twitchViewersCountLoader = { channelName: String ->
        TwitchViewersCountLoader(channelName, tokenAwareTwitchApiClient)
    }


    // Goodgame
    val ggApi2Client = GgApi2Client(
            httpClient = okHttpClient,
            objectMapper = objectMapper
    )
    val ggBadgeHandler = { channel: GgChannel ->
        GgBadgeHandler(channel, configuration)
    }
    val ggChatClient = { channel: GgChannel ->
        GgChatClient(
                channel = channel,
                webSocketUri = configuration.getString("goodgame.ws-url"),
                messageIdGenerator = messageIdGenerator,
                emoticonHandler = ggEmoticonHandler,
                badgeHandler = ggBadgeHandler.invoke(channel),
                history = chatMessageHistory,
                callbacks = chatClientCallbacks.invoke(),
                objectMapper = objectMapper
        )
    }
    val ggViewersCountLoader = { channelName: String ->
        GgViewersCountLoader(
                ggApi2Client,
                channelName
        )
    }
    val ggApiClient = GgApiClient(
            httpClient = okHttpClient,
            apiUrl = configuration.getString("goodgame.api-url"),
            emoticonsJsUrl = configuration.getString("goodgame.emoticon-js-url"),
            objectMapper = objectMapper
    )
    val ggEmoticonBulkLoader = GgEmoticonLoader(ggApiClient)
    val ggEmoticonLoadConfiguration = GgEmoticonLoadConfiguration(ggEmoticonBulkLoader)
    val ggEmoticonHandler = GgEmoticonHandler(emoticonFinder)


    // Youtube
    val youtubeHtmlParser = YoutubeHtmlParser(objectMapper)
    val youtubeClient = YoutubeClient(
            httpClient,
            objectMapper,
            youtubeHtmlParser
    )
    val youtubeChatClient = { videoId: String ->
        YoutubeChatClient(
                chatClientCallbacks.invoke(),
                youtubeClient,
                messageIdGenerator,
                chatMessageHistory,
                videoId
        )
    }


    // Etc
    val emoticonLoadConfigurations: List<EmoticonLoadConfiguration<out Emoticon>> = listOf(
            peka2tvEmoticonLoadConfiguration,
            ggEmoticonLoadConfiguration,
            twitchEmoticonLoadConfiguration,
            bttvGlobalEmoticonLoadConfiguration,
            sevenTvGlobalEmoticonLoadConfiguration
    )
    val channelEmoticonUpdater = ChannelEmoticonUpdater(
            emoticonStorage,
            bttvApiClient,
            ffzApiClient,
            sevenTvApiClient
    )
    val badgeManager = BadgeManager(
            badgeStorage,
            tokenAwareTwitchApiClient,
            peka2tvApiClient
    )

    val appStateManager = AppStateManager(this)

    val guiEventHandler = run {
        val guiMode = GuiMode.valueOf(configuration.getString("gui-mode"))
        when (guiMode) {
            GuiMode.CHAT_ONLY -> ChatGuiEventHandler(
                    appStateManager,
                    chatMessageSender
            )
            GuiMode.FULL_GUI -> FullGuiEventHandler(
                    appStateManager,
                    chatMessageSender,
                    configuration
            )
            else -> error("Unexpected gui mode: $guiMode")
        }
    }
    val globalEmoticonUpdater = GlobalEmoticonUpdater(
            emoticonManager,
            emoticonLoadConfigurations,
            backgroundExecutorService,
            guiEventHandler,
            configuration
    )
}
