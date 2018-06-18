package failchat.gui

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object WebViewLogger {

    private val logger: Logger = LoggerFactory.getLogger(WebViewLogger::class.java)

    fun log(text: String) {
        logger.debug(text)
    }

    fun error(text: String) {
        logger.warn(text)
    }

}
