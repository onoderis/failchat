package failchat.core

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.jul.LevelChangePropagator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import ch.qos.logback.classic.Logger as LogbackLogger

fun configureLogging(args: Array<String>) {
    val rootLevel = if (args.contains("--root-debug")) Level.DEBUG else Level.WARN
    val failchatLevel = if (args.contains("--failchat-debug")) Level.DEBUG else Level.WARN
    val consoleEnabled = args.contains("--console")


    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()


    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as LogbackLogger
    rootLogger.detachAndStopAllAppenders()
    rootLogger.level = rootLevel

    val logbackContext = LoggerFactory.getILoggerFactory() as LoggerContext

    // jul propagator
    val julPropagator = LevelChangePropagator().apply {
        setResetJUL(true)
        context = logbackContext
        start()
    }
    logbackContext.addListener(julPropagator)


    val consoleAppender = if (consoleEnabled) configureConsoleAppender(logbackContext) else null
    val fileAppender = configureFileAppender(logbackContext)

    val failchatLogger = LoggerFactory.getLogger("failchat") as LogbackLogger
    failchatLogger.level = failchatLevel
    failchatLogger.isAdditive = false
    failchatLogger.addAppender(fileAppender)
    consoleAppender?.let { failchatLogger.addAppender(it) }

    rootLogger.addAppender(fileAppender)
    consoleAppender?.let { rootLogger.addAppender(it) }


    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        rootLogger.error("Uncaught exception", e)
    }


    failchatLogger.info("Logging configured")
}

private fun configureConsoleAppender(logbackContext: LoggerContext): ConsoleAppender<ILoggingEvent> {
    val consoleEncoder = PatternLayoutEncoder().apply {
        context = logbackContext
//        pattern = """%date %level [%thread] %logger \(%file:%line\) %msg%n"""
//        pattern = """%date{HH:mm:ss.SSS} %level %msg%n"""
        pattern = """%date{HH:mm:ss.SSS} %.-1level \(%file:%line\) %msg%n"""
        start()
    }
    return ConsoleAppender<ILoggingEvent>().apply {
        context = logbackContext
        encoder = consoleEncoder
        target = "System.err"
        isWithJansi = true
        start()
    }
}

private fun configureFileAppender(logbackContext: LoggerContext): FileAppender<ILoggingEvent> {
    val fileEncoder = PatternLayoutEncoder().apply {
        context = logbackContext
        pattern = """%date %level [%thread] %logger \(%file:%line\) %msg%n"""
        start()
    }
    return FileAppender<ILoggingEvent>().apply {
        context = logbackContext
        encoder = fileEncoder
        file = "log/failchat.log"
//        isAppend = false
        start()
    }
}
