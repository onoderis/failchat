package failchat

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.factory
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import com.google.api.services.youtube.YouTube
import either.Either
import failchat.chat.ChatMessageRemover
import failchat.chat.ChatMessageSender
import failchat.chat.MessageIdGenerator
import failchat.chat.badge.BadgeFinder
import failchat.chat.badge.BadgeManager
import failchat.chat.badge.BadgeStorage
import failchat.chat.handlers.IgnoreFilter
import failchat.chat.handlers.ImageLinkHandler
import failchat.cybergame.CgApiClient
import failchat.cybergame.CgChatClient
import failchat.cybergame.CgViewersCountLoader
import failchat.emoticon.EmoticonFinder
import failchat.emoticon.EmoticonManager
import failchat.emoticon.EmoticonStorage
import failchat.github.GithubClient
import failchat.github.ReleaseChecker
import failchat.goodgame.GgApiClient
import failchat.goodgame.GgBadgeHandler
import failchat.goodgame.GgChannel
import failchat.goodgame.GgChatClient
import failchat.goodgame.GgEmoticonHandler
import failchat.goodgame.GgEmoticonLoader
import failchat.gui.GuiEventHandler
import failchat.peka2tv.Peka2tvApiClient
import failchat.peka2tv.Peka2tvBadgeHandler
import failchat.peka2tv.Peka2tvChatClient
import failchat.peka2tv.Peka2tvEmoticonHandler
import failchat.peka2tv.Peka2tvEmoticonLoader
import failchat.reporter.EventReporter
import failchat.reporter.GAEventReporter
import failchat.reporter.ToggleEventReporter
import failchat.reporter.UserIdManager
import failchat.skin.Skin
import failchat.skin.SkinScanner
import failchat.twitch.BttvApiClient
import failchat.twitch.BttvEmoticonHandler
import failchat.twitch.BttvGlobalEmoticonLoader
import failchat.twitch.TwitchApiClient
import failchat.twitch.TwitchBadgeHandler
import failchat.twitch.TwitchChatClient
import failchat.twitch.TwitchEmoticonLoader
import failchat.twitch.TwitchEmoticonUrlFactory
import failchat.twitch.TwitchViewersCountLoader
import failchat.twitch.TwitchemotesApiClient
import failchat.viewers.ViewersCountLoader
import failchat.viewers.ViewersCountWsHandler
import failchat.viewers.ViewersCounter
import failchat.ws.server.TtnWsServer
import failchat.ws.server.WsServer
import failchat.youtube.ChannelId
import failchat.youtube.VideoId
import failchat.youtube.YouTubeFactory
import failchat.youtube.YtApiClient
import failchat.youtube.YtChatClient
import okhttp3.OkHttpClient
import org.apache.commons.configuration2.Configuration
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicInteger

@Suppress("RemoveExplicitTypeArguments")
val kodein = Kodein {

    // Websocket server
    bind<WsServer>() with singleton { TtnWsServer(wsServerAddress, instance<ObjectMapper>()) }
    bind<ViewersCountWsHandler>() with singleton {
        ViewersCountWsHandler(instance<Configuration>())
    }

    // Core dependencies
    bind<AppStateManager>() with singleton { AppStateManager(kodein) }
    bind<ConfigLoader>() with singleton { ConfigLoader(instance<Path>("homeDirectory")) }
    bind<Configuration>() with singleton { instance<ConfigLoader>().get() }
    bind<ChatMessageSender>() with singleton {
        ChatMessageSender(
                instance<WsServer>(),
                instance<Configuration>(),
                instance<IgnoreFilter>(),
                instance<ImageLinkHandler>()
        )
    }
    bind<ChatMessageRemover>() with singleton {
        ChatMessageRemover(instance<WsServer>())
    }
    bind<ViewersCounter>() with factory { vcLoaders: List<ViewersCountLoader> ->
        ViewersCounter(
                vcLoaders,
                instance<WsServer>()
        )
    }
    bind<GuiEventHandler>() with singleton {
        GuiEventHandler(
                instance<AppStateManager>(),
                instance<ChatMessageSender>()
        )
    }

    // Emoticons
    bind<EmoticonStorage>() with singleton { EmoticonStorage() }
    bind<EmoticonFinder>() with singleton { instance<EmoticonStorage>() }
    bind<EmoticonManager>() with singleton {
        EmoticonManager(instance<Path>("workingDirectory"), instance<Configuration>())
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


    // Handlers and filters
    bind<IgnoreFilter>() with singleton { IgnoreFilter(instance<Configuration>()) }
    bind<ImageLinkHandler>() with singleton { ImageLinkHandler(instance<Configuration>()) }


    // Etc
    bind<Path>("workingDirectory") with singleton { Paths.get("") }
    bind<Path>("homeDirectory") with singleton { Paths.get(System.getProperty("user.home")).resolve(".failchat") }
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
        Executors.newScheduledThreadPool(2) {
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
                instance<Configuration>().getString("peka2tv.api-url"),
                instance<ObjectMapper>()
        )
    }
    bind<Peka2tvEmoticonLoader>() with singleton { Peka2tvEmoticonLoader(instance<Peka2tvApiClient>()) }
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
                badgeHandler = instance<Peka2tvBadgeHandler>()
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
    bind<TwitchEmoticonLoader>() with singleton {
        TwitchEmoticonLoader(instance<TwitchApiClient>(), instance<TwitchemotesApiClient>())
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
                twitchBadgeHandler = instance<TwitchBadgeHandler>()
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
    bind<BttvGlobalEmoticonLoader>() with singleton { BttvGlobalEmoticonLoader(instance<BttvApiClient>()) }

    // Goodgame
    bind<GgChatClient>() with factory { channel: GgChannel ->
        GgChatClient(
                channelName = channel.name,
                channelId = channel.id,
                webSocketUri = instance<Configuration>().getString("goodgame.ws-url"),
                messageIdGenerator = instance<MessageIdGenerator>(),
                emoticonHandler = instance<GgEmoticonHandler>(),
                badgeHandler = factory<GgChannel, GgBadgeHandler>().invoke(channel)
        )
    }
    bind<GgApiClient>() with singleton {
        GgApiClient(
                httpClient = instance<OkHttpClient>(),
                apiUrl = instance<Configuration>().getString("goodgame.api-url"),
                emoticonsJsUrl = instance<Configuration>().getString("goodgame.emoticon-js-url")
        )
    }
    bind<GgEmoticonLoader>() with singleton { GgEmoticonLoader(instance<GgApiClient>()) }
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
                instance<MessageIdGenerator>()
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
                messageIdGenerator = instance<MessageIdGenerator>()
        )
    }
    bind<CgViewersCountLoader>() with factory { channelName: String ->
        CgViewersCountLoader(instance<CgApiClient>(), channelName)
    }

}
