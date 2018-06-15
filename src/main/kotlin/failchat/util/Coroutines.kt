package failchat.util

import kotlinx.coroutines.experimental.CoroutineExceptionHandler
import kotlinx.coroutines.experimental.CoroutineName
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.coroutines.experimental.CoroutineContext

object CoroutineExceptionLogger : CoroutineExceptionHandler {

    private val log: Logger = LoggerFactory.getLogger(CoroutineExceptionLogger::class.java)

    override val key = CoroutineExceptionHandler.Key

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        log.warn("Uncaught exception in coroutine '{}'", context[CoroutineName.Key]?.name, exception)
    }
}
