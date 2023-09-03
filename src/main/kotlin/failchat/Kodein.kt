@file:Suppress("RemoveExplicitTypeArguments")

package failchat

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.chat.AppConfiguration
import failchat.chat.ChatClientCallbacks
import failchat.chat.ChatMessage
import failchat.chat.ChatMessageHistory
import failchat.chat.ChatMessageRemover
import failchat.chat.ChatMessageSender
import failchat.chat.MessageHandler
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
import failchat.goodgame.GgEmoticonBulkLoader
import failchat.goodgame.GgEmoticonHandler
import failchat.goodgame.GgEmoticonLoadConfiguration
import failchat.goodgame.GgViewersCountLoader
import failchat.gui.ChatGuiEventHandler
import failchat.gui.FullGuiEventHandler
import failchat.gui.GuiEventHandler
import failchat.gui.GuiMode
import failchat.peka2tv.Peka2tvApiClient
import failchat.peka2tv.Peka2tvBadgeHandler
import failchat.peka2tv.Peka2tvChatClient
import failchat.peka2tv.Peka2tvEmoticonBulkLoader
import failchat.peka2tv.Peka2tvEmoticonHandler
import failchat.peka2tv.Peka2tvEmoticonLoadConfiguration
import failchat.reporter.EventReporter
import failchat.reporter.GAEventReporter
import failchat.reporter.UserIdManager
import failchat.skin.Skin
import failchat.skin.SkinScanner
import failchat.twitch.BttvApiClient
import failchat.twitch.BttvEmoticonHandler
import failchat.twitch.BttvGlobalEmoticonBulkLoader
import failchat.twitch.BttvGlobalEmoticonLoadConfiguration
import failchat.twitch.ConfigurationTokenContainer
import failchat.twitch.FfzApiClient
import failchat.twitch.FfzEmoticonHandler
import failchat.twitch.SevenTvApiClient
import failchat.twitch.SevenTvGlobalEmoticonLoadConfiguration
import failchat.twitch.SevenTvGlobalEmoticonLoader
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
import io.ktor.server.engine.ApplicationEngine
import okhttp3.OkHttpClient
import org.apache.commons.configuration2.Configuration
import org.kodein.di.DI
import org.kodein.di.DirectDI
import org.kodein.di.bind
import org.kodein.di.factory
import org.kodein.di.instance
import org.kodein.di.singleton
import org.mapdb.DB
import java.io.BufferedWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicInteger

val kodein = DI.direct {

    // Http/websocket server
    bind<ApplicationEngine>() with singleton { createHttpServer() }
    bind<WsMessageDispatcher>() with singleton {
        WsMessageDispatcher(
                instance<ObjectMapper>(),
                listOf(
                        ClientConfigurationWsHandler(instance<ChatMessageSender>()),
                        instance<ViewersCountWsHandler>(),
                        DeleteWsMessageHandler(instance<ChatMessageRemover>()),
                        IgnoreWsMessageHandler(instance<IgnoreFilter>(), instance<Configuration>()),
                        instance<OriginsStatusHandler>()
                )
        )
    }
    bind<WsFrameSender>() with singleton { WsFrameSender() }
    bind<OriginsStatusHandler>() with singleton {
        OriginsStatusHandler(
                instance<OriginStatusManager>(),
                instance<ChatMessageSender>()
        )
    }

    bind<ViewersCountWsHandler>() with singleton {
        ViewersCountWsHandler(instance<Configuration>())
    }

    // Core dependencies
    bind<AppStateManager>() with singleton { AppStateManager(directDI) }
    bind<AppConfiguration>() with singleton { AppConfiguration(instance<Configuration>()) }
    bind<OriginStatusManager>() with singleton {
        OriginStatusManager(instance<ChatMessageSender>())
    }
    bind<ConfigLoader>() with singleton { ConfigLoader(instance<Path>("homeDirectory")) }
    bind<Configuration>() with singleton { instance<ConfigLoader>().load() }
    bind<ChatMessageSender>() with singleton {
        ChatMessageSender(
                instance<WsFrameSender>(),
                instance<AppConfiguration>(),
                instance<ObjectMapper>()
        )
    }
    bind<ChatMessageRemover>() with singleton {
        ChatMessageRemover(instance<ChatMessageSender>())
    }
    bind<ViewersCounter>() with factory { vcLoaders: List<ViewersCountLoader> ->
        ViewersCounter(
                vcLoaders,
                instance<ChatMessageSender>()
        )
    }
    bind<GuiEventHandler>() with singleton {
        val guiMode = GuiMode.valueOf(instance<Configuration>().getString("gui-mode"))

        when (guiMode) {
            GuiMode.CHAT_ONLY -> ChatGuiEventHandler(
                    instance<AppStateManager>(),
                    instance<ChatMessageSender>()
            )
            GuiMode.FULL_GUI -> FullGuiEventHandler(
                    instance<AppStateManager>(),
                    instance<ChatMessageSender>(),
                    instance<Configuration>()
            )
            else -> error("Unexpected gui mode: $guiMode")
        }
    }
    bind<ChatMessageHistory>() with singleton { ChatMessageHistory(50) }
    bind<DeletedMessagePlaceholderFactory>() with singleton {
        DeletedMessagePlaceholderFactory(
                instance<EmoticonFinder>(),
                instance<Configuration>()
        )
    }

    // Emoticons
    bind<DB>("emoticons") with singleton { MapdbFactory.create(instance<Path>("emoticonDbFile")) }
    bind<EmoticonStorage>() with singleton {
        EmoticonStorage()
    }
    bind<EmoticonFinder>() with singleton { instance<EmoticonStorage>() }
    bind<EmoticonManager>() with singleton {
        EmoticonManager(
                instance<EmoticonStorage>(),
                instance<ScheduledExecutorService>("background")
        )
    }
    bind<List<EmoticonLoadConfiguration<out Emoticon>>>("emoticonLoadConfigurations") with singleton {
        listOf(
                instance<Peka2tvEmoticonLoadConfiguration>(),
                instance<GgEmoticonLoadConfiguration>(),
                instance<TwitchEmoticonLoadConfiguration>(),
                instance<BttvGlobalEmoticonLoadConfiguration>(),
                instance<SevenTvGlobalEmoticonLoadConfiguration>()
        )
    }
    bind<GlobalEmoticonUpdater>() with singleton {
        GlobalEmoticonUpdater(
                instance<EmoticonManager>(),
                instance<List<EmoticonLoadConfiguration<out Emoticon>>>("emoticonLoadConfigurations"),
                instance<ScheduledExecutorService>("background"),
                instance<GuiEventHandler>(),
                instance<Configuration>()
        )
    }
    bind<ChannelEmoticonUpdater>() with singleton {
        ChannelEmoticonUpdater(
                instance<EmoticonStorage>(),
                instance<BttvApiClient>(),
                instance<FfzApiClient>(),
                instance<SevenTvApiClient>()
        )
    }

    bind<FailchatEmoticonUpdater>() with singleton {
        FailchatEmoticonUpdater(
                instance<EmoticonStorage>(),
                instance<FailchatEmoticonScanner>()
        )
    }
    bind<FailchatEmoticonScanner>() with singleton {
        FailchatEmoticonScanner(
                instance<Path>("failchatEmoticonsDirectory"),
                instance<String>("failchatEmoticonsUrl")
        )
    }


    // Badges
    bind<BadgeStorage>() with singleton { BadgeStorage() }
    bind<BadgeFinder>() with singleton { instance<BadgeStorage>() }
    bind<BadgeManager>() with singleton {
        BadgeManager(
                instance<BadgeStorage>(),
                instance<TwitchApiClient>(),
                instance<Peka2tvApiClient>()
        )
    }

    // General purpose dependencies
    bind<ObjectMapper>() with singleton {
        objectMapper()
    }
    bind<OkHttpClient>() with singleton {
        OkHttpClient.Builder()
//                .addInterceptor(okhttp3.logging.HttpLoggingInterceptor(failchat.util.OkHttpLogger).also { it.level = okhttp3.logging.HttpLoggingInterceptor.Level.BODY })
                .build()
    }


    // Message handlers and filters
    bind<IgnoreFilter>() with singleton { IgnoreFilter(instance<Configuration>()) }
    bind<ImageLinkHandler>() with singleton { ImageLinkHandler() }
    bind<FailchatEmoticonHandler>() with singleton {
        FailchatEmoticonHandler(instance<EmoticonFinder>())
    }

    // Chat history logger
    bind<BufferedWriter>(tag = "chatHistoryWriter") with singleton {
        val chatHistoryFilePath = instance<Path>("workingDirectory").resolve("history").resolve("history.txt")
        Files.createDirectories(chatHistoryFilePath.parent)
        Files.newBufferedWriter(chatHistoryFilePath, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }
    bind<ChatHistoryLogger>() with singleton {
        ChatHistoryLogger(instance<BufferedWriter>(tag = "chatHistoryWriter"))
    }


    // Chat client callbacks
    bind<ChatClientCallbacks>() with factory {
        ChatClientCallbacks(
                factory<Unit, OnChatMessageCallback>().invoke(Unit),
                instance<OnStatusUpdateCallback>(),
                instance<OnChatMessageDeletedCallback>()
        )
    }
    bind<OnChatMessageCallback>() with factory {
        initMessagePipeline(directDI)
    }
    bind<OnStatusUpdateCallback>() with singleton {
        OnStatusUpdateCallback(
                instance<OriginStatusManager>()
        )
    }
    bind<OnChatMessageDeletedCallback>() with singleton {
        OnChatMessageDeletedCallback(instance<ChatMessageRemover>())
    }


    // Etc
    bind<Path>("workingDirectory") with singleton { Paths.get("") }
    bind<Path>("homeDirectory") with singleton { getFailchatHomePath() }
    bind<Path>("failchatEmoticonsDirectory") with singleton { instance<Path>("homeDirectory").resolve("failchat-emoticons") }
    bind<Path>("emoticonCacheDirectory") with singleton { instance<Path>("workingDirectory").resolve("emoticons") }
    bind<Path>("emoticonDbFile") with singleton { instance<Path>("emoticonCacheDirectory").resolve("emoticons.db") }
    bind<String>("failchatEmoticonsUrl") with singleton { "http://${FailchatServerInfo.host.hostAddress}:${FailchatServerInfo.port}/emoticons/" }
    bind<String>("userId") with singleton { instance<UserIdManager>().getUserId() }

    bind<MessageIdGenerator>() with singleton { MessageIdGenerator(instance<Configuration>().getLong("lastMessageId")) }
    bind<List<Skin>>() with singleton { SkinScanner(instance<Path>("workingDirectory")).scan() }
    bind<UserIdManager>() with singleton { UserIdManager(instance<Path>("homeDirectory")) }
    bind<EventReporter>() with singleton {
        val config = instance<Configuration>()
        GAEventReporter(
                instance<OkHttpClient>(),
                instance<String>("userId"),
                config.getString("version"),
                config.getString("reporter.tracking-id")
        )
    }


    // Release related
    bind<ReleaseChecker>() with singleton { ReleaseChecker(instance<GithubClient>(), instance<Configuration>()) }
    bind<GithubClient>() with singleton {
        GithubClient(
                instance<Configuration>().getString("github.api-url"),
                instance<OkHttpClient>(),
                instance<ObjectMapper>()
        )
    }


    // Background task executor
    bind<ScheduledExecutorService>("background") with singleton {
        val threadNumber = AtomicInteger()
        Executors.newScheduledThreadPool(4) {
            Thread(it, "BackgroundExecutor-${threadNumber.getAndIncrement()}").apply {
                isDaemon = true
                priority = 3
            }
        }
    }


    // Origin specific dependencies

    // Peka2tv
    bind<Peka2tvApiClient>() with singleton {
        Peka2tvApiClient(
                instance<OkHttpClient>(),
                instance<ObjectMapper>(),
                instance<Configuration>().getString("peka2tv.api-url")
        )
    }
    bind<Peka2tvEmoticonBulkLoader>() with singleton { Peka2tvEmoticonBulkLoader(instance<Peka2tvApiClient>()) }
    bind<Peka2tvEmoticonLoadConfiguration>() with singleton {
        Peka2tvEmoticonLoadConfiguration(instance<Peka2tvEmoticonBulkLoader>())
    }
    bind<Peka2tvBadgeHandler>() with singleton { Peka2tvBadgeHandler(instance<BadgeFinder>()) }
    bind<Peka2tvEmoticonHandler>() with singleton { Peka2tvEmoticonHandler(instance<EmoticonFinder>()) }
    bind<Peka2tvChatClient>() with factory { channelNameAndId: Pair<String, Long> ->
        Peka2tvChatClient(
                channelName = channelNameAndId.first,
                channelId = channelNameAndId.second,
                socketIoUrl = instance<Configuration>().getString("peka2tv.socketio-url"),
                okHttpClient = instance<OkHttpClient>(),
                messageIdGenerator = instance<MessageIdGenerator>(),
                emoticonHandler = instance<Peka2tvEmoticonHandler>(),
                badgeHandler = instance<Peka2tvBadgeHandler>(),
                history = instance<ChatMessageHistory>(),
                callbacks = factory<Unit, ChatClientCallbacks>().invoke(Unit)
        )
    }


    // Twitch
    bind<TwitchApiClient>() with singleton {
        val config = instance<Configuration>()
        TwitchApiClient(
                httpClient = instance<OkHttpClient>(),
                objectMapper = instance<ObjectMapper>(),
                clientId = config.getString("twitch.client-id"),
                clientSecret = config.getString("twitch.client-secret"),
                tokenContainer = ConfigurationTokenContainer(instance<Configuration>())
        )
    }
    bind<TwitchGlobalEmoticonLoader>() with singleton { TwitchGlobalEmoticonLoader(instance<TwitchApiClient>()) }
    bind<TwitchEmoticonLoadConfiguration>() with singleton {
        TwitchEmoticonLoadConfiguration(
                instance<TwitchGlobalEmoticonLoader>()
        )
    }
    bind<TwitchChatClient>() with factory { channelName: String ->
        val config = instance<Configuration>()
        TwitchChatClient(
                userName = channelName,
                ircAddress = config.getString("twitch.irc-address"),
                ircPort = config.getInt("twitch.irc-port"),
                botName = config.getString("twitch.bot-name"),
                botPassword = config.getString("twitch.bot-password"),
                twitchEmoticonHandler = instance<TwitchEmoticonHandler>(),
                messageIdGenerator = instance<MessageIdGenerator>(),
                bttvEmoticonHandler = instance<BttvEmoticonHandler>(),
                ffzEmoticonHandler = instance<FfzEmoticonHandler>(),
                sevenTvGlobalEmoticonHandler = instance<MessageHandler<ChatMessage>>(tag = "7tvGlobal"),
                sevenTvChannelEmoticonHandler = instance<MessageHandler<ChatMessage>>(tag = "7tvChannel"),
                twitchBadgeHandler = instance<TwitchBadgeHandler>(),
                history = instance<ChatMessageHistory>(),
                callbacks = factory<Unit, ChatClientCallbacks>().invoke(Unit)
        )
    }
    bind<TwitchViewersCountLoader>() with factory { channelName: String ->
        TwitchViewersCountLoader(channelName, instance<TwitchApiClient>())
    }
    bind<TwitchBadgeHandler>() with singleton {
        TwitchBadgeHandler(instance<BadgeFinder>())
    }
    bind<TwitchEmoticonHandler>() with singleton {
        TwitchEmoticonHandler(instance<TwitchEmotesTagParser>())
    }
    bind<TwitchEmotesTagParser>() with singleton {
        TwitchEmotesTagParser()
    }
    bind<TwitchEmoticonFactory>() with singleton {
        TwitchEmoticonFactory()
    }

    // BTTV
    bind<BttvApiClient>() with singleton {
        BttvApiClient(
                httpClient = instance<OkHttpClient>(),
                apiUrl = instance<Configuration>().getString("bttv.api-url"),
                objectMapper = instance<ObjectMapper>()
        )
    }
    bind<BttvEmoticonHandler>() with singleton { BttvEmoticonHandler(instance<EmoticonFinder>()) }
    bind<BttvGlobalEmoticonBulkLoader>() with singleton { BttvGlobalEmoticonBulkLoader(instance<BttvApiClient>()) }
    bind<BttvGlobalEmoticonLoadConfiguration>() with singleton {
        BttvGlobalEmoticonLoadConfiguration(instance<BttvGlobalEmoticonBulkLoader>())
    }

    // FFZ
    bind<FfzApiClient>() with singleton {
        FfzApiClient(
                httpClient = instance<OkHttpClient>(),
                apiUrl = instance<Configuration>().getString(ConfigKeys.frankerfacezApiUrl),
                objectMapper = instance<ObjectMapper>()
        )
    }
    bind<FfzEmoticonHandler>() with singleton {
        FfzEmoticonHandler(instance<EmoticonFinder>())
    }

    // 7tv
    bind<SevenTvApiClient>() with singleton {
        SevenTvApiClient(
                httpClient = instance<OkHttpClient>(),
                objectMapper = instance<ObjectMapper>()
        )
    }
    bind<MessageHandler<ChatMessage>>(tag = "7tvGlobal") with singleton {
        SpaceSeparatedEmoticonHandler(Origin.SEVEN_TV_GLOBAL, instance<EmoticonFinder>())
    }
    bind<MessageHandler<ChatMessage>>(tag = "7tvChannel") with singleton {
        SpaceSeparatedEmoticonHandler(Origin.SEVEN_TV_CHANNEL, instance<EmoticonFinder>())
    }
    bind<SevenTvGlobalEmoticonLoader>() with singleton { SevenTvGlobalEmoticonLoader(instance<SevenTvApiClient>()) }
    bind<SevenTvGlobalEmoticonLoadConfiguration>() with singleton {
        SevenTvGlobalEmoticonLoadConfiguration(instance<SevenTvGlobalEmoticonLoader>())
    }

    // Goodgame
    bind<GgChatClient>() with factory { channel: GgChannel ->
        GgChatClient(
                channel = channel,
                webSocketUri = instance<Configuration>().getString("goodgame.ws-url"),
                messageIdGenerator = instance<MessageIdGenerator>(),
                emoticonHandler = instance<GgEmoticonHandler>(),
                badgeHandler = factory<GgChannel, GgBadgeHandler>().invoke(channel),
                history = instance<ChatMessageHistory>(),
                callbacks = factory<Unit, ChatClientCallbacks>().invoke(Unit),
                objectMapper = instance<ObjectMapper>()
        )
    }
    bind<GgViewersCountLoader>() with factory { channelName: String ->
        GgViewersCountLoader(
                instance<GgApi2Client>(),
                channelName
        )
    }
    bind<GgApiClient>() with singleton {
        GgApiClient(
                httpClient = instance<OkHttpClient>(),
                apiUrl = instance<Configuration>().getString("goodgame.api-url"),
                emoticonsJsUrl = instance<Configuration>().getString("goodgame.emoticon-js-url"),
                objectMapper = instance<ObjectMapper>()
        )
    }
    bind<GgApi2Client>() with singleton {
        GgApi2Client(
                httpClient = instance<OkHttpClient>(),
                objectMapper = instance<ObjectMapper>(),
                apiUrl = instance<Configuration>().getString("goodgame.api2-url")
        )
    }
    bind<GgEmoticonBulkLoader>() with singleton { GgEmoticonBulkLoader(instance<GgApiClient>()) }
    bind<GgEmoticonLoadConfiguration>() with singleton { GgEmoticonLoadConfiguration(instance<GgEmoticonBulkLoader>()) }
    bind<GgEmoticonHandler>() with singleton { GgEmoticonHandler(instance<EmoticonFinder>()) }
    bind<GgBadgeHandler>() with factory { channel: GgChannel ->
        GgBadgeHandler(channel, instance<Configuration>())
    }


    bind<HttpClient>() with singleton {
        HttpClient(OkHttp) {
            engine {
                preconfigured = instance<OkHttpClient>()
            }
        }
    }

    // Youtube
    bind<YoutubeClient>() with singleton {
        YoutubeClient(
                instance<HttpClient>(),
                instance<ObjectMapper>(),
                instance<YoutubeHtmlParser>()
        )
    }
    bind<YoutubeHtmlParser>() with singleton {
        YoutubeHtmlParser(instance<ObjectMapper>())
    }
    bind<YoutubeChatClient>() with factory { videoId: String ->
        YoutubeChatClient(
                factory<Unit, ChatClientCallbacks>().invoke(Unit),
                instance<YoutubeClient>(),
                instance<MessageIdGenerator>(),
                instance<ChatMessageHistory>(),
                videoId
        )
    }

}

fun initMessagePipeline(kodein: DirectDI): OnChatMessageCallback {
    //todo build handler pipeline for each start
    val config = kodein.instance<Configuration>()

    val handlers = mutableListOf(
            LinkHandler(),
            kodein.instance<ImageLinkHandler>(),
            EmojiHandler(),
            kodein.instance<FailchatEmoticonHandler>()
    )
    if (config.getBoolean(ConfigKeys.saveMessageHistory)) {
        handlers += kodein.instance<ChatHistoryLogger>()
    }

    return OnChatMessageCallback(
            listOf(kodein.instance<IgnoreFilter>()),
            handlers,
            kodein.instance<ChatMessageHistory>(),
            kodein.instance<ChatMessageSender>()
    )
}
