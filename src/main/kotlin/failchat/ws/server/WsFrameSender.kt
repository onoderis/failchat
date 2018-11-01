package failchat.ws.server

import failchat.util.offerOrThrow
import failchat.ws.server.WsFrameSender.ChannelMessage.Broadcast
import failchat.ws.server.WsFrameSender.ChannelMessage.SessionClosed
import failchat.ws.server.WsFrameSender.ChannelMessage.SessionOpened
import io.ktor.http.cio.websocket.Frame
import io.ktor.websocket.DefaultWebSocketServerSession
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.launch
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
        channel.offerOrThrow(Broadcast(message))
    }

    fun notifyNewSession(session: DefaultWebSocketServerSession) {
        channel.offerOrThrow(SessionOpened(session))
    }

    private sealed class ChannelMessage {
        class Broadcast(val payload: String) : ChannelMessage()
        class SessionOpened(val session: DefaultWebSocketServerSession) : ChannelMessage()
        class SessionClosed(val session: DefaultWebSocketServerSession) : ChannelMessage()
    }

}
