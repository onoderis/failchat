package failchat.core


import com.fasterxml.jackson.databind.ObjectMapper
import com.github.salomonbrys.kodein.instance
import failchat.core.chat.ChatMessageRemover
import failchat.core.chat.handlers.IgnoreFilter
import failchat.core.emoticon.EmoticonManager
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
import failchat.gui.GuiLauncher
import javafx.application.Application
import org.apache.commons.configuration.CompositeConfiguration
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
    val config: CompositeConfiguration = kodein.instance()
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
    val emoticonManager: EmoticonManager = kodein.instance()
    thread(start = true, name = "SmileLoaderThread", priority = 3) {
        emoticonManager.loadEmoticons()
    }

    kodein.instance<EventReporter>()
            .reportEvent(EventCategory.general, EventAction.start)
            .exceptionally { e ->
                log.warn("Failed to report event {}.{}", EventCategory.general.name, EventAction.start.name, e)
                null
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
