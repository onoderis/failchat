package failchat.core.viewers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import failchat.core.Origin
import failchat.core.Origin.cybergame
import failchat.core.Origin.goodgame
import failchat.core.Origin.peka2tv
import failchat.core.Origin.twitch
import failchat.core.viewers.ViewersCounter.State.ready
import failchat.core.viewers.ViewersCounter.State.shutdown
import failchat.core.viewers.ViewersCounter.State.working
import failchat.core.ws.server.WsServer
import failchat.exceptions.ChannelOfflineException
import failchat.utils.await
import org.apache.commons.configuration.CompositeConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.EnumSet
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

class ViewersCounter(
        private val wsServer: WsServer,
        private val config: CompositeConfiguration,
        private val objectMapper: ObjectMapper = ObjectMapper()
) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ViewersCounter::class.java)
        val updateInterval: Duration = Duration.ofSeconds(15)
    }

    private val lock: Lock = ReentrantLock()
    private val shutdownCondition: Condition = lock.newCondition()
    private val viewersCount: MutableMap<Origin, Int> = ConcurrentHashMap()
    private val viewersCountLoaders: MutableList<ViewersCountLoader> = CopyOnWriteArrayList()
    private val countableOrigins: Set<Origin> = EnumSet.of(peka2tv, twitch, goodgame, cybergame)

    private var state: AtomicReference<State> = AtomicReference(State.ready)


    fun start(viewersCountLoaders: List<ViewersCountLoader>) = lock.withLock {
        if (state.get() != State.ready) throw IllegalStateException("Expected state: ${State.ready}, actual: $state")
        state.set(working)

        this.viewersCountLoaders.addAll(viewersCountLoaders)
        thread(start = true, name = "ViewersCounterThread") {
            updateAndSendLoop()
        }
        log.info("ViewersCounter started")
    }

    fun stop() = lock.withLock {
        if (state.get() != working) return

        viewersCountLoaders.clear()
        viewersCount.clear()
        state.set(shutdown)
        shutdownCondition.signal()
    }

    fun sendViewersCountWsMessage() {
        val enabledOrigins = countableOrigins.filter {
            config.getBoolean("${it.name}.enabled")
        }

        val message = formViewersWsMessage(enabledOrigins)
        wsServer.sendToAll(message.toString())
    }

    private fun updateAndSendLoop() {
        while (state.get() != shutdown) {
            updateViewersCount()
            sendViewersCountWsMessage()
            lock.withLock { shutdownCondition.await(updateInterval) }
        }
        viewersCount.clear()
        state.set(ready)
        log.info("ViewersManager stopped")
    }

    private fun updateViewersCount() {
        viewersCountLoaders.forEach { accessor ->
            val count: Int? = try {
                accessor.loadViewersCount().join()
            } catch (e: CompletionException) {
                val cause = e.cause
                if (cause is ChannelOfflineException) {
                    log.info("Couldn't update viewers count, channel {}#{} is offline", cause.channel, cause.origin.name)
                } else {
                    log.warn("Failed to get viewers count for origin {}", accessor.origin, e)
                }
                null
            }

            count?.let { viewersCount.put(accessor.origin, it) }
        }
    }


    private fun formViewersWsMessage(originsToInclude: List<Origin>): JsonNode {
        val messageNode = objectMapper.createObjectNode()
                .put("type", "viewers")

        val contentNode = messageNode.putObject("content")
                .put("show", config.getBoolean("show-viewers"))

        val countersNode = contentNode.putObject("counters")

        originsToInclude.forEach { origin ->
            viewersCount.get(origin)
                    ?.let { countersNode.put(origin.name, it) }
                    ?: countersNode.putNull(origin.name)
        }

        return messageNode
    }

    private enum class State {
        ready,
        working,
        shutdown
    }

}
