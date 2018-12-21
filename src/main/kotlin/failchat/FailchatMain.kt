package failchat

import com.github.salomonbrys.kodein.instance
import failchat.chat.ChatMessageHistory
import failchat.chat.badge.BadgeManager
import failchat.emoticon.EmoticonStorage
import failchat.emoticon.EmoticonUpdater
import failchat.emoticon.OriginEmoticonStorageFactory
import failchat.gui.GuiLauncher
import failchat.reporter.EventAction
import failchat.reporter.EventCategory
import failchat.reporter.EventReporter
import failchat.twitch.TwitchEmoticonUrlFactory
import failchat.util.CoroutineExceptionLogger
import failchat.util.bytesToMegabytes
import failchat.util.sp
import failchat.ws.server.WsFrameSender
import failchat.ws.server.WsMessageDispatcher
import io.ktor.application.install
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.Option
import org.apache.commons.cli.Options
import org.apache.commons.configuration2.Configuration
import org.mapdb.DB
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import io.ktor.application.Application as KtorApplication
import javafx.application.Application as JfxApplication

val wsServerAddress = InetSocketAddress(InetAddress.getLoopbackAddress(), 10880)
const val httpServerHost = "127.0.0.1"
const val httpServerPort = 10880

private val logger = KotlinLogging.logger {}

fun main(args: Array<String>) {
    try {
        main0(args)
    } catch (t: Throwable) {
        logger.error("Exception in main method", t)
        System.err.println("Exception in main method")
        t.printStackTrace()

        System.exit(-1)
    }
}

fun main0(args: Array<String>) {

    checkForAnotherInstance()

    val cmd = parseArguments(args)

    configureLogging(cmd)

    // GUI
    // Javafx starts earlier for responsiveness. The thread will be blocked
    thread(name = "GuiLauncher", priority = Thread.MAX_PRIORITY) {
        logger.info("Launching javafx application")
        JfxApplication.launch(GuiLauncher::class.java)
    }

    val config: Configuration = kodein.instance()
    handleProgramArguments(cmd, config)

    logSystemInfo()


    // Http/websocket server
    val wsFrameSender: WsFrameSender = kodein.instance()
    wsFrameSender.start()

    val httpServer: ApplicationEngine = kodein.instance()
    httpServer.start()
    logger.info("Http/websocket server started at {}:{}", httpServerHost, httpServerPort)


    // Reporter
    val backgroundExecutor = kodein.instance<ScheduledExecutorService>("background")
    scheduleReportTasks(backgroundExecutor)


    // If emoticon db file not exists, reset 'last-updated' config values
    val dbPath = kodein.instance<Path>("emoticonDbFile")
    val dbFileExists = Files.exists(dbPath)
    if (!dbFileExists) {
        logger.info("DB file '{}' not exists, resetting 'emoticons.last-updated' config parameters to 0", dbPath)
        config.resetEmoticonsUpdatedTime()
    }

    // Initialize emoticon storages
    val emoticonStorage = kodein.instance<EmoticonStorage>()
    val originEmoticonStorages = OriginEmoticonStorageFactory.create(
            kodein.instance<DB>("emoticons"),
            kodein.instance<TwitchEmoticonUrlFactory>()
    )
    emoticonStorage.setStorages(originEmoticonStorages)
    logger.info("Emoticon storages initialized")


    // Aztualize emoticons
    val emoticonUpdater = kodein.instance<EmoticonUpdater>()
    emoticonUpdater.actualizeEmoticonsAsync()

    // Load global badges in background thread
    val badgeManager: BadgeManager = kodein.instance()
    val badgeLoaderCtx = backgroundExecutor.asCoroutineDispatcher() + CoroutineName("GlobalBadgeLoader") + CoroutineExceptionLogger
    CoroutineScope(badgeLoaderCtx).launch {
        badgeManager.loadGlobalBadges()
    }

    kodein.instance<ChatMessageHistory>().start()


    // Create directory for custom emoticons if required
    Files.createDirectories(kodein.instance<Path>("customEmoticonsDirectory"))

    logger.info("Application started")
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
        addOption(Option("c", "skip-release-check",     false, "Skip the check for a new release"))
        addOption(Option("r", "disable-reporter",       false, "Disable reporter"))
        addOption(Option("l", "logger-root-level",      true,  "Logging level for root logger"))
        addOption(Option("f", "logger-failchat-level",  true,  "Logging level for failchat package"))
        addOption(Option("o", "enable-console-logging", false, "Enable logging into the console"))
    }

    return DefaultParser().parse(options, args)
}

private fun logSystemInfo() {
    logger.info {
        val failchatVersion = kodein.instance<Configuration>().getString("version")
        val workingDirectory = kodein.instance<Path>("workingDirectory").toAbsolutePath()
        val rt = Runtime.getRuntime()
        "Failchat started. Version: $failchatVersion, OS: ${sp("os.name")} (${sp("os.version")}). " +
                "Processors: ${rt.availableProcessors()}. " +
                "Memory: max ${rt.maxMemory().bytesToMegabytes()}mb; total ${rt.totalMemory().bytesToMegabytes()}mb, " +
                "free ${rt.freeMemory().bytesToMegabytes()}mb; " +
                "Working directory: '$workingDirectory'"
    }
}

private fun handleProgramArguments(cmd: CommandLine, config: Configuration) {
    if (cmd.hasOption("skip-release-check")) {
        config.setProperty("release-checker.enabled", false)
    }
    if (cmd.hasOption("disable-reporter")) {
        config.setProperty("reporter.enabled", false)
    }
}

fun createHttpServer(): ApplicationEngine {
    return embeddedServer(
            Netty,
            host = httpServerHost,
            port = httpServerPort,
            module = KtorApplication::failchat
    )
}

fun KtorApplication.failchat() {
    val wsMessageDispatcher: WsMessageDispatcher = kodein.instance()
    val wsFrameSender: WsFrameSender = kodein.instance()
    val customEmoticonsPath: Path = kodein.instance("customEmoticonsDirectory")

    install(WebSockets)

    routing {
        static("resources") {
            files("skins")
        }
        static("emoticons") {
            files(customEmoticonsPath.toFile())
        }

        webSocket("/chat") {
            wsFrameSender.notifyNewSession(this)
            wsMessageDispatcher.handleWebSocket(this)
        }

//        get("websocket") {
//            call.respondRedirect("http://${wsServerAddress.hostString}:${wsServerAddress.port}/", permanent = true)
//        }
    }
}

/**
 * Send General.AppLaunch event and schedule General.Heartbeat events.
 * */
private fun scheduleReportTasks(executor: ScheduledExecutorService) {
    val reporter = kodein.instance<EventReporter>()
    val dispatcher = executor.asCoroutineDispatcher()

    CoroutineScope(dispatcher).launch {
        try {
            reporter.report(EventCategory.GENERAL, EventAction.APP_LAUNCH)
        } catch (t: Throwable) {
            logger.warn("Failed to report event {}.{}", EventCategory.GENERAL, EventAction.APP_LAUNCH, t)
        }
    }

    executor.scheduleAtFixedRate({
        CoroutineScope(Dispatchers.Unconfined).launch {
            try {
                reporter.report(EventCategory.GENERAL, EventAction.HEARTBEAT)
            } catch (t: Throwable) {
                logger.warn("Failed to report event {}.{}", EventCategory.GENERAL, EventAction.HEARTBEAT, t)
            }
        }
    }, 5, 5, TimeUnit.MINUTES)
}
