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
import org.apache.commons.configuration.CompositeConfiguration
import java.nio.file.Path
import java.nio.file.Paths

val kodein = Kodein {

    // Websocket server
    bind<WsServer>() with singleton { TtnWsServer(instance<ObjectMapper>()) }
    bind<ViewersCountWsHandler>() with singleton {
        ViewersCountWsHandler(instance<CompositeConfiguration>(), instance<ObjectMapper>())
    }

    // Core dependencies
    bind<AppStateTransitionManager>() with singleton { AppStateTransitionManager(kodein) }
    bind<ConfigLoader>() with singleton { ConfigLoader(instance<Path>("workingDirectory")) }
    bind<CompositeConfiguration>() with singleton { instance<ConfigLoader>().get() }
    bind<ChatMessageSender>() with singleton {
        ChatMessageSender(
                instance<WsServer>(),
                instance<CompositeConfiguration>(),
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
        GuiEventHandler(instance<WsServer>(), instance<ObjectMapper>())
    }

    // Emoticons
    bind<EmoticonStorage>() with singleton { EmoticonStorage() }
    bind<EmoticonFinder>() with singleton { instance<EmoticonStorage>() }
    bind<EmoticonManager>() with singleton {
        EmoticonManager(instance("workingDirectory"), instance<CompositeConfiguration>())
    }


    // General purpose dependencies
    bind<ObjectMapper>() with singleton { ObjectMapper() }
    bind<OkHttpClient>() with singleton { OkHttpClient() }


    // Handlers and filters
    bind<IgnoreFilter>() with singleton { IgnoreFilter(instance<CompositeConfiguration>()) }
    bind<ImageLinkHandler>() with singleton { ImageLinkHandler(instance<CompositeConfiguration>()) }


    // Etc
    bind<Path>("workingDirectory") with singleton { Paths.get("") }
    bind<MessageIdGenerator>() with singleton { MessageIdGenerator(instance<CompositeConfiguration>().getLong("lastId")) }
    bind<List<Skin>>() with singleton { SkinScanner(instance("workingDirectory")).scan() }
    bind<EventReporter>() with singleton { EventReporter(instance<OkHttpClient>(), instance<ConfigLoader>()) }


    // Origin specific dependencies

    // Peka2tv
    bind<Peka2tvApiClient>() with singleton {
        Peka2tvApiClient(
                instance<OkHttpClient>(),
                "http://peka2.tv/api",
                instance<ObjectMapper>()
        )
    }
    bind<Peka2tvEmoticonLoader>() with singleton { Peka2tvEmoticonLoader(instance<Peka2tvApiClient>()) }
    bind<Peka2tvChatClient>() with factory { channelNameAndId: Pair<String, Long> ->
        Peka2tvChatClient(
                channelName = channelNameAndId.first,
                channelId = channelNameAndId.second,
                socketIoUrl = instance<CompositeConfiguration>().getString("peka2tv.socketio-url"),
                okHttpClient = instance<OkHttpClient>(),
                messageIdGenerator = instance<MessageIdGenerator>(),
                emoticonFinder = instance<EmoticonFinder>()
        )
    }


    // Twitch
    bind<TwitchApiClient>() with singleton {
        val config = instance<CompositeConfiguration>()
        TwitchApiClient(
                httpClient = instance<OkHttpClient>(),
                apiUrl = config.getString("twitch.api-url"),
                token = config.getString("twitch.api-token"),
                objectMapper = instance<ObjectMapper>()
        )
    }
    bind<TwitchEmoticonLoader>() with singleton { TwitchEmoticonLoader(instance<TwitchApiClient>()) }
    bind<TwitchChatClient>() with factory { channelName: String ->
        val config = instance<CompositeConfiguration>()
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
                webSocketUri = instance<CompositeConfiguration>().getString("goodgame.ws-url"),
                messageIdGenerator = instance<MessageIdGenerator>(),
                emoticonFinder = instance<EmoticonFinder>(),
                objectMapper = instance<ObjectMapper>()
        )
    }
    bind<GgApiClient>() with singleton {
        GgApiClient(
                httpClient = instance<OkHttpClient>(),
                apiUrl = instance<CompositeConfiguration>().getString("goodgame.api-url"),
                emoticonsJsUrl = instance<CompositeConfiguration>().getString("goodgame.emoticon-js-url"),
                objectMapper = instance<ObjectMapper>()
        )
    }
    bind<GgEmoticonLoader>() with singleton { GgEmoticonLoader(instance<GgApiClient>()) }

}
