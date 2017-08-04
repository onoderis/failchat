package failchat.youtube

import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.LiveChatMessage
import failchat.core.Origin
import failchat.core.chat.ChatClient
import failchat.core.chat.ChatClientStatus
import failchat.core.chat.ChatClientStatus.connected
import failchat.core.chat.ChatClientStatus.connecting
import failchat.core.chat.ChatClientStatus.offline
import failchat.core.chat.ChatClientStatus.ready
import failchat.core.chat.MessageIdGenerator
import failchat.core.chat.OriginStatus
import failchat.core.chat.StatusMessage
import failchat.core.viewers.ViewersCountLoader
import failchat.exception.ChannelOfflineException
import failchat.util.schedule
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
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


    private val atomicStatus: AtomicReference<ChatClientStatus> = AtomicReference(ChatClientStatus.ready)
    private val searchInterval: Duration = Duration.ofSeconds(5)
    private val reconnectInterval: Duration = Duration.ofSeconds(5)
    private val liveBroadcastId: AtomicReference<String?> = AtomicReference(null)


    override fun start() {
        val statusChanged = atomicStatus.compareAndSet(ready, connecting)
        if (!statusChanged) return //todo throw IllegalStateException?

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
            val lbId = liveBroadcastId.get()
                    ?: throw ChannelOfflineException(Origin.youtube, channelId)

            return@Supplier ytApiClient.getViewersCount(lbId)
                    ?: throw ChannelOfflineException(Origin.youtube, channelId)
        }, youtubeExecutor)
    }

    private fun getLiveChatIdRecursiveTask() {
        if (atomicStatus.get() == offline) {
            log.debug("Shutting down youtube chat client. task: 'getMessagesRecursiveTask'")
            return
        }

        val liveChatId = try {
            val lbId = ytApiClient.getLiveBroadcastId(channelId)
                    ?: ytApiClient.getUpcomingBroadcastId(channelId)
                    ?: throw BroadcastNotFoundException("Youtube broadcast not ")
            log.debug("Got liveBroadcastId: '{}'", lbId)

            val lcId = ytApiClient.getLiveChatId(lbId)
                    ?: throw LiveChatNotFoundException(channelId, lbId)
            log.debug("Got liveChatId: '{}'", lcId)

            liveBroadcastId.set(lbId)
            lcId
        } catch (e: Exception) {
            log.warn("Failed to get liveChatId. Retry in {} ms. channelId: '{}'", searchInterval.toMillis(), channelId, e)
            youtubeExecutor.schedule(searchInterval) { getLiveChatIdRecursiveTask() }
            return
        }

        youtubeExecutor.execute { getMessagesRecursiveTask(YtRequestParameters(liveChatId, null, true)) }
    }

    private fun getMessagesRecursiveTask(params: YtRequestParameters) {
        if (atomicStatus.get() == offline) {
            log.debug("Shutting down youtube chat client. task: 'getMessagesRecursiveTask'")
            return
        }

        // Get message list
        log.debug("Requesting liveChatMessages. channelId: '{}', liveChatId: '{}'", channelId, params.liveChatId)
        val response = try {
            youTube.LiveChatMessages()
                    .list(params.liveChatId, "snippet, authorDetails") //не кидает исключение если невалидный id
                    .setPageToken(params.nextPageToken)
                    .execute()
        } catch (e: Exception) {
            log.warn("Failed to get liveChatMessages", e)

            //todo сколько сообщений подгрузится после реконнекта - все или какие нужны. т.е. когда у гугла протухает nextPageToken
            youtubeExecutor.schedule(reconnectInterval) { getMessagesRecursiveTask(params) }

            // Change status to disconnected / send status message
            val statusChanged = atomicStatus.compareAndSet(connected, connecting)
            if (statusChanged) onStatusMessage?.invoke(StatusMessage(Origin.youtube, OriginStatus.DISCONNECTED))
            return
        }

        val messageUpdateInterval = Duration.ofMillis(response.pollingIntervalMillis)
        log.debug("messageUpdateInterval: {} ms", messageUpdateInterval.toMillis())


        // Send "connected" status message
        val statusChanged = atomicStatus.compareAndSet(connecting, connected)
        if (statusChanged) onStatusMessage?.invoke(StatusMessage(Origin.youtube, OriginStatus.CONNECTED))

        // Skip messages from first request
        if (params.isFirstRequest) {
            val nextRequestParameters = params.copy(
                    nextPageToken = response.nextPageToken,
                    isFirstRequest = false
            )
            youtubeExecutor.schedule(messageUpdateInterval) { getMessagesRecursiveTask(nextRequestParameters) }
            log.debug("Messages from first success request skipped. channelId: '{}', liveChatId: '{}'", channelId, params.liveChatId)
            return
        }

        // Handle messages
        response.items
                .map { it.toYsChatMessage() }
                .forEach { onChatMessage?.invoke(it) }

        // Schedule task
        val nextRequestParameters = params.copy(nextPageToken = response.nextPageToken)
        youtubeExecutor.schedule(messageUpdateInterval) { getMessagesRecursiveTask(nextRequestParameters) }
    }

    private fun LiveChatMessage.toYsChatMessage(): YtMessage {
        return YtMessage(
                messageIdGenerator.generate(),
                this.id,
                this.authorDetails.displayName,
                this.snippet.displayMessage
        )
    }

}
