package failchat.peka2tv

import com.google.common.collect.EvictingQueue
import failchat.Origin
import failchat.Origin.PEKA2TV
import failchat.chat.ChatClient
import failchat.chat.ChatClientStatus
import failchat.chat.ChatClientStatus.CONNECTING
import failchat.chat.ChatClientStatus.OFFLINE
import failchat.chat.ChatClientStatus.READY
import failchat.chat.MessageFilter
import failchat.chat.MessageHandler
import failchat.chat.MessageIdGenerator
import failchat.chat.OriginStatus.CONNECTED
import failchat.chat.OriginStatus.DISCONNECTED
import failchat.chat.StatusMessage
import failchat.chat.handlers.BraceEscaper
import failchat.chat.handlers.ElementLabelEscaper
import failchat.emoticon.EmoticonFinder
import failchat.exception.UnexpectedResponseException
import failchat.util.synchronized
import failchat.util.warn
import failchat.viewers.ViewersCountLoader
import io.socket.client.IO
import io.socket.client.Socket
import okhttp3.OkHttpClient
import org.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicReference

class Peka2tvChatClient(
        private val channelName: String,
        private val channelId: Long,
        private val socketIoUrl: String,
        private val okHttpClient: OkHttpClient,
        private val messageIdGenerator: MessageIdGenerator,
        private val emoticonFinder: EmoticonFinder
) : ChatClient<Peka2tvMessage>,
        ViewersCountLoader {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(Peka2tvChatClient::class.java)
    }

    override val status: ChatClientStatus get() = atomicStatus.get()
    override val origin = PEKA2TV

    override var onChatMessage: ((Peka2tvMessage) -> Unit)? = null
    override var onStatusMessage: ((StatusMessage) -> Unit)? = null
    override var onChatMessageDeleted: ((Peka2tvMessage) -> Unit)? = null


    private val socket = buildSocket()
    private val atomicStatus: AtomicReference<ChatClientStatus> = AtomicReference(READY)
    private val history = EvictingQueue.create<Peka2tvMessage>(50).synchronized()

    private val messageHandlers: List<MessageHandler<Peka2tvMessage>> = listOf(
            ElementLabelEscaper(),
            BraceEscaper(),
            Peka2tvHighlightHandler(channelName),
            Peka2tvEmoticonHandler(emoticonFinder)
    )
    private val messageFilters: List<MessageFilter<Peka2tvMessage>> = listOf(
            Peka2tvOriginFilter(),
            AnnounceMessageFilter()
    )


    override fun start() {
        if (!atomicStatus.compareAndSet(READY, CONNECTING)) {
            return
        }

        socket.connect()
    }

    override fun stop() {
        atomicStatus.set(OFFLINE)
        socket.disconnect()
    }

    override fun loadViewersCount(): CompletableFuture<Int> {
        val viewersCountFuture = CompletableFuture<Int>()

        val obj = JSONObject()
        obj.put("channel", "stream/$channelId")

        socket.emit("/chat/channel/list", arrayOf(obj)) { responseObjects ->
            try {
                val response = responseObjects[0] as JSONObject
                val status = response.getString("status")
                if (status != "ok") {
                    viewersCountFuture.completeExceptionally(UnexpectedResponseException("Unexpected response status $status"))
                }

                viewersCountFuture.complete(response.getJSONObject("result").getInt("amount"))
            } catch (e: Exception) {
                log.warn(e) {
                    "Unexpected exception during updating peka2tv viewers count. response message: ${responseObjects.contentToString()}"
                }
                viewersCountFuture.completeExceptionally(e)
            }
        }

        return viewersCountFuture
    }

    private fun buildSocket(): Socket {
        IO.setDefaultOkHttpCallFactory(okHttpClient)
        IO.setDefaultOkHttpWebSocketFactory(okHttpClient)

        val options = IO.Options().apply {
            transports = arrayOf("websocket")
            forceNew = true
        }

        val socket = IO.socket(socketIoUrl, options)
        socket
                // Connect
                .on(Socket.EVENT_CONNECT) {
                    if (atomicStatus.get() == ChatClientStatus.OFFLINE) { //for quick close case
                        socket.disconnect()
                        return@on
                    }

                    val message = JSONObject().apply {
                        put("channel", "stream/$channelId")
                    }
                    socket.emit("/chat/join", arrayOf(message)) {
                        log.info("Connected to ${Origin.PEKA2TV}")
                        atomicStatus.set(ChatClientStatus.CONNECTED)
                        onStatusMessage?.invoke(StatusMessage(PEKA2TV, CONNECTED))
                    }
                }

                // Disconnect
                .on(Socket.EVENT_DISCONNECT) {
                    atomicStatus.set(CONNECTING)
                    log.info("Received disconnected event from peka2tv ")
                    onStatusMessage?.invoke(StatusMessage(PEKA2TV, DISCONNECTED))
                }

                // Message
                .on("/chat/message") { objects ->
                    // https://github.com/peka2tv/api/blob/master/chat.md#Новое-сообщение
                    val messageNode = objects[0] as JSONObject
                    val toNode: JSONObject? = if (!messageNode.isNull("to")) {
                        messageNode.getJSONObject("to")
                    } else {
                        null
                    }

                    val message = Peka2tvMessage(
                            id = messageIdGenerator.generate(),
                            peka2tvId = messageNode.getLong("id"),
                            fromUser = messageNode.getJSONObject("from").toUser(),
                            text = messageNode.getString("text"),
                            type = messageNode.getString("type"),
                            toUser = toNode?.toUser()
                    )

                    //filter message
                    messageFilters.forEach {
                        if (it.filterMessage(message)) return@on
                    }
                    //handle message
                    messageHandlers.forEach { it.handleMessage(message) }

                    history.add(message)
                    onChatMessage?.invoke(message)
                }

                // Message removal
                .on("/chat/message/remove") { objects ->
                    val removeMessage = objects[0] as JSONObject
                    val idToRemove = removeMessage.getLong("id")

                    val foundMessage = history.find { it.peka2tvId == idToRemove }
                    foundMessage?.let { onChatMessageDeleted?.invoke(it) }
                }

        return socket
    }

    private fun JSONObject.toUser(): Peka2tvUser {
        return Peka2tvUser(this.getString("name"), this.getLong("id"))
    }

}
