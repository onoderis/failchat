package failchat

import com.github.salomonbrys.kodein.instance
import failchat.chat.ChatMessageHistory
import failchat.chat.badge.BadgeManager
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
import failchat.ws.server.WsFrameSender
import failchat.ws.server.WsMessageDispatcher
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.pipeline.PipelineContext
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.CoroutineScope
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

    checkForAnotherInstance()

    val cmd = parseArguments(args)

    configureLogging(cmd)

    logSystemInfo()

    handleProgramArguments(cmd)

    // GUI
    // Javafx starts earlier for responsiveness. The thread will be blocked
    thread(name = "GuiLauncher") {
        logger.info("Launching javafx application")
        JfxApplication.launch(GuiLauncher::class.java)
    }

    // Http/websocket server
    val wsFrameSender: WsFrameSender = kodein.instance()
    wsFrameSender.start()

    val httpServer: ApplicationEngine = kodein.instance()
    httpServer.start()
    logger.info("Http/websocket server started at {}:{}", httpServerHost, httpServerPort)


    // Reporter
    val backgroundExecutor = kodein.instance<ScheduledExecutorService>("background")
    scheduleReportTasks(backgroundExecutor)


    // Load emoticons in background thread
    backgroundExecutor.executeWithCatch { loadEmoticons() }

    // Load global badges in background thread
    val badgeManager: BadgeManager = kodein.instance()
    val badgeLoaderCtx = backgroundExecutor.asCoroutineDispatcher() + CoroutineName("GlobalBadgeLoader") + CoroutineExceptionLogger
    CoroutineScope(badgeLoaderCtx).launch {
        badgeManager.loadGlobalBadges()
    }

    kodein.instance<ChatMessageHistory>().start()

    val config: Configuration = kodein.instance()

    // Create directory for custom emoticons if required
    Files.createDirectories(kodein.instance<Path>("customEmoticonsDirectory"))

    val workingDirectory = kodein.instance<Path>("workingDirectory").toAbsolutePath()
    logger.info("Application started. Version: {}. Working directory: {}", config.getString("version"), workingDirectory)
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

class MyHandler {
    suspend fun PipelineContext<Unit, ApplicationCall>.handle(unit: Unit) {
        call.respond(HttpStatusCode.OK, "ok")
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
