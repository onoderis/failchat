package failchat.core.ws.client

import failchat.core.ws.client.WsClient.Status.CONNECTING
import failchat.core.ws.client.WsClient.Status.ERROR
import failchat.core.ws.client.WsClient.Status.READY
import failchat.core.ws.client.WsClient.Status.SHUTDOWN
import failchat.core.ws.client.WsClient.Status.WORKING
import failchat.utils.sleep
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

/**
 * Web socket client with auto-reconnect feature.
 * */
open class WsClient(private val serverUri: URI) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(WsClient::class.java)
    }

    private val reconnectInterval = Duration.ofSeconds(5)
    private val lock: Lock = ReentrantLock()
    private val reconnectCondition: Condition = lock.newCondition()
    private val status: AtomicReference<Status> = AtomicReference(READY)

    private var wsClient: WebSocketClient = Wsc()

    fun start() {
        thread(start = true, name = "WsClientReconnectThread") {
            tryReconnectLoop()
        }

        log.info("WsClient starter")
    }

    fun stop() {
        status.set(SHUTDOWN)
        lock.withLock { reconnectCondition.signal() }
        wsClient.close()
    }

    fun send(message: String) = wsClient.send(message)

    open fun onOpen(serverHandshake: ServerHandshake) {}

    open fun onMessage(message: String) {}

    open fun onClose(code: Int, reason: String, remote: Boolean) {}

    open fun onReconnect() {}

    open fun onError(e: Exception) {}

    private fun tryReconnectLoop() {
        status.compareAndSet(READY, CONNECTING)

        while (status.get() != SHUTDOWN && status.get() != ERROR) {
            val connected = wsClient.connectBlocking()
            if (!connected) {
                log.warn("Failed to reconnect to {}, next try in {} ms", serverUri, reconnectInterval.toMillis())
                sleep(reconnectInterval)
                wsClient = Wsc()
                continue
            }

            //connected
            log.info("Reconnected to {}", serverUri)
            lock.withLock {
                reconnectCondition.await()
            }

            //prepare to reconnect
            wsClient = Wsc()
        }
        log.info("WsClient stopped")
    }


    private inner class Wsc : WebSocketClient(this@WsClient.serverUri) {

        private val status = this@WsClient.status

        override fun onOpen(serverHandshake: ServerHandshake) {
            status.set(WORKING)
            this@WsClient.onOpen(serverHandshake)
        }

        override fun onMessage(message: String) {
            this@WsClient.onMessage(message)
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            when (status.get()!!) {
                CONNECTING,
                SHUTDOWN,
                ERROR -> return

                WORKING -> {
                    status.set(CONNECTING)
                    this@WsClient.onReconnect()
                }

                READY -> {
                    throw IllegalStateException("$READY")
                }
            }

            this@WsClient.onClose(code, reason, remote)
            lock.withLock { reconnectCondition.signal() }
        }

        override fun onError(e: Exception) {
            val statusChanged = status.compareAndSet(WORKING, CONNECTING)
            if (statusChanged) this@WsClient.onReconnect()

            this@WsClient.onError(e)
        }
    }

    enum class Status {
        READY,
        WORKING,
        CONNECTING,
        ERROR,
        SHUTDOWN
    }

}
