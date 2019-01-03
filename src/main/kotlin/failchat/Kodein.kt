package failchat

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.factory
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import com.google.api.services.youtube.YouTube
import either.Either
import failchat.chat.ChatClientCallbacks
import failchat.chat.ChatMessageHistory
import failchat.chat.ChatMessageRemover
import failchat.chat.ChatMessageSender
import failchat.chat.MessageIdGenerator
import failchat.chat.OnChatMessageCallback
import failchat.chat.OnChatMessageDeletedCallback
import failchat.chat.OnStatusUpdateCallback
import failchat.chat.OriginStatusManager
import failchat.chat.badge.BadgeFinder
import failchat.chat.badge.BadgeManager
import failchat.chat.badge.BadgeStorage
import failchat.chat.handlers.FailchatEmoticonHandler
import failchat.chat.handlers.IgnoreFilter
import failchat.chat.handlers.ImageLinkHandler
import failchat.chat.handlers.LinkHandler
import failchat.chat.handlers.OriginsStatusHandler
import failchat.cybergame.CgApiClient
import failchat.cybergame.CgChatClient
import failchat.cybergame.CgViewersCountLoader
import failchat.emoticon.ChannelEmoticonUpdater
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
import failchat.goodgame.GgApiClient
import failchat.goodgame.GgBadgeHandler
import failchat.goodgame.GgChannel
import failchat.goodgame.GgChatClient
import failchat.goodgame.GgEmoticonBulkLoader
import failchat.goodgame.GgEmoticonHandler
import failchat.goodgame.GgEmoticonLoadConfiguration
import failchat.goodgame.GgViewersCountLoader
import failchat.gui.GuiEventHandler
import failchat.peka2tv.Peka2tvApiClient
import failchat.peka2tv.Peka2tvBadgeHandler
import failchat.peka2tv.Peka2tvChatClient
import failchat.peka2tv.Peka2tvEmoticonBulkLoader
import failchat.peka2tv.Peka2tvEmoticonHandler
import failchat.peka2tv.Peka2tvEmoticonLoadConfiguration
import failchat.reporter.EventReporter
import failchat.reporter.GAEventReporter
import failchat.reporter.ToggleEventReporter
import failchat.reporter.UserIdManager
import failchat.skin.Skin
import failchat.skin.SkinScanner
import failchat.twitch.BttvApiClient
import failchat.twitch.BttvEmoticonHandler
import failchat.twitch.BttvGlobalEmoticonBulkLoader
import failchat.twitch.BttvGlobalEmoticonLoadConfiguration
import failchat.twitch.FfzApiClient
import failchat.twitch.FfzEmoticonHandler
import failchat.twitch.TwitchApiClient
import failchat.twitch.TwitchBadgeHandler
import failchat.twitch.TwitchChatClient
import failchat.twitch.TwitchEmoticonLoadConfiguration
import failchat.twitch.TwitchEmoticonStreamLoader
import failchat.twitch.TwitchEmoticonUrlFactory
import failchat.twitch.TwitchViewersCountLoader
import failchat.twitch.TwitchemotesApiClient
import failchat.twitch.TwitchemotesStreamLoader
import failchat.viewers.ViewersCountLoader
import failchat.viewers.ViewersCountWsHandler
import failchat.viewers.ViewersCounter
import failchat.ws.server.ClientConfigurationWsHandler
import failchat.ws.server.DeleteWsMessageHandler
import failchat.ws.server.IgnoreWsMessageHandler
import failchat.ws.server.WsFrameSender
import failchat.ws.server.WsMessageDispatcher
import failchat.youtube.ChannelId
import failchat.youtube.VideoId
import failchat.youtube.YouTubeFactory
import failchat.youtube.YtApiClient
import failchat.youtube.YtChatClient
import io.ktor.server.engine.ApplicationEngine
import okhttp3.OkHttpClient
import org.apache.commons.configuration2.Configuration
import org.mapdb.DB
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicInteger

@Suppress("RemoveExplicitTypeArguments")
val kodein = Kodein {

    // Http/websocket server
    bind<ApplicationEngine>() with singleton { createHttpServer() }
    bind<WsMessageDispatcher>() with singleton {
        WsMessageDispatcher(listOf(
                ClientConfigurationWsHandler(instance<ChatMessageSender>()),
                instance<ViewersCountWsHandler>(),
                DeleteWsMessageHandler(instance<ChatMessageRemover>()),
                IgnoreWsMessageHandler(instance<IgnoreFilter>(), instance<Configuration>()),
                instance<OriginsStatusHandler>()
        ))
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
    bind<AppStateManager>() with singleton { AppStateManager(kodein) }
    bind<OriginStatusManager>() with singleton {
        OriginStatusManager(instance<ChatMessageSender>())
    }
    bind<ConfigLoader>() with singleton { ConfigLoader(instance<Path>("homeDirectory")) }
    bind<Configuration>() with singleton { instance<ConfigLoader>().load() }
    bind<ChatMessageSender>() with singleton {
        ChatMessageSender(
                instance<WsFrameSender>(),
                instance<Configuration>()
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
        GuiEventHandler(
                instance<AppStateManager>(),
                instance<ChatMessageSender>(),
                instance<Configuration>()
        )
    }
    bind<ChatMessageHistory>() with singleton { ChatMessageHistory(50) }

    // Emoticons
    bind<DB>("emoticons") with singleton { MapdbFactory.create(instance<Path>("emoticonDbFile")) }
    bind<EmoticonStorage>() with singleton {
        EmoticonStorage()
    }
    bind<EmoticonFinder>() with singleton { instance<EmoticonStorage>() }
    bind<EmoticonManager>() with singleton {
        EmoticonManager(
                instance<Configuration>(),
                instance<EmoticonStorage>(),
                instance<ScheduledExecutorService>("background")
        )
    }
    bind<List<EmoticonLoadConfiguration<out Emoticon>>>("emoticonLoadConfigurations") with singleton {
        listOf(
                instance<Peka2tvEmoticonLoadConfiguration>(),
                instance<GgEmoticonLoadConfiguration>(),
                instance<BttvGlobalEmoticonLoadConfiguration>(),
                instance<TwitchEmoticonLoadConfiguration>()
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
                instance<ScheduledExecutorService>("background")
        )
    }

    bind<FailchatEmoticonUpdater>() with singleton {
        FailchatEmoticonUpdater(
                instance<EmoticonStorage>(),
                instance<FailchatEmoticonScanner>(),
                instance<ScheduledExecutorService>("background")
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
    bind<ObjectMapper>() with singleton { ObjectMapper() }
    bind<OkHttpClient>() with singleton { OkHttpClient() }


    // Message handlers and filters
    bind<IgnoreFilter>() with singleton { IgnoreFilter(instance<Configuration>()) }
    bind<ImageLinkHandler>() with singleton { ImageLinkHandler() }
    bind<FailchatEmoticonHandler>() with singleton {
        FailchatEmoticonHandler(instance<EmoticonFinder>())
    }


    // Chat client callbacks
    bind<ChatClientCallbacks>() with singleton {
        ChatClientCallbacks(
                kodein.instance<OnChatMessageCallback>(),
                kodein.instance<OnStatusUpdateCallback>(),
                kodein.instance<OnChatMessageDeletedCallback>()
        )
    }
    bind<OnChatMessageCallback>() with singleton {
        OnChatMessageCallback(
                listOf(instance<IgnoreFilter>()),
                listOf(LinkHandler(), instance<ImageLinkHandler>(), instance<FailchatEmoticonHandler>()),
                instance<ChatMessageHistory>(),
                instance<ChatMessageSender>()
        )
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
    bind<Path>("homeDirectory") with singleton { Paths.get(System.getProperty("user.home")).resolve(".failchat") }
    bind<Path>("failchatEmoticonsDirectory") with singleton { instance<Path>("homeDirectory").resolve("failchat-emoticons") }
    bind<Path>("emoticonCacheDirectory") with singleton { instance<Path>("workingDirectory").resolve("emoticons") }
    bind<Path>("emoticonDbFile") with singleton { instance<Path>("emoticonCacheDirectory").resolve("emoticons.db") }
    bind<String>("failchatEmoticonsUrl") with singleton { "http://${FcServerInfo.host.hostAddress}:${FcServerInfo.port}/emoticons/" }
    bind<String>("userId") with singleton { instance<UserIdManager>().getUserId() }

    bind<MessageIdGenerator>() with singleton { MessageIdGenerator(instance<Configuration>().getLong("lastMessageId")) }
    bind<List<Skin>>() with singleton { SkinScanner(instance<Path>("workingDirectory")).scan() }
    bind<UserIdManager>() with singleton { UserIdManager(instance<Path>("homeDirectory")) }
    bind<EventReporter>() with singleton {
        val config = instance<Configuration>()
        ToggleEventReporter(
                GAEventReporter(
                        instance<OkHttpClient>(),
                        instance<String>("userId"),
                        config.getString("version"),
                        config.getString("reporter.tracking-id")
                ),
                config.getBoolean("reporter.enabled")
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
                callbacks = instance<ChatClientCallbacks>()
        )
    }


    // Twitch
    bind<TwitchApiClient>() with singleton {
        val config = instance<Configuration>()
        TwitchApiClient(
                httpClient = instance<OkHttpClient>(),
                mainApiUrl = config.getString("twitch.api-url"),
                badgeApiUrl = config.getString("twitch.badge-api-url"),
                token = config.getString("twitch.api-token"),
                emoticonUrlFactory = instance<TwitchEmoticonUrlFactory>()
        )
    }
    bind<TwitchEmoticonFactory>() with singleton {
        TwitchEmoticonFactory(instance<TwitchEmoticonUrlFactory>())
    }
    bind<TwitchEmoticonUrlFactory>() with singleton {
        with(instance<Configuration>()) {
            TwitchEmoticonUrlFactory(
                    getString("twitch.emoticon-url-prefix"),
                    getString("twitch.emoticon-url-suffix")
            )
        }
    }
    bind<TwitchemotesApiClient>() with singleton {
        TwitchemotesApiClient(
                instance<OkHttpClient>(),
                instance<Configuration>().getString("twitch.twitchemotes-api-url"),
                instance<TwitchEmoticonUrlFactory>()
        )
    }
    bind<TwitchEmoticonStreamLoader>() with singleton { TwitchEmoticonStreamLoader(instance<TwitchApiClient>()) }
    bind<TwitchemotesStreamLoader>() with singleton { TwitchemotesStreamLoader(instance<TwitchemotesApiClient>()) }
    bind<TwitchEmoticonLoadConfiguration>() with singleton {
        TwitchEmoticonLoadConfiguration(
                instance<TwitchEmoticonStreamLoader>(),
                instance<TwitchemotesStreamLoader>()
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
                emoticonFinder = instance<EmoticonFinder>(),
                messageIdGenerator = instance<MessageIdGenerator>(),
                bttvEmoticonHandler = instance<BttvEmoticonHandler>(),
                ffzEmoticonHandler = instance<FfzEmoticonHandler>(),
                twitchBadgeHandler = instance<TwitchBadgeHandler>(),
                history = instance<ChatMessageHistory>(),
                callbacks = instance<ChatClientCallbacks>()
        )
    }
    bind<TwitchViewersCountLoader>() with factory { channelName: String ->
        TwitchViewersCountLoader(channelName, instance<TwitchApiClient>())
    }
    bind<TwitchBadgeHandler>() with singleton {
        TwitchBadgeHandler(instance<BadgeFinder>())
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
                apiUrl = instance<Configuration>().getString(ConfigKeys.frankerfacezApiUrl)
        )
    }
    bind<FfzEmoticonHandler>() with singleton {
        FfzEmoticonHandler(instance<EmoticonFinder>())
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
                callbacks = instance<ChatClientCallbacks>()
        )
    }
    bind<GgViewersCountLoader>() with factory { channelName: String ->
        GgViewersCountLoader(
                instance<GgApiClient>(),
                channelName
        )
    }
    bind<GgApiClient>() with singleton {
        GgApiClient(
                httpClient = instance<OkHttpClient>(),
                apiUrl = instance<Configuration>().getString("goodgame.api-url"),
                emoticonsJsUrl = instance<Configuration>().getString("goodgame.emoticon-js-url")
        )
    }
    bind<GgEmoticonBulkLoader>() with singleton { GgEmoticonBulkLoader(instance<GgApiClient>()) }
    bind<GgEmoticonLoadConfiguration>() with singleton { GgEmoticonLoadConfiguration(instance<GgEmoticonBulkLoader>()) }
    bind<GgEmoticonHandler>() with singleton { GgEmoticonHandler(instance<EmoticonFinder>()) }
    bind<GgBadgeHandler>() with factory { channel: GgChannel ->
        GgBadgeHandler(channel, instance<Configuration>())
    }


    // Youtube
    bind<YouTube>() with singleton { YouTubeFactory.create(instance<Configuration>()) }
    bind<YtApiClient>() with singleton { YtApiClient(instance<YouTube>()) }
    bind<ScheduledExecutorService>("youtube") with singleton {
        Executors.newSingleThreadScheduledExecutor { Thread(it, "YoutubeExecutor") }
    }
    bind<YtChatClient>() with factory { channelIdOrVideoId: Either<ChannelId, VideoId> ->
        YtChatClient(
                channelIdOrVideoId,
                instance<YtApiClient>(),
                instance<ScheduledExecutorService>("youtube"),
                instance<MessageIdGenerator>(),
                instance<ChatMessageHistory>(),
                instance<ChatClientCallbacks>()
        )
    }

    // Cybergame
    bind<CgApiClient>() with singleton {
        CgApiClient(
                httpClient = instance<OkHttpClient>(),
                apiUrl = instance<Configuration>().getString("cybergame.api-url")
        )
    }
    bind<CgChatClient>() with factory { channelNameAndId: Pair<String, Long> ->
        CgChatClient(
                channelName = channelNameAndId.first,
                channelId = channelNameAndId.second,
                wsUrl = instance<Configuration>().getString("cybergame.ws-url"),
                emoticonUrlPrefix = instance<Configuration>().getString("cybergame.emoticon-url-prefix"),
                messageIdGenerator = instance<MessageIdGenerator>(),
                history = instance<ChatMessageHistory>(),
                callbacks = instance<ChatClientCallbacks>()
        )
    }
    bind<CgViewersCountLoader>() with factory { channelName: String ->
        CgViewersCountLoader(instance<CgApiClient>(), channelName)
    }

}
