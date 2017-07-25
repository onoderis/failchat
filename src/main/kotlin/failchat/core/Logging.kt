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

fun configureLogging() {
    SLF4JBridgeHandler.removeHandlersForRootLogger()
    SLF4JBridgeHandler.install()


    val rootLogger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as LogbackLogger
    rootLogger.detachAndStopAllAppenders()
    rootLogger.level = Level.WARN
//    rootLogger.level = Level.DEBUG

    val logback = LoggerFactory.getILoggerFactory() as LoggerContext

    // jul propagator
    val julPropagator = LevelChangePropagator().apply {
        setResetJUL(true)
        context = logback
        start()
    }
    logback.addListener(julPropagator)

    // Console appender
    val consoleEncoder = PatternLayoutEncoder().apply {
        context = logback
        pattern = """%date %level [%thread] %logger \(%file:%line\) %msg%n"""
//        pattern = """%date{HH:mm:ss.SSS} %level %msg%n"""
//        pattern = """%date{HH:mm:ss.SSS} %.-1level %msg%n"""
        start()
    }
    val consoleAppender = ConsoleAppender<ILoggingEvent>().apply {
        context = logback
        encoder = consoleEncoder
        target = "System.err"
        isWithJansi = true
        start()
    }

    // File Appender
    val fileEncoder = PatternLayoutEncoder().apply {
        context = logback
        pattern = """%date %level [%thread] %logger \(%file:%line\) %msg%n"""
        start()
    }
    val fileAppender = FileAppender<ILoggingEvent>().apply {
        context = logback
        encoder = fileEncoder
        file = "failchat.log"
        isAppend = false
        start()
    }

    val failchatLogger = LoggerFactory.getLogger("failchat") as LogbackLogger
    failchatLogger.level = Level.DEBUG
    failchatLogger.isAdditive = false
    failchatLogger.addAppender(fileAppender)
    failchatLogger.addAppender(consoleAppender)

    rootLogger.addAppender(fileAppender)
    rootLogger.addAppender(consoleAppender)


    Thread.setDefaultUncaughtExceptionHandler { _, e ->
        rootLogger.error("Uncaught exception", e)
    }


    failchatLogger.info("Logging configured")
}
