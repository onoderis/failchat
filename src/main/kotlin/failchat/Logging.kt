package failchat

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.jul.LevelChangePropagator
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy
import ch.qos.logback.core.util.FileSize
import org.apache.commons.cli.CommandLine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.bridge.SLF4JBridgeHandler
import ch.qos.logback.classic.Logger as LogbackLogger

fun configureLogging(cmd: CommandLine) {
    val rootLevel = Level.toLevel(cmd.getOptionValue("logger-root-level"), Level.WARN)
    val failchatLevel = Level.toLevel(cmd.getOptionValue("logger-failchat-level"), Level.INFO)

    val consoleEnabled = cmd.hasOption("enable-console-logging")


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
        if (e is OutOfMemoryError) {
            val r = Runtime.getRuntime()
            rootLogger.error("Memory info. max: {}, total: {}, free: {}", r.maxMemory(), r.totalMemory(), r.freeMemory())
        }
    }


    failchatLogger.info("Logging configured")
}

private fun configureConsoleAppender(logbackContext: LoggerContext): ConsoleAppender<ILoggingEvent> {
    val consoleEncoder = PatternLayoutEncoder().apply {
        context = logbackContext
        pattern = """%date{HH:mm:ss.SSSXXX} %.-1level \(%file:%line\) %msg%n"""
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
        pattern = """%date{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} %level [%thread] %logger \(%file:%line\) %msg%n"""
    }

    val fileAppender = RollingFileAppender<ILoggingEvent>().apply {
        context = logbackContext
        encoder = fileEncoder
        file = "log/failchat.log"
        isAppend = true
    }

    val triggeringPolicy = SizeBasedTriggeringPolicy<ILoggingEvent>().apply {
        context = logbackContext
        setMaxFileSize(FileSize(5 * FileSize.MB_COEFFICIENT))
    }

    val rollingPolicy = FixedWindowRollingPolicy().apply {
        context = logbackContext
        setParent(fileAppender)
        fileNamePattern = "log/failchat-%i.log"
        minIndex = 1
        maxIndex = 1
    }

    fileAppender.rollingPolicy = rollingPolicy
    fileAppender.triggeringPolicy = triggeringPolicy

    rollingPolicy.start()
    triggeringPolicy.start()
    fileEncoder.start()
    fileAppender.start()

    return fileAppender
}
