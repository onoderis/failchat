package failchat.util

import kotlinx.coroutines.experimental.CoroutineExceptionHandler
import kotlinx.coroutines.experimental.CoroutineName
import kotlinx.coroutines.experimental.channels.SendChannel
import mu.KotlinLogging
import kotlin.coroutines.experimental.CoroutineContext

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
