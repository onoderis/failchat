package failchat

import failchat.chat.ChatMessageHistory
import failchat.chat.badge.BadgeManager
import failchat.emoticon.EmoticonStorage
import failchat.emoticon.GlobalEmoticonUpdater
import failchat.emoticon.OriginEmoticonStorageFactory
import failchat.emoticon.TwitchEmoticonFactory
import failchat.gui.ChatFrameLauncher
import failchat.gui.GuiLauncher
import failchat.gui.GuiMode
import failchat.gui.PortBindAlert
import failchat.reporter.EventAction
import failchat.reporter.EventCategory
import failchat.reporter.EventReporter
import failchat.skin.Skins
import failchat.util.CoroutineExceptionLogger
import failchat.util.bytesToMegabytes
import failchat.util.sp
import failchat.ws.server.WsFrameSender
import failchat.ws.server.WsMessageDispatcher
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.http.content.files
import io.ktor.http.content.static
import io.ktor.response.respondRedirect
import io.ktor.routing.get
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
import org.kodein.di.instance
import org.mapdb.DB
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.reflect.KClass
import io.ktor.application.Application as KtorApplication
import javafx.application.Application as JfxApplication

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

    val cmd = parseArguments(args)

    handlePortArgument(cmd)

    checkForAnotherInstance()

    configureLogging(cmd)

    handleResetConfigurationOption()

    val config: Configuration = kodein.instance()
    handleProgramArguments(cmd, config)

    val guiMode = GuiMode.valueOf(config.getString("gui-mode"))
    runGui(guiMode)

    logSystemInfo()


    // Http/websocket server
    val wsFrameSender: WsFrameSender = kodein.instance()
    wsFrameSender.start()

    val httpServer: ApplicationEngine = kodein.instance()
    httpServer.start()
    logger.info("Http/websocket server started at {}:{}", FailchatServerInfo.host.hostAddress, FailchatServerInfo.port)


    // Reporter
    val backgroundExecutor = kodein.instance<ScheduledExecutorService>("background")
    runReporterIfEnabled(backgroundExecutor, config)

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
            kodein.instance<TwitchEmoticonFactory>()
    )
    emoticonStorage.setStorages(originEmoticonStorages)
    logger.info("Emoticon storages initialized")


    // Actualize emoticons
    val emoticonUpdater = kodein.instance<GlobalEmoticonUpdater>()
    emoticonUpdater.actualizeEmoticonsAsync()

    // Load global badges in background thread
    val badgeManager: BadgeManager = kodein.instance()
    val badgeLoaderCtx = backgroundExecutor.asCoroutineDispatcher() + CoroutineName("GlobalBadgeLoader") + CoroutineExceptionLogger
    CoroutineScope(badgeLoaderCtx).launch {
        badgeManager.loadGlobalBadges()
    }

    kodein.instance<ChatMessageHistory>().start()


    // Create directory for failchat emoticons if required
    Files.createDirectories(kodein.instance<Path>("failchatEmoticonsDirectory"))

    if (guiMode == GuiMode.CHAT_ONLY || guiMode == GuiMode.NO_GUI) {
        // start app with current configuration
        val appStateManager = kodein.instance<AppStateManager>()
        appStateManager.startChat()

        if (guiMode == GuiMode.NO_GUI) {
            // add shutdown hook
            val hookThread = Thread({
                appStateManager.shutDown(false)
            }, "ShutdownHookThread")
            Runtime.getRuntime().addShutdownHook(hookThread)
        }
    }

    logger.info("Application started")
}

private fun checkForAnotherInstance() {
    try {
        val serverSocket = ServerSocket(FailchatServerInfo.port, 10, FailchatServerInfo.host)
        serverSocket.close()
        return
    } catch (e: Exception) {
        System.err.println("Another instance is running at ${FailchatServerInfo.host}:${FailchatServerInfo.port}. Exception: $e")
    }

    JfxApplication.launch(PortBindAlert::class.java)
    System.exit(0)
}

/** Delete user config file and emoticons db file if user configuration was reset during a previous launch. */
private fun handleResetConfigurationOption() {
    // don't initialize Configuration in kodein module for the case when configuration reset needed
    val configLoader = kodein.instance<ConfigLoader>()
    val config = configLoader.load()

    if (config.getBoolean(ConfigKeys.resetConfiguration)) {
        logger.info("Resetting user configuration...")
        configLoader.deleteUserConfigFile()
        configLoader.dropLoadedConfig()

        val emoticonsDbFile: Path = kodein.instance("emoticonDbFile")
        Files.deleteIfExists(emoticonsDbFile)
        logger.info("Emoticons db file was deleted: {}", emoticonsDbFile)
    } else {
        logger.debug("Configuration reset isn't needed")
    }
}

private fun runGui(guiMode: GuiMode) {
    if (guiMode == GuiMode.NO_GUI) {
        return
    }

    val guiClass: KClass<out JfxApplication> = when (guiMode) {
        GuiMode.FULL_GUI -> GuiLauncher::class
        GuiMode.CHAT_ONLY -> ChatFrameLauncher::class
        else -> error("Unexpected gui mode: $guiMode")
    }
    // Javafx starts earlier for responsiveness. Thread will be blocked
    thread(name = "GuiLauncher", priority = Thread.MAX_PRIORITY) {
        logger.info("Launching javafx application")
        JfxApplication.launch(guiClass.java)
    }
}

private fun parseArguments(args: Array<String>): CommandLine {
    val options = Options().apply {
        addOption(Option("c", "skip-release-check",     false, "Skip the check for a new release"))
        addOption(Option("r", "disable-reporter",       false, "Disable reporter"))
        addOption(Option("l", "logger-root-level",      true,  "Logging level for root logger"))
        addOption(Option("f", "logger-failchat-level",  true,  "Logging level for failchat package"))
        addOption(Option("o", "enable-console-logging", false, "Enable logging into the console"))
        addOption(Option("p", "port",                   true,  "Server port"))
        addOption(Option("g", "gui-mode",               true, "Possible modes: NO_GUI, ONLY_CHAT, FULL_GUI"))
    }

    return DefaultParser().parse(options, args)
}

private fun logSystemInfo() {
    logger.info {
        val failchatVersion = kodein.instance<Configuration>().getString("version")
        val workingDirectory = kodein.instance<Path>("workingDirectory").toAbsolutePath()
        val rt = Runtime.getRuntime()
        "Failchat started. Version: $failchatVersion." +
                "OS: ${sp("os.name")} (${sp("os.version")}). " +
                "Processors: ${rt.availableProcessors()}. " +
                "Memory: max ${rt.maxMemory().bytesToMegabytes()}mb; total ${rt.totalMemory().bytesToMegabytes()}mb, " +
                "free ${rt.freeMemory().bytesToMegabytes()}mb. " +
                "Working directory: '$workingDirectory'. " +
                "Time zone: ${ZoneOffset.systemDefault().rules.getOffset(Instant.now())}"
    }
}

private fun handlePortArgument(cmd: CommandLine) {
    if (cmd.hasOption("port")) {
        FailchatServerInfo.port = cmd.getOptionValue("port")!!.toInt()
    }
}

private fun handleProgramArguments(cmd: CommandLine, config: Configuration) {
    if (cmd.hasOption("skip-release-check")) {
        config.setProperty("release-checker.enabled", false)
    }
    if (cmd.hasOption("disable-reporter")) {
        config.setProperty("reporter.enabled", false)
    }
    if (cmd.hasOption("gui-mode")) {
        config.setProperty("gui-mode", cmd.getOptionValue("gui-mode"))
    } else {
        config.setProperty("gui-mode", GuiMode.FULL_GUI.name)
    }
}

fun createHttpServer(): ApplicationEngine {
    return embeddedServer(
            Netty,
            host = FailchatServerInfo.host.hostAddress,
            port = FailchatServerInfo.port,
            module = KtorApplication::failchat
    )
}

fun KtorApplication.failchat() {
    val wsMessageDispatcher: WsMessageDispatcher = kodein.instance()
    val wsFrameSender: WsFrameSender = kodein.instance()
    val failchatEmoticonsPath: Path = kodein.instance("failchatEmoticonsDirectory")

    install(WebSockets)

    routing {
        static("resources") {
            files("skins")
        }
        static("emoticons") {
            files(failchatEmoticonsPath.toFile())
        }

        webSocket("/ws") {
            wsFrameSender.notifyNewSession(this)
            wsMessageDispatcher.handleWebSocket(this)
        }

        get("/chat/{skin?}") {
            val skin = call.parameters["skin"] ?: Skins.default
            val params = call.parameters["port"]?.let { p -> "?port=$p" } ?: ""
            call.respondRedirect("/resources/$skin/$skin.html$params", permanent = true)
        }
    }
}

/**
 * Send General.AppLaunch event and schedule General.Heartbeat events.
 * */
private fun runReporterIfEnabled(executor: ScheduledExecutorService, config: Configuration) {
    if (!config.getBoolean("reporter.enabled")) {
        logger.info("Event reporter is disabled and won't be running")
        return
    }

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
        CoroutineScope(Dispatchers.Default).launch {
            try {
                reporter.report(EventCategory.GENERAL, EventAction.HEARTBEAT)
            } catch (t: Throwable) {
                logger.warn("Failed to report event {}.{}", EventCategory.GENERAL, EventAction.HEARTBEAT, t)
            }
        }
    }, 2, 2, TimeUnit.MINUTES)
}
