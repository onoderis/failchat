package failchat.ws.server

import failchat.ws.server.WsFrameSender.ChannelMessage.Broadcast
import failchat.ws.server.WsFrameSender.ChannelMessage.SessionClosed
import failchat.ws.server.WsFrameSender.ChannelMessage.SessionOpened
import io.ktor.http.cio.websocket.Frame
import io.ktor.websocket.DefaultWebSocketServerSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.launch
import mu.KLogging

class WsFrameSender {

    private companion object : KLogging()

    private val job = Job()
    private val channel: Channel<ChannelMessage> = Channel(Channel.UNLIMITED)
    private val activeSessions: MutableList<DefaultWebSocketServerSession> = ArrayList()

    fun start() {
        CoroutineScope(job).launch {
            for (message in channel) {
                when (message) {
                    is SessionOpened -> {
                        val session = message.session
                        activeSessions.add(session)
                        watchForSessionToClose(session)
                    }

                    is SessionClosed -> activeSessions.remove(message.session)

                    is Broadcast -> {
                        logger.debug("Sending message to all websocket clients: {}", message.payload)
                        activeSessions.forEach { session ->
                            try {
                                session.send(Frame.Text(message.payload))
                            } catch (e: Exception) {
                                logger.warn("Failed to send ws message to a client", e)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun CoroutineScope.watchForSessionToClose(session: DefaultWebSocketServerSession) {
        launch {
            try {
                session.closeReason.await()
            } finally {
                channel.send(SessionClosed(session))
            }
        }
    }

    fun stop() {
        job.cancel()
    }

    fun sendToAll(message: String) {
        channel.sendBlocking(Broadcast(message))
    }

    suspend fun notifyNewSession(session: DefaultWebSocketServerSession) {
        channel.send(SessionOpened(session))
    }

    private sealed class ChannelMessage {
        class Broadcast(val payload: String) : ChannelMessage()
        class SessionOpened(val session: DefaultWebSocketServerSession) : ChannelMessage()
        class SessionClosed(val session: DefaultWebSocketServerSession) : ChannelMessage()
    }

}
