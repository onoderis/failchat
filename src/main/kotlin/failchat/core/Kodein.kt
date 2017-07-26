package failchat.core

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.bind
import com.github.salomonbrys.kodein.factory
import com.github.salomonbrys.kodein.instance
import com.github.salomonbrys.kodein.singleton
import failchat.core.chat.ChatMessageRemover
import failchat.core.chat.ChatMessageSender
import failchat.core.chat.MessageIdGenerator
import failchat.core.chat.handlers.IgnoreFilter
import failchat.core.chat.handlers.ImageLinkHandler
import failchat.core.emoticon.EmoticonFinder
import failchat.core.emoticon.EmoticonManager
import failchat.core.emoticon.EmoticonStorage
import failchat.core.reporter.EventReporter
import failchat.core.skin.Skin
import failchat.core.skin.SkinScanner
import failchat.core.viewers.ViewersCountLoader
import failchat.core.viewers.ViewersCountWsHandler
import failchat.core.viewers.ViewersCounter
import failchat.core.ws.server.TtnWsServer
import failchat.core.ws.server.WsServer
import failchat.github.GithubClient
import failchat.github.ReleaseChecker
import failchat.goodgame.GgApiClient
import failchat.goodgame.GgChatClient
import failchat.goodgame.GgEmoticonLoader
import failchat.gui.GuiEventHandler
import failchat.peka2tv.Peka2tvApiClient
import failchat.peka2tv.Peka2tvChatClient
import failchat.peka2tv.Peka2tvEmoticonLoader
import failchat.twitch.TwitchApiClient
import failchat.twitch.TwitchChatClient
import failchat.twitch.TwitchEmoticonLoader
import failchat.twitch.TwitchViewersCountLoader
import okhttp3.OkHttpClient
import org.apache.commons.configuration2.Configuration
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicInteger

val kodein = Kodein {

    // Websocket server
    bind<WsServer>() with singleton { TtnWsServer(instance<ObjectMapper>()) }
    bind<ViewersCountWsHandler>() with singleton {
        ViewersCountWsHandler(instance<Configuration>(), instance<ObjectMapper>())
    }

    // Core dependencies
    bind<AppStateTransitionManager>() with singleton { AppStateTransitionManager(kodein) }
    bind<ConfigLoader>() with singleton { ConfigLoader(instance<Path>("workingDirectory")) }
    bind<Configuration>() with singleton { instance<ConfigLoader>().get() }
    bind<ChatMessageSender>() with singleton {
        ChatMessageSender(
                instance<WsServer>(),
                instance<Configuration>(),
                instance<IgnoreFilter>(),
                instance<ImageLinkHandler>(),
                instance<ObjectMapper>()
        )
    }
    bind<ChatMessageRemover>() with singleton {
        ChatMessageRemover(instance<WsServer>(), instance<ObjectMapper>())
    }
    bind<ViewersCounter>() with factory { vcLoaders: List<ViewersCountLoader> ->
        ViewersCounter(
                vcLoaders,
                instance<WsServer>(),
                instance<ObjectMapper>()
        )
    }
    bind<GuiEventHandler>() with singleton {
        GuiEventHandler(
                instance<WsServer>(),
                instance<AppStateTransitionManager>(),
                instance<ObjectMapper>()
        )
    }

    // Emoticons
    bind<EmoticonStorage>() with singleton { EmoticonStorage() }
    bind<EmoticonFinder>() with singleton { instance<EmoticonStorage>() }
    bind<EmoticonManager>() with singleton {
        EmoticonManager(instance("workingDirectory"), instance<Configuration>())
    }


    // General purpose dependencies
    bind<ObjectMapper>() with singleton { ObjectMapper() }
    bind<OkHttpClient>() with singleton { OkHttpClient() }


    // Handlers and filters
    bind<IgnoreFilter>() with singleton { IgnoreFilter(instance<Configuration>()) }
    bind<ImageLinkHandler>() with singleton { ImageLinkHandler(instance<Configuration>()) }


    // Etc
    bind<Path>("workingDirectory") with singleton { Paths.get("") }
    bind<MessageIdGenerator>() with singleton { MessageIdGenerator(instance<Configuration>().getLong("lastId")) }
    bind<List<Skin>>() with singleton { SkinScanner(instance("workingDirectory")).scan() }
    bind<EventReporter>() with singleton { EventReporter(instance<OkHttpClient>(), instance<ConfigLoader>()) }


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
    bind<ScheduledExecutorService>() with singleton {
        val threadNumber = AtomicInteger()
        Executors.newScheduledThreadPool(2) {
            Thread(it, "BackgroundTaskThread-${threadNumber.getAndIncrement()}").apply {
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
                messageIdGenerator = instance<MessageIdGenerator>()
        )
    }
    bind<TwitchViewersCountLoader>() with factory { channelName: String ->
        TwitchViewersCountLoader(channelName, instance<TwitchApiClient>())
    }


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

}
