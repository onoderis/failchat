package failchat.ws.client

import failchat.util.sleep
import failchat.util.value
import failchat.ws.client.WsClient.Status.CONNECTING
import failchat.ws.client.WsClient.Status.ERROR
import failchat.ws.client.WsClient.Status.READY
import failchat.ws.client.WsClient.Status.SHUTDOWN
import failchat.ws.client.WsClient.Status.WORKING
import mu.KLogging
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
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

    private companion object : KLogging() {
        val requestHeaders = mapOf("Connection" to "Upgrade")
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

        logger.info("WsClient started. uri: {}", serverUri)
    }

    fun stop() {
        status.set(SHUTDOWN)
        lock.withLock { reconnectCondition.signal() }
        wsClient.close()

        logger.info("WsClient stopped. uri: {}", serverUri)
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
                logger.warn("Failed to reconnect to {}, next try in {} ms", serverUri, reconnectInterval.toMillis())
                sleep(reconnectInterval)
                wsClient = Wsc()
                continue
            }

            //connected
            logger.info("Reconnected to {}", serverUri)
            lock.withLock {
                reconnectCondition.await()
            }

            //prepare to reconnect
            wsClient = Wsc()
        }
    }


    private inner class Wsc : WebSocketClient(
            this@WsClient.serverUri,
            Draft_6455(),
            requestHeaders,
            0
    ) {

        private val status: AtomicReference<Status> = this@WsClient.status

        override fun onOpen(serverHandshake: ServerHandshake) {
            status.set(WORKING)
            logger.debug("Connection open. uri: '{}'", serverUri)
            this@WsClient.onOpen(serverHandshake)
        }

        override fun onMessage(message: String) {
            logger.debug("Message received. uri: '{}', message: {}", serverUri, message)
            this@WsClient.onMessage(message)
        }

        override fun onClose(code: Int, reason: String, remote: Boolean) {
            logger.debug("Connection closed. uri: '{}', code: '{}', reason: '{}'", serverUri, code, reason)

            when (status.value) {
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
            logger.debug("Error occurred. uri: {},", serverUri)

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
