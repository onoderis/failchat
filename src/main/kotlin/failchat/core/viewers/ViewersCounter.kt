package failchat.core.viewers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import failchat.core.Origin
import failchat.core.viewers.ViewersCounter.State.ready
import failchat.core.viewers.ViewersCounter.State.shutdown
import failchat.core.viewers.ViewersCounter.State.working
import failchat.core.ws.server.WsServer
import failchat.exception.ChannelOfflineException
import failchat.util.await
import failchat.util.info
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
 * Not reusable.
 * */
class ViewersCounter(
        private val viewersCountLoaders: List<ViewersCountLoader>,
        private val wsServer: WsServer,
        private val objectMapper: ObjectMapper = ObjectMapper()
) {
    private companion object {
        val log: Logger = LoggerFactory.getLogger(ViewersCounter::class.java)
        val updateInterval: Duration = Duration.ofSeconds(15)
    }

    private val lock: Lock = ReentrantLock()
    private val shutdownCondition: Condition = lock.newCondition()
    private val enabledOrigins: List<Origin> = viewersCountLoaders.map { it.origin }
    private val viewersCount: MutableMap<Origin, Int> = ConcurrentHashMap()

    private var state: AtomicReference<State> = AtomicReference(State.ready)


    fun start() {
        val changed = state.compareAndSet(ready, working)
        if (!changed) throw IllegalStateException("Expected state: ${ready.name}, actual: ${state.get().name}." +
                "(Actual state could change after unsuccessful CAS operation)")

        thread(start = true, name = "ViewersCounterThread") {
            updateAndSendLoop()
        }
        log.info { "ViewersCounter started. Enabled origins: " + viewersCountLoaders.map { it.origin }.joinToString(separator = ", ") }
    }

    fun stop() {
        val changed = state.compareAndSet(working, shutdown)
        if (!changed) return

        lock.withLock { shutdownCondition.signal() }
        viewersCount.clear()
    }

    fun sendViewersCountWsMessage() {
        val message = formViewersWsMessage(enabledOrigins)
        wsServer.send(message.toString())
    }

    private fun updateAndSendLoop() {
        while (state.get() != shutdown) {
            updateViewersCount()
            sendViewersCountWsMessage()
            lock.withLock { shutdownCondition.await(updateInterval) }
        }
        viewersCount.clear()
        log.info("ViewersManager stopped")
    }

    private fun updateViewersCount() {
        viewersCountLoaders.forEach { accessor ->
            val count: Int? = try {
                accessor.loadViewersCount().join() //todo get(timeout)
            } catch (e: CompletionException) {
                val cause = e.cause
                if (cause is ChannelOfflineException) {
                    log.info("Couldn't update viewers count, channel {}#{} is offline", cause.channel, cause.origin.name)
                } else {
                    log.warn("Failed to get viewers count for origin {}", accessor.origin, e)
                }
                null
            } catch (e: Exception) {
                log.warn("Unexpected exception during loading viewers count. origin: '{}'", accessor.origin.name, e)
                null
            }

            if (count != null) viewersCount.put(accessor.origin, count)
            else viewersCount.remove(accessor.origin)
        }
    }


    private fun formViewersWsMessage(originsToInclude: List<Origin>): JsonNode {
        val messageNode = objectMapper.createObjectNode()
                .put("type", "viewers-count")

        val contentNode = messageNode.putObject("content")

        originsToInclude.forEach { origin ->
            viewersCount.get(origin)
                    ?.let { contentNode.put(origin.name, it) }
                    ?: contentNode.putNull(origin.name)
        }

        return messageNode
    }

    private enum class State {
        ready,
        working,
        shutdown
    }

}
