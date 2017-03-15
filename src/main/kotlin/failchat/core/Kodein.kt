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
import failchat.core.emoticon.EmoticonManager
import failchat.core.skin.Skin
import failchat.core.skin.SkinScanner
import failchat.core.viewers.ViewersCounter
import failchat.core.ws.server.TtnWsServer
import failchat.core.ws.server.WsServer
import failchat.goodgame.GgApiClient
import failchat.goodgame.GgChatClient
import failchat.goodgame.GgEmoticonLoader
import failchat.goodgame.GgViewersCountLoader
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
    bind<WsServer>() with singleton { TtnWsServer(instance()) }


    // Core dependencies
    bind<AppStateTransitionManager>() with singleton { AppStateTransitionManager(kodein) }
    bind<ConfigLoader>() with singleton { ConfigLoader(instance<Path>("workingDirectory")) }
    bind<CompositeConfiguration>() with singleton { instance<ConfigLoader>().get() }
    bind<EmoticonManager>() with singleton {
        EmoticonManager(instance("workingDirectory"), instance(),
                listOf(
                        instance<Peka2tvEmoticonLoader>(),
                        instance<GgEmoticonLoader>(),
                        instance<TwitchEmoticonLoader>()
                ))
    }
    bind<ChatMessageSender>() with singleton {
        ChatMessageSender(instance(), instance(), instance(), instance(), instance())
    }
    bind<ChatMessageRemover>() with singleton {
        ChatMessageRemover(instance(), instance())
    }
    bind<ViewersCounter>() with singleton {
        ViewersCounter(instance(), instance(), instance())
    }
    bind<GuiEventHandler>() with singleton { GuiEventHandler(instance()) }


    // General purpose dependencies
    bind<ObjectMapper>() with singleton { ObjectMapper() }
    bind<OkHttpClient>() with singleton { OkHttpClient() }


    // Handlers and filters
    bind<IgnoreFilter>() with singleton { IgnoreFilter(instance()) }
    bind<ImageLinkHandler>() with singleton { ImageLinkHandler(instance()) }


    // Etc
    bind<Path>("workingDirectory") with singleton { Paths.get("") }
    bind<MessageIdGenerator>() with singleton { MessageIdGenerator(instance<CompositeConfiguration>().getLong("lastId")) }
    bind<List<Skin>>() with singleton { SkinScanner(instance("workingDirectory")).scan() }



    // Origin specific dependencies

    // Peka2tv
    bind<Peka2tvApiClient>() with singleton { Peka2tvApiClient(instance(), "http://peka2.tv/api", instance()) }
    bind<Peka2tvEmoticonLoader>() with singleton { Peka2tvEmoticonLoader(instance()) }
    bind<Peka2tvChatClient>() with factory { channelNameAndId: Pair<String, Long> ->
        Peka2tvChatClient(
                channelName = channelNameAndId.first,
                channelId = channelNameAndId.second,
                socketIoUrl = instance<CompositeConfiguration>().getString("peka2tv.socketio-url"),
                messageIdGenerator = instance(),
                emoticonManager = instance()
        )
    }


    // Twitch
    bind<TwitchApiClient>() with singleton {
        val config = instance<CompositeConfiguration>()
        TwitchApiClient(
                httpClient = instance(),
                apiUrl = config.getString("twitch.api-url"),
                token = config.getString("twitch.api-token"),
                objectMapper = instance()
        )
    }
    bind<TwitchEmoticonLoader>() with singleton { TwitchEmoticonLoader(instance()) }
    bind<TwitchChatClient>() with factory { channelName: String ->
        val config = instance<CompositeConfiguration>()
        TwitchChatClient(
                userName = channelName,
                ircAddress = config.getString("twitch.api-url"),
                ircPort = config.getInt("twitch.api-url"),
                botName = config.getString("twitch.api-url"),
                botPassword = config.getString("twitch.api-url"),
                emoticonManager = instance(),
                messageIdGenerator = instance()
        )
    }
    bind<TwitchViewersCountLoader>() with factory { channelName: String ->
        TwitchViewersCountLoader(channelName, instance())
    }


    // Goodgame
    bind<GgChatClient>() with factory { channelNameAndId: Pair<String, Long> ->
        GgChatClient(
                channelName = channelNameAndId.first,
                channelId = channelNameAndId.second,
                webSocketUri = instance<CompositeConfiguration>().getString("goodgame.ws-url"),
                messageIdGenerator = instance(),
                emoticonManager = instance(),
                objectMapper = instance()
        )
    }
    bind<GgApiClient>() with singleton {
        GgApiClient(
                httpClient = instance(),
                apiUrl = instance<CompositeConfiguration>().getString("goodgame.api-url"),
                emoticonsJsUrl = instance<CompositeConfiguration>().getString("goodgame.emoticon-js-url"),
                objectMapper = instance()
        )
    }
    bind<GgEmoticonLoader>() with singleton { GgEmoticonLoader(instance()) }
    bind<GgViewersCountLoader>() with factory { channelName: String ->
        GgViewersCountLoader(channelName, instance())
    }

}
