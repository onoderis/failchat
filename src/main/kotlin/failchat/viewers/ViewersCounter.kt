package failchat.viewers

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import failchat.Origin
import failchat.chat.ChatMessageSender
import failchat.exception.ChannelOfflineException
import failchat.util.await
import failchat.util.doUnwrappingExecutionException
import failchat.util.get
import failchat.util.value
import failchat.viewers.ViewersCounter.State.READY
import failchat.viewers.ViewersCounter.State.SHUTDOWN
import failchat.viewers.ViewersCounter.State.WORKING
import mu.KotlinLogging
import java.time.Duration
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
        private val chatMessageSender: ChatMessageSender
) {
    private companion object {
        val logger = KotlinLogging.logger {}
        val updateInterval: Duration = Duration.ofSeconds(15)
        val countAwaitDuration: Duration = Duration.ofSeconds(3)
    }

    private val nodeFactory: JsonNodeFactory = JsonNodeFactory.instance
    private val lock: Lock = ReentrantLock()
    private val shutdownCondition: Condition = lock.newCondition()
    private val enabledOrigins: List<Origin> = viewersCountLoaders.map { it.origin }
    private val viewersCount: MutableMap<Origin, Int> = ConcurrentHashMap()

    private val state: AtomicReference<State> = AtomicReference(State.READY)


    fun start() {
        val changed = state.compareAndSet(READY, WORKING)
        if (!changed) throw IllegalStateException("Expected state: $READY, actual: ${state.get()}." +
                "(Actual state could change after unsuccessful CAS operation)")

        thread(name = "ViewersCounterThread") {
            updateAndSendLoop()
        }
        logger.info {
            "ViewersCounter started. Enabled origins: " +
                    viewersCountLoaders.map { it.origin }.joinToString(separator = ", ")
        }
    }

    fun stop() {
        state.set(SHUTDOWN)
        lock.withLock { shutdownCondition.signal() }
        viewersCount.clear()
    }

    fun sendViewersCountWsMessage() {
        val message = formViewersWsMessage(enabledOrigins)
        chatMessageSender.send(message)
    }

    private fun updateAndSendLoop() {
        while (state.get() != SHUTDOWN) {
            updateViewersCount()
            sendViewersCountWsMessage()
            lock.withLock {
                if (state.value == WORKING) {
                    shutdownCondition.await(updateInterval)
                }
            }
        }
        viewersCount.clear()
        logger.info("ViewersManager stopped")
    }

    private fun updateViewersCount() {
        viewersCountLoaders
                .map { it.origin to it.loadViewersCount() }
                .map { (origin, countFuture) ->
                    try {
                        doUnwrappingExecutionException {
                            origin to countFuture.get(countAwaitDuration)
                        }
                    } catch (e: ChannelOfflineException) {
                        logger.info("Couldn't update viewers count, channel {}#{} is offline", e.channel, e.origin)
                        origin to null
                    } catch (e: Exception) {
                        logger.warn("Failed to get viewers count for origin {}", origin, e)
                        origin to null
                    }
                }
                .forEach { (origin, count) ->
                    if (count != null) viewersCount.put(origin, count)
                    else viewersCount.remove(origin)
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
