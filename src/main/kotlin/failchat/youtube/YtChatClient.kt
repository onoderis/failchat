package failchat.youtube

import com.google.api.services.youtube.model.LiveChatMessage
import com.google.common.collect.EvictingQueue
import either.Either
import either.fold
import failchat.Origin
import failchat.Origin.YOUTUBE
import failchat.chat.Author
import failchat.chat.ChatClient
import failchat.chat.ChatClientStatus
import failchat.chat.ChatClientStatus.CONNECTING
import failchat.chat.ChatClientStatus.OFFLINE
import failchat.chat.ChatClientStatus.READY
import failchat.chat.ImageFormat.VECTOR
import failchat.chat.MessageHandler
import failchat.chat.MessageIdGenerator
import failchat.chat.OriginStatus.CONNECTED
import failchat.chat.OriginStatus.DISCONNECTED
import failchat.chat.StatusMessage
import failchat.chat.badge.ImageBadge
import failchat.chat.handlers.BraceEscaper
import failchat.chat.handlers.ElementLabelEscaper
import failchat.exception.ChannelOfflineException
import failchat.util.any
import failchat.util.executeWithCatch
import failchat.util.scheduleWithCatch
import failchat.util.synchronized
import failchat.util.value
import failchat.viewers.ViewersCountLoader
import mu.KLogging
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Supplier

/**
 * Youtube chat client.
 * */
class YtChatClient(
        private val channelIdOrBroadcastId: Either<ChannelId, VideoId>,
        private val ytApiClient: YtApiClient,
        private val youtubeExecutor: ScheduledExecutorService,
        private val messageIdGenerator: MessageIdGenerator
) : ChatClient<YtMessage>,
    ViewersCountLoader {

    private companion object : KLogging() {
        val searchInterval: Duration = Duration.ofSeconds(15)
        val reconnectInterval: Duration = Duration.ofSeconds(5)
        val verifiedBadge = ImageBadge("../_shared/icons/youtube-verified.svg", VECTOR, "Verified")
        val streamerBadge = ImageBadge("../_shared/icons/youtube-streamer.svg", VECTOR, "Streamer")
        val moderatorBadge = ImageBadge("../_shared/icons/youtube-moderator.svg", VECTOR, "Moderator")
        val sponsorBadge = ImageBadge("../_shared/icons/youtube-sponsor.svg", VECTOR, "Sponsor")
    }

    override val origin = Origin.YOUTUBE
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

    private val atomicStatus = AtomicReference<ChatClientStatus>(ChatClientStatus.READY)
    private val channelId = AtomicReference<String?>()
    private val liveBroadcastId = AtomicReference<String?>()
    private val liveChatId = AtomicReference<String?>()
    private val history = EvictingQueue.create<YtMessage>(50).synchronized()

    override fun start() {
        val statusChanged = atomicStatus.compareAndSet(READY, CONNECTING)
        if (!statusChanged) return

        youtubeExecutor.executeWithCatch { getLiveChatIdRecursiveTask() }
    }

    override fun stop() {
        atomicStatus.set(OFFLINE)
    }

    /**
     * Load viewers count.
     * */
    override fun loadViewersCount(): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync<Int>(Supplier {
            //todo change exceptions
            val lbId = liveBroadcastId.value
                    ?: throw ChannelOfflineException(Origin.YOUTUBE, channelIdOrBroadcastId.any())

            return@Supplier ytApiClient.getViewersCount(lbId)
                    ?: throw ChannelOfflineException(Origin.YOUTUBE, channelIdOrBroadcastId.any())
        }, youtubeExecutor)
    }

    private fun getLiveChatIdRecursiveTask() {
        if (atomicStatus.get() == OFFLINE) {
            logger.info("Shutting down youtube chat client. task: 'getMessagesRecursiveTask'")
            return
        }

        // meh
        try {
            val channelId = channelIdOrBroadcastId.fold(
                    { it },
                    {
                        ytApiClient.getChannelId(it)
                                ?.also { logger.debug("Got channelId: '{}'", it) }
                                ?: throw BroadcastNotFoundException("videos", it)
                    }
            )

            val liveBroadcastId = channelIdOrBroadcastId.fold(
                    {
                        ytApiClient.findFirstLiveBroadcast(it)
                                ?: ytApiClient.findFirstUpcomingBroadcast(it)
                                ?.also { logger.debug("Got liveBroadcastId: '{}'", it) }
                                ?: throw BroadcastNotFoundException("search", it)
                    },
                    { it }
            )

            val liveChatId = ytApiClient.getLiveChatId(liveBroadcastId)
                    ?: channelIdOrBroadcastId.fold(
                            { throw LiveChatNotFoundException(it, liveBroadcastId) },
                            { throw LiveChatNotFoundException(it) }
                    )
            logger.debug("Got liveChatId: '{}'", liveChatId)

            val channelTitle = ytApiClient.getChannelTitle(channelId)
                    ?: throw ChannelNotFoundException(channelId)
            logger.debug("Got channelTitle: '{}'", channelTitle)

            highlightHandler.channelTitle.value = channelTitle //todo refactor
            this.liveBroadcastId.value = liveBroadcastId
            this.channelId.value = channelId
            this.liveChatId.value = liveChatId

            youtubeExecutor.executeWithCatch { getMessagesRecursiveTask(YtRequestParameters(liveChatId, null, true)) }
        } catch (e: Exception) {
            logger.warn("Failed to find stream. Retry in {} ms. channelId/broadcastId: '{}'", searchInterval.toMillis(),
                    channelIdOrBroadcastId.any(), e)
            youtubeExecutor.scheduleWithCatch(searchInterval) { getLiveChatIdRecursiveTask() }
            return
        }
    }

    private fun getMessagesRecursiveTask(params: YtRequestParameters) {
        if (atomicStatus.get() == OFFLINE) {
            logger.info("Shutting down youtube chat client. task: 'getMessagesRecursiveTask'")
            return
        }

        // Get message list
        logger.debug("Requesting liveChatMessages. channelId/broadcastId: '{}', liveChatId: '{}'", channelId, params.liveChatId)
        val response = try {
            ytApiClient.getLiveChatMessages(params.liveChatId, params.nextPageToken)
        } catch (e: Exception) {
            logger.warn("Failed to get liveChatMessages", e)

            youtubeExecutor.scheduleWithCatch(reconnectInterval) { getMessagesRecursiveTask(params) }

            // Change status to disconnected / send status message
            val statusChanged = atomicStatus.compareAndSet(ChatClientStatus.CONNECTED, CONNECTING)
            if (statusChanged) onStatusMessage?.invoke(StatusMessage(YOUTUBE, DISCONNECTED))
            return
        }

        logger.debug { "Got liveChatMessages response: $response" }
        val messageUpdateInterval = Duration.ofMillis(response.pollingIntervalMillis)

        // Schedule task
        val nextRequestParameters = params.copy(nextPageToken = response.nextPageToken, isFirstRequest = false)
        youtubeExecutor.scheduleWithCatch(messageUpdateInterval) { getMessagesRecursiveTask(nextRequestParameters) }

        // Send "connected" status message
        val statusChanged = atomicStatus.compareAndSet(CONNECTING, ChatClientStatus.CONNECTED)
        if (statusChanged) onStatusMessage?.invoke(StatusMessage(YOUTUBE, CONNECTED))

        // Skip messages from first request
        if (params.isFirstRequest) {
            logger.debug("Messages from first success request skipped. channelId: '{}', liveChatId: '{}'", channelId, params.liveChatId)
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
        // Code is not tested because youtube doesn't send delete message events
        response.items.asSequence()
                .filter { it.snippet.type == "messageDeletedEvent" }
                .map { ytMessage -> history.find { it.ytId == ytMessage.snippet.messageDeletedDetails.deletedMessageId } }
                .filterNotNull()
                .forEach { onChatMessageDeleted?.invoke(it) }
    }

    private fun LiveChatMessage.toYtChatMessage(): YtMessage {
        val message = YtMessage(
                messageIdGenerator.generate(),
                this.id,
                Author(this.authorDetails.displayName, this.authorDetails.channelId),
                this.snippet.displayMessage
        )

        if (this.authorDetails.isVerified)
            message.addBadge(verifiedBadge)
        if (this.authorDetails.isChatOwner)
            message.addBadge(streamerBadge)
        else if (this.authorDetails.isChatModerator)
            message.addBadge(moderatorBadge)
        if (this.authorDetails.isChatSponsor)
            message.addBadge(sponsorBadge)

        return message
    }

}
