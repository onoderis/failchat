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
import failchat.core.reporter.UserIdLoader
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
import failchat.twitch.BttvGlobalEmoticonLoader
import failchat.twitch.TwitchEmoticonLoader
import javafx.application.Application
import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.ServerSocket
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

object Main

private val log: Logger = LoggerFactory.getLogger(Main::class.java)

fun main(args: Array<String>) {
    checkForAnotherInstance()

    configureLogging(args)

    handleProgramArguments(args)

    // Start GUI
    // Тред блокируется. Javafx приложение лучше запустить раньше(а не а конце main()) для отзывчивости интерфейса
    thread(name = "GuiLauncher") { Application.launch(GuiLauncher::class.java) }


    // Websocket server
    val wsServer: WsServer = kodein.instance()
    val objectMapper: ObjectMapper = kodein.instance()
    val config: Configuration = kodein.instance()
    wsServer.apply {
        setOnMessage("enabled-origins", EnabledOriginsWsHandler(config, objectMapper))
        setOnMessage("viewers-count", kodein.instance<ViewersCountWsHandler>())
        setOnMessage("show-viewers-count", ShowViewersCountWsHandler(config, objectMapper))
        setOnMessage("delete-message", DeleteWsMessageHandler(kodein.instance<ChatMessageRemover>()))
        setOnMessage("ignore-author", IgnoreWsMessageHandler(kodein.instance<IgnoreFilter>(), config))
    }
    wsServer.start()


    // Save user id to config/home file
    val userId = kodein.instance<String>("userId")
    kodein.instance<UserIdLoader>().saveUserId(userId)


    // Reporter
    val backgroundExecutor = kodein.instance<ScheduledExecutorService>("background")
    scheduleReportTasks(backgroundExecutor)


    // Load emoticons in background thread
    loadEmoticonsAsync(backgroundExecutor)


    log.info("Application started. Working directory: {}", kodein.instance<Path>("workingDirectory").toAbsolutePath())
}

private fun checkForAnotherInstance() {
    val sAddress = TtnWsServer.defaultAddress
    try {
        val serverSocket = ServerSocket(sAddress.port, 10, sAddress.address)
        serverSocket.close()
    } catch (e: Exception) {
        System.err.println("Another instance is running on $sAddress")
        System.exit(0)
    }
}

private fun handleProgramArguments(args: Array<String>) {
    //todo make it in a good way
    val config: Configuration = kodein.instance()
    if (args.contains("--disable-release-checker")) {
        config.setProperty("update-checker.enabled", false)
    }
    if (args.contains("--disable-reporter")) {
        config.setProperty("reporter.enabled", false)
    }
}

private fun loadEmoticonsAsync(executor: ExecutorService) = executor.submit {
    val manager: EmoticonManager = kodein.instance()
    val storage: EmoticonStorage = kodein.instance()
    val loadersAndOptions: List<Pair<EmoticonLoader<out Emoticon>, EmoticonStoreOptions>> = listOf(
            kodein.instance<Peka2tvEmoticonLoader>() to EmoticonStoreOptions(true, false),
            kodein.instance<GgEmoticonLoader>() to EmoticonStoreOptions(true, false),
            kodein.instance<BttvGlobalEmoticonLoader>() to EmoticonStoreOptions(true, false),
            kodein.instance<TwitchEmoticonLoader>() to EmoticonStoreOptions(true, true)
    )

    loadersAndOptions.forEach {
        try {
            manager.loadInStorage(storage, it.first, it.second)
        } catch (e: Exception) {
            log.warn("Exception during loading emoticons for origin {}", it.first.origin, e)
        }
    }
}

/**
 * Посылает репорт General.AppLaunch и репортит по расписанию General.Heartbeat.
 * */
private fun scheduleReportTasks(executor: ScheduledExecutorService) {
    val reporter = kodein.instance<EventReporter>()

    val exceptionHandler = { e: Throwable ->
        log.warn("Failed to report event {}.{}", EventCategory.General.name, EventAction.AppLaunch.name, e)
    }
    executor.execute {
        reporter
                .reportEvent(EventCategory.General, EventAction.AppLaunch)
                .exceptionally(exceptionHandler)
    }
    //todo don't start task if reporter disabled
    executor.scheduleAtFixedRate({
        reporter
                .reportEvent(EventCategory.General, EventAction.Heartbeat)
                .exceptionally(exceptionHandler)
    }, 5, 5, TimeUnit.MINUTES)
}
