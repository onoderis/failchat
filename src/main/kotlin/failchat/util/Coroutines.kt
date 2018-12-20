package failchat.util

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.channels.SendChannel
import mu.KotlinLogging
import kotlin.coroutines.CoroutineContext

object CoroutineExceptionLogger : CoroutineExceptionHandler {

    private val logger = KotlinLogging.logger {}

    override val key = CoroutineExceptionHandler.Key

    override fun handleException(context: CoroutineContext, exception: Throwable) {
        logger.warn("Uncaught exception in coroutine '{}'", context[CoroutineName.Key]?.name, exception)
    }
}

fun <T> SendChannel<T>.offerOrThrow(element: T) {
    val offered = offer(element)
    if (!offered) throw RuntimeException("SendChannel.offer operation failed")
}
