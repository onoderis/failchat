package failchat

import com.github.salomonbrys.kodein.instance
import failchat.chat.ChatMessageRemover
import failchat.chat.ChatMessageSender
import failchat.chat.badge.BadgeManager
import failchat.chat.handlers.IgnoreFilter
import failchat.emoticon.Emoticon
import failchat.emoticon.EmoticonLoader
import failchat.emoticon.EmoticonManager
import failchat.emoticon.EmoticonStorage
import failchat.emoticon.EmoticonStoreOptions
import failchat.goodgame.GgEmoticonLoader
import failchat.gui.GuiLauncher
import failchat.peka2tv.Peka2tvEmoticonLoader
import failchat.reporter.EventAction
import failchat.reporter.EventCategory
import failchat.reporter.EventReporter
import failchat.twitch.BttvGlobalEmoticonLoader
import failchat.twitch.TwitchEmoticonLoader
import failchat.util.CoroutineExceptionLogger
import failchat.util.executeWithCatch
import failchat.util.sp
import failchat.viewers.ViewersCountWsHandler
import failchat.ws.server.ClientConfigurationWsHandler
import failchat.ws.server.DeleteWsMessageHandler
import failchat.ws.server.IgnoreWsMessageHandler
import failchat.ws.server.WsServer
import javafx.application.Application
import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.Unconfined
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import mu.KotlinLogging
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.configuration2.Configuration
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.file.Path
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

val wsServerAddress = InetSocketAddress(InetAddress.getLoopbackAddress(), 10880)

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {

    checkForAnotherInstance()

    val cmd = parseArguments(args)

    configureLogging(cmd)

    logSystemInfo()

    handleProgramArguments(cmd)

    // GUI
    // Javafx starts earlier for responsiveness. The thread will be blocked
    thread(name = "GuiLauncher") { Application.launch(GuiLauncher::class.java) }


    // Websocket server
    val wsServer: WsServer = kodein.instance()
    val config: Configuration = kodein.instance()
    wsServer.apply {
        setOnMessage("client-configuration", ClientConfigurationWsHandler(kodein.instance<ChatMessageSender>()))
        setOnMessage("viewers-count", kodein.instance<ViewersCountWsHandler>())
        setOnMessage("delete-message", DeleteWsMessageHandler(kodein.instance<ChatMessageRemover>()))
        setOnMessage("ignore-author", IgnoreWsMessageHandler(kodein.instance<IgnoreFilter>(), config))
    }
    wsServer.start()


    // Reporter
    val backgroundExecutor = kodein.instance<ScheduledExecutorService>("background")
    scheduleReportTasks(backgroundExecutor)


    // Load emoticons in background thread
    backgroundExecutor.executeWithCatch { loadEmoticons() }

    // Load global badges in background thread
    val badgeManager: BadgeManager = kodein.instance()
    launch(backgroundExecutor.asCoroutineDispatcher() + CoroutineName("GlobalBadgeLoader") + CoroutineExceptionLogger) {
        badgeManager.loadGlobalBadges()
    }

    logger.info("Application started. Version: {}. Working directory: {}", config.getString("version"),
            kodein.instance<Path>("workingDirectory").toAbsolutePath())
}

private fun checkForAnotherInstance() {
    try {
        val serverSocket = ServerSocket(wsServerAddress.port, 10, wsServerAddress.address)
        serverSocket.close()
    } catch (e: Exception) {
        System.err.println("Another instance is running at $wsServerAddress. Exception: $e")
        System.exit(0)
    }
}

private fun parseArguments(args: Array<String>): CommandLine {
    val options = Options().apply {
        addOption(Option("c", "skip-release-check", false, "Skip the check for a new release"))
        addOption(Option("r", "disable-reporter", false, "Disable reporter"))
        addOption(Option("l", "logger-root-level", true, "Logging level for root logger"))
        addOption(Option("f", "logger-failchat-level", true, "Logging level for failchat package"))
        addOption(Option("o", "enable-console-logging", false, "Enable logging into the console"))
    }

    return DefaultParser().parse(options, args)
}

private fun logSystemInfo() {
    val failchatVersion = kodein.instance<Configuration>().getString("version")
    logger.info {
        "Failchat started. Version: $failchatVersion, OS: ${sp("os.name")} ${sp("os.version")}"
    }
}

private fun handleProgramArguments(cmd: CommandLine) {
    val config: Configuration = kodein.instance()

    if (cmd.hasOption("skip-release-check")) {
        config.setProperty("release-checker.enabled", false)
    }
    if (cmd.hasOption("disable-reporter")) {
        config.setProperty("reporter.enabled", false)
    }
}

private fun loadEmoticons() {
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
            logger.warn("Exception during loading emoticons for origin {}", it.first.origin, e)
        }
    }
}

/**
 * Посылает репорт General.AppLaunch и репортит по расписанию General.Heartbeat.
 * */
private fun scheduleReportTasks(executor: ScheduledExecutorService) {
    val reporter = kodein.instance<EventReporter>()
    val dispatcher = executor.asCoroutineDispatcher()

    launch(dispatcher) {
        try {
            reporter.report(EventCategory.GENERAL, EventAction.APP_LAUNCH)
        } catch (t: Throwable) {
            logger.warn("Failed to report event {}.{}", EventCategory.GENERAL, EventAction.APP_LAUNCH, t)
        }
    }

    executor.scheduleAtFixedRate({
        launch(Unconfined) {
            try {
                reporter.report(EventCategory.GENERAL, EventAction.HEARTBEAT)
            } catch (t: Throwable) {
                logger.warn("Failed to report event {}.{}", EventCategory.GENERAL, EventAction.HEARTBEAT, t)
            }
        }
    }, 5, 5, TimeUnit.MINUTES)
}
