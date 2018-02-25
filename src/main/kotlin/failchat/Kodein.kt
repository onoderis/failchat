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
import failchat.chat.handlers.IgnoreFilter
import failchat.chat.handlers.ImageLinkHandler
import failchat.emoticon.EmoticonFinder
import failchat.emoticon.EmoticonManager
import failchat.emoticon.EmoticonStorage
import failchat.github.GithubClient
import failchat.github.ReleaseChecker
import failchat.goodgame.GgApiClient
import failchat.goodgame.GgChatClient
import failchat.goodgame.GgEmoticonLoader
import failchat.gui.GuiEventHandler
import failchat.peka2tv.Peka2tvApiClient
import failchat.peka2tv.Peka2tvChatClient
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
import failchat.twitch.TwitchChatClient
import failchat.twitch.TwitchEmoticonLoader
import failchat.twitch.TwitchViewersCountLoader
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


    //Background task executor
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
    bind<Peka2tvChatClient>() with factory { channelNameAndId: Pair<String, Long> ->
        Peka2tvChatClient(
                channelName = channelNameAndId.first,
                channelId = channelNameAndId.second,
                socketIoUrl = instance<Configuration>().getString("peka2tv.socketio-url"),
                okHttpClient = instance<OkHttpClient>(),
                messageIdGenerator = instance<MessageIdGenerator>(),
                emoticonFinder = instance<EmoticonFinder>()
        )
    }


    // Twitch
    bind<TwitchApiClient>() with singleton {
        val config = instance<Configuration>()
        TwitchApiClient(
                httpClient = instance<OkHttpClient>(),
                apiUrl = config.getString("twitch.api-url"),
                token = config.getString("twitch.api-token"),
                objectMapper = instance<ObjectMapper>()
        )
    }
    bind<TwitchEmoticonLoader>() with singleton { TwitchEmoticonLoader(instance<TwitchApiClient>()) }
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
                bttvEmoticonHandler = instance<BttvEmoticonHandler>()
        )
    }
    bind<TwitchViewersCountLoader>() with factory { channelName: String ->
        TwitchViewersCountLoader(channelName, instance<TwitchApiClient>())
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
    bind<GgChatClient>() with factory { channelNameAndId: Pair<String, Long> ->
        GgChatClient(
                channelName = channelNameAndId.first,
                channelId = channelNameAndId.second,
                webSocketUri = instance<Configuration>().getString("goodgame.ws-url"),
                messageIdGenerator = instance<MessageIdGenerator>(),
                emoticonFinder = instance<EmoticonFinder>(),
                objectMapper = instance<ObjectMapper>()
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
    bind<GgEmoticonLoader>() with singleton { GgEmoticonLoader(instance<GgApiClient>()) }


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


}
