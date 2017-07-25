package failchat.core


import com.fasterxml.jackson.databind.ObjectMapper
import com.github.salomonbrys.kodein.instance
import failchat.core.chat.ChatMessageRemover
import failchat.core.chat.handlers.IgnoreFilter
import failchat.core.emoticon.Emoticon
import failchat.core.emoticon.EmoticonLoader
import failchat.core.emoticon.EmoticonManager
import failchat.core.emoticon.EmoticonStorage
import failchat.core.emoticon.EmoticonStoreOptions
import failchat.core.reporter.EventAction
import failchat.core.reporter.EventCategory
import failchat.core.reporter.EventReporter
import failchat.core.skin.Skin
import failchat.core.viewers.ShowViewersCountWsHandler
import failchat.core.viewers.ViewersCountWsHandler
import failchat.core.ws.server.DeleteWsMessageHandler
import failchat.core.ws.server.EnabledOriginsWsHandler
import failchat.core.ws.server.IgnoreWsMessageHandler
import failchat.core.ws.server.TtnWsServer
import failchat.core.ws.server.WsServer
import failchat.goodgame.GgEmoticonLoader
import failchat.gui.GuiLauncher
import failchat.peka2tv.Peka2tvEmoticonLoader
import failchat.twitch.TwitchEmoticonLoader
import javafx.application.Application
import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ServerSocket
import java.nio.file.Path
import kotlin.concurrent.thread

object Bootstrap

private val log: Logger = LoggerFactory.getLogger(Bootstrap::class.java)

fun main(args: Array<String>) {
    checkForAnotherInstance()

    configureLogging()

    log.info("Working directory: {}", kodein.instance<Path>("workingDirectory").toAbsolutePath())

    // Get common dependencies
    val configLoader: ConfigLoader = kodein.instance()
    val config: Configuration = kodein.instance()
    val objectMapper: ObjectMapper = kodein.instance()

    // Scan skins
    kodein.instance<List<Skin>>()


    // Initialize websocket server
    val wsServer: WsServer = kodein.instance()


    // Initialize and set websocket message handlers
    wsServer.apply {
        setOnMessage("enabled-origins", EnabledOriginsWsHandler(config, objectMapper))
        setOnMessage("viewers-count", kodein.instance<ViewersCountWsHandler>())
        setOnMessage("show-viewers-count", ShowViewersCountWsHandler(config, objectMapper))
        setOnMessage("delete-message", DeleteWsMessageHandler(kodein.instance<ChatMessageRemover>()))
        setOnMessage("ignore-user", IgnoreWsMessageHandler(kodein.instance<IgnoreFilter>(), config))
    }


    //Start websocket server
    wsServer.start()


    // Load emoticons in background thread
    startEmoticonLoaderThread()

    // Report about start of application
    kodein.instance<EventReporter>()
            .reportEvent(EventCategory.general, EventAction.start)
            .exceptionally { e ->
                log.warn("Failed to report event {}.{}", EventCategory.general.name, EventAction.start.name, e)
            }

    Application.launch(GuiLauncher::class.java) //todo research: launch is blocking
}

private fun checkForAnotherInstance() {
    try {
        val serverSocket = ServerSocket(TtnWsServer.defaultAddress.port)
        serverSocket.close()
    } catch (e: Exception) {
        System.exit(0)
    }
}

private fun startEmoticonLoaderThread() {
    // todo is kodein class thread safe?
    val manager: EmoticonManager = kodein.instance()
    val storage: EmoticonStorage = kodein.instance()
    val loadersAndOptions: List<Pair<EmoticonLoader<out Emoticon>, EmoticonStoreOptions>> = listOf(
            kodein.instance<Peka2tvEmoticonLoader>() to EmoticonStoreOptions(true, false),
            kodein.instance<GgEmoticonLoader>() to EmoticonStoreOptions(true, false),
            kodein.instance<TwitchEmoticonLoader>() to EmoticonStoreOptions(true, true)
    )

    thread(start = true, name = "SmileLoaderThread", priority = 3) {
        loadersAndOptions.forEach {
            try {
                manager.loadInStorage(storage, it.first, it.second)
            } catch (e: Exception) {
                log.warn("Exception during loading emoticons for origin {}", it.first.origin, e)
            }
        }
    }
}
