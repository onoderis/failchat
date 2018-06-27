package failchat.viewers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import failchat.Origin
import failchat.exception.ChannelOfflineException
import failchat.util.await
import failchat.viewers.ViewersCounter.State.READY
import failchat.viewers.ViewersCounter.State.SHUTDOWN
import failchat.viewers.ViewersCounter.State.WORKING
import failchat.ws.server.WsServer
import mu.KLogging
import java.time.Duration
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

/**
 * Class instance is not reusable.
 * */
class ViewersCounter(
        private val viewersCountLoaders: List<ViewersCountLoader>,
        private val wsServer: WsServer
) {
    private companion object : KLogging() {
        val updateInterval: Duration = Duration.ofSeconds(15)
    }

    private val nodeFactory: JsonNodeFactory = JsonNodeFactory.instance
    private val lock: Lock = ReentrantLock()
    private val shutdownCondition: Condition = lock.newCondition()
    private val enabledOrigins: List<Origin> = viewersCountLoaders.map { it.origin }
    private val viewersCount: MutableMap<Origin, Int> = ConcurrentHashMap()

    private var state: AtomicReference<State> = AtomicReference(State.READY)


    fun start() {
        val changed = state.compareAndSet(READY, WORKING)
        if (!changed) throw IllegalStateException("Expected state: $READY, actual: ${state.get()}." +
                "(Actual state could change after unsuccessful CAS operation)")

        thread(start = true, name = "ViewersCounterThread") {
            updateAndSendLoop()
        }
        logger.info { "ViewersCounter started. Enabled origins: " + viewersCountLoaders.map { it.origin }.joinToString(separator = ", ") }
    }

    fun stop() {
        val changed = state.compareAndSet(WORKING, SHUTDOWN)
        if (!changed) return

        lock.withLock { shutdownCondition.signal() }
        viewersCount.clear()
    }

    fun sendViewersCountWsMessage() {
        val message = formViewersWsMessage(enabledOrigins)
        wsServer.send(message.toString())
    }

    private fun updateAndSendLoop() {
        while (state.get() != SHUTDOWN) {
            updateViewersCount()
            sendViewersCountWsMessage()
            lock.withLock { shutdownCondition.await(updateInterval) }
        }
        viewersCount.clear()
        logger.info("ViewersManager stopped")
    }

    private fun updateViewersCount() {
        viewersCountLoaders.forEach { accessor ->
            val count: Int? = try {
                accessor.loadViewersCount().join() //todo get(timeout)
            } catch (e: CompletionException) {
                val cause = e.cause
                if (cause is ChannelOfflineException) {
                    logger.info("Couldn't update viewers count, channel {}#{} is offline", cause.channel, cause.origin)
                } else {
                    logger.warn("Failed to get viewers count for origin {}", accessor.origin, e)
                }
                null
            } catch (e: Exception) {
                logger.warn("Unexpected exception during loading viewers count. origin: '{}'", accessor.origin, e)
                null
            }

            if (count != null) viewersCount.put(accessor.origin, count)
            else viewersCount.remove(accessor.origin)
        }
    }


    private fun formViewersWsMessage(originsToInclude: List<Origin>): JsonNode {
        val messageNode = nodeFactory.objectNode()
                .put("type", "viewers-count")

        val contentNode = messageNode.putObject("content")

        originsToInclude.forEach { origin ->
            viewersCount.get(origin)
                    ?.let { contentNode.put(origin.commonName, it) }
                    ?: contentNode.putNull(origin.commonName)
        }

        return messageNode
    }

    private enum class State {
        READY,
        WORKING,
        SHUTDOWN
    }

}
