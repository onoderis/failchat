package failchat.youtube

import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.LiveChatMessage
import failchat.core.Origin
import failchat.core.chat.Author
import failchat.core.chat.ChatClient
import failchat.core.chat.ChatClientStatus
import failchat.core.chat.ChatClientStatus.connected
import failchat.core.chat.ChatClientStatus.connecting
import failchat.core.chat.ChatClientStatus.offline
import failchat.core.chat.ChatClientStatus.ready
import failchat.core.chat.MessageHandler
import failchat.core.chat.MessageIdGenerator
import failchat.core.chat.OriginStatus
import failchat.core.chat.StatusMessage
import failchat.core.chat.handlers.BraceEscaper
import failchat.core.chat.handlers.ElementLabelEscaper
import failchat.core.viewers.ViewersCountLoader
import failchat.exception.ChannelOfflineException
import failchat.util.ConcurrentEvictingQueue
import failchat.util.debug
import failchat.util.scheduleWithCatch
import failchat.util.value
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.Queue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

/**
 * Youtube chat client.
 * */
class YtChatClient(
        private val channelId: String,
        private val youTube: YouTube,
        private val ytApiClient: YtApiClient,
        private val youtubeExecutor: ScheduledExecutorService,
        private val messageIdGenerator: MessageIdGenerator
) : ChatClient<YtMessage>,
        ViewersCountLoader {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(YtChatClient::class.java)
    }

    override val origin = Origin.youtube
    override val status: ChatClientStatus get() = atomicStatus.get()

    override var onChatMessage: ((YtMessage) -> Unit)? = null
    override var onStatusMessage: ((StatusMessage) -> Unit)? = null
    override var onChatMessageDeleted: ((YtMessage) -> Unit)? = null

    private val highlightHandler = YtHighlightHandler()
    private val messageHandlers: List<MessageHandler<YtMessage>> = listOf(
            ElementLabelEscaper(),
            BraceEscaper(), // символы < и > приходят неэкранированными
            YtEmojiHandler(),
            highlightHandler
    )
    private val atomicStatus: AtomicReference<ChatClientStatus> = AtomicReference(ChatClientStatus.ready)
    private val searchInterval: Duration = Duration.ofSeconds(15)
    private val reconnectInterval: Duration = Duration.ofSeconds(5)
    private val liveBroadcastId: AtomicReference<String?> = AtomicReference(null)
    private val history: Queue<YtMessage> = ConcurrentEvictingQueue(50)

    override fun start() {
        val statusChanged = atomicStatus.compareAndSet(ready, connecting)
        if (!statusChanged) return

        youtubeExecutor.execute { getLiveChatIdRecursiveTask() }
    }

    override fun stop() {
        atomicStatus.set(offline)
    }

    /**
     * Load viewers count in asynchronous blocking way on dedicated [java.util.concurrent.ExecutorService].
     * */
    override fun loadViewersCount(): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync<Int>(Supplier {
            //todo change exceptions
            val lbId = liveBroadcastId.value
                    ?: throw ChannelOfflineException(Origin.youtube, channelId)

            return@Supplier ytApiClient.getViewersCount(lbId)
                    ?: throw ChannelOfflineException(Origin.youtube, channelId)
        }, youtubeExecutor)
    }

    private fun getLiveChatIdRecursiveTask() {
        if (atomicStatus.get() == offline) {
            log.info("Shutting down youtube chat client. task: 'getMessagesRecursiveTask'")
            return
        }

        val liveChatId = try {
            val lbId = ytApiClient.findLiveBroadcast(channelId)
                    ?: ytApiClient.findUpcomingBroadcast(channelId)
                    ?: throw BroadcastNotFoundException(channelId)
            log.debug("Got liveBroadcastId: '{}'", lbId)

            val lcId = ytApiClient.getLiveChatId(lbId)
                    ?: throw LiveChatNotFoundException(channelId, lbId)
            log.debug("Got liveChatId: '{}'", lcId)

            val channelTitle = ytApiClient.getChannelTitle(channelId)
                    ?: throw ChannelNotFoundException(channelId)

            highlightHandler.channelTitle.value = channelTitle
            liveBroadcastId.value = lbId
            lcId
        } catch (e: Exception) {
            log.warn("Failed to find stream. Retry in {} ms. channelId: '{}'", searchInterval.toMillis(), channelId, e)
            youtubeExecutor.scheduleWithCatch(searchInterval) { getLiveChatIdRecursiveTask() }
            return
        }

        youtubeExecutor.execute { getMessagesRecursiveTask(YtRequestParameters(liveChatId, null, true)) }
    }

    private fun getMessagesRecursiveTask(params: YtRequestParameters) {
        if (atomicStatus.get() == offline) {
            log.info("Shutting down youtube chat client. task: 'getMessagesRecursiveTask'")
            return
        }

        // Get message list
        log.debug("Requesting liveChatMessages. channelId: '{}', liveChatId: '{}'", channelId, params.liveChatId)
        val response = try {
            youTube.LiveChatMessages()
                    .list(params.liveChatId, "id, snippet, authorDetails") //не кидает исключение если невалидный id
                    .setPageToken(params.nextPageToken)
                    .execute()
        } catch (e: Exception) {
            log.warn("Failed to get liveChatMessages", e)

            youtubeExecutor.scheduleWithCatch(reconnectInterval) { getMessagesRecursiveTask(params) }

            // Change status to disconnected / send status message
            val statusChanged = atomicStatus.compareAndSet(connected, connecting)
            if (statusChanged) onStatusMessage?.invoke(StatusMessage(Origin.youtube, OriginStatus.DISCONNECTED))
            return
        }

        log.debug { "Got liveChatMessages response: $response" }
        val messageUpdateInterval = Duration.ofMillis(response.pollingIntervalMillis)

        // Schedule task
        val nextRequestParameters = params.copy(nextPageToken = response.nextPageToken, isFirstRequest = false)
        youtubeExecutor.scheduleWithCatch(messageUpdateInterval) { getMessagesRecursiveTask(nextRequestParameters) }

        // Send "connected" status message
        val statusChanged = atomicStatus.compareAndSet(connecting, connected)
        if (statusChanged) onStatusMessage?.invoke(StatusMessage(Origin.youtube, OriginStatus.CONNECTED))

        // Skip messages from first request
        if (params.isFirstRequest) {
            log.debug("Messages from first success request skipped. channelId: '{}', liveChatId: '{}'", channelId, params.liveChatId)
            return
        }

        // Handle chat messages
        response.items.asSequence()
                .filter { it.snippet.type == "textMessageEvent" || it.snippet.type == "superChatEvent" }
                .map {
                    val ytMessage = it.toYtChatMessage()
                    if (it.snippet.type == "superChatEvent") ytMessage.highlighted = true
                    ytMessage
                }
                .forEach { message ->
                    messageHandlers.forEach { it.handleMessage(message) }
                    history.add(message)
                    onChatMessage?.invoke(message)
                }

        // Handle message deletions
        response.items.asSequence()
                .filter { it.snippet.type == "messageDeletedEvent" }
                .map { ytMessage -> history.find { it.ytId == ytMessage.snippet.messageDeletedDetails.deletedMessageId } }
                .filterNotNull()
                .forEach { onChatMessageDeleted?.invoke(it) }
    }

    private fun LiveChatMessage.toYtChatMessage(): YtMessage {
        return YtMessage(
                messageIdGenerator.generate(),
                this.id,
                Author(this.authorDetails.displayName, this.authorDetails.channelId),
                this.snippet.displayMessage
        )
    }

}
