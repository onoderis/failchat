package failchat.core


import com.github.salomonbrys.kodein.instance
import failchat.core.emoticon.EmoticonManager
import failchat.core.skin.Skin
import failchat.core.viewers.ViewersCountHandler
import failchat.core.ws.server.DeleteWsMessageHandler
import failchat.core.ws.server.IgnoreWsMessageHandler
import failchat.core.ws.server.TtnWsServer
import failchat.core.ws.server.WsServer
import failchat.gui.GuiLauncher
import javafx.application.Application
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ServerSocket
import java.nio.file.Path
import kotlin.concurrent.thread

object Bootstrap

private val logger: Logger = LoggerFactory.getLogger(Bootstrap::class.java)

fun main(args: Array<String>) {
    checkForAnotherInstance()

    configureLogging()

    logger.info("Working directory: {}", kodein.instance<Path>("workingDirectory").toAbsolutePath())


    // Scan skins
    kodein.instance<List<Skin>>()

    // Initialize websocket server
    val wsServer: WsServer = kodein.instance()

    // Set websocket message handlers
    val ignoreWsMessageHandler = IgnoreWsMessageHandler(
            kodein.instance(),
            kodein.instance(),
            kodein.instance<ConfigLoader>().get()
    )
    val deleteWsMessageHandler = DeleteWsMessageHandler(kodein.instance())
    val viewersCountHandler = ViewersCountHandler(kodein.instance())

    wsServer.setOnMessage("ignore", ignoreWsMessageHandler)
    wsServer.setOnMessage("delete-message", deleteWsMessageHandler)
    wsServer.setOnMessage("viewers", viewersCountHandler)

    //Start websocket server
    wsServer.start()


    // Load emoticons in background thread
    val emoticonManager: EmoticonManager = kodein.instance()
    thread(start = true, name = "SmileLoaderThread", priority = 3) {
        emoticonManager.loadEmoticons()
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
