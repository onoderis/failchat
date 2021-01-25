package failchat.youtube2

import failchat.Origin
import failchat.chat.Author
import failchat.chat.ChatClient
import failchat.chat.ChatClientCallbacks
import failchat.chat.ChatClientStatus
import failchat.chat.ChatMessageHistory
import failchat.chat.Elements
import failchat.chat.ImageFormat
import failchat.chat.MessageHandler
import failchat.chat.MessageIdGenerator
import failchat.chat.OriginStatus
import failchat.chat.StatusUpdate
import failchat.chat.badge.ImageBadge
import failchat.chat.findTyped
import failchat.chat.handlers.BraceEscaper
import failchat.util.LateinitVal
import failchat.util.value
import failchat.youtube.YtColors
import failchat.youtube.YtEmoticon
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KLogging
import java.util.concurrent.atomic.AtomicReference

class Youtube2ChatClient(
        override val callbacks: ChatClientCallbacks,
        private val youtubeClient: YoutubeClient2,
        private val messageIdGenerator: MessageIdGenerator,
        private val history: ChatMessageHistory,
        private val videoId: String
) : ChatClient {

    private companion object : KLogging() {
        val roleToBadgeMap = mapOf(
                RoleBadges.verified.description to RoleBadges.verified,
                "Owner" to RoleBadges.streamer,
                RoleBadges.moderator.description to RoleBadges.moderator

        )
        val roleBadgeToColorMap = mapOf(
                RoleBadges.streamer to YtColors.streamer,
                RoleBadges.moderator to YtColors.moderator
        )
    }

    override val origin = Origin.YOUTUBE
    override val status: ChatClientStatus
        get() = atomicStatus.get()

    private val atomicStatus: AtomicReference<ChatClientStatus> = AtomicReference(ChatClientStatus.READY)
    private val pollJob = LateinitVal<Job>()

    private val highlightHandler = YoutubeHighlightHandler()

    private val messageHandlers: List<MessageHandler<YoutubeMessage>> = listOf(
            BraceEscaper(),
            highlightHandler
    )

    override fun start() {
        val statusChanged = atomicStatus.compareAndSet(ChatClientStatus.READY, ChatClientStatus.CONNECTING)
        if (!statusChanged) {
            error("Chat client status: ${atomicStatus.value}")
        }

        //todo coroutine dispatcher
        val job = GlobalScope.launch {
            val initialParameters = youtubeClient.getNewLiveChatSessionData(videoId)
            logger.info { "Initial youtube parameters: $initialParameters" }

            val statusUpdated = atomicStatus.compareAndSet(ChatClientStatus.CONNECTING, ChatClientStatus.CONNECTED)
            if (statusUpdated) {
                callbacks.onStatusUpdate(StatusUpdate(Origin.YOUTUBE, OriginStatus.CONNECTED))
            }

            highlightHandler.setChannelTitle(initialParameters.channelName)

            val actionChannel = with(youtubeClient) {
                pollLiveChatActions(initialParameters)
            }
            for (action in actionChannel) {
                if (action.isModerationAction()) {
                    val channelIdToDeleteMessages = action.markChatItemsByAuthorAsDeletedAction!!.externalChannelId

                    val messagesToDelete = history.findTyped<YoutubeMessage> {
                        channelIdToDeleteMessages == it.author.id
                    }
                    messagesToDelete.forEach {
                        callbacks.onChatMessageDeleted(it)
                    }

                } else {
                    val message = action.toChatMessage() ?: continue
                    messageHandlers.forEach {
                        it.handleMessage(message)
                    }
                    callbacks.onChatMessage(message)
                }
            }
        }

        job.invokeOnCompletion { e ->
            if (e != null) {
                atomicStatus.set(ChatClientStatus.ERROR)
            } else {
                atomicStatus.set(ChatClientStatus.OFFLINE)
            }
            callbacks.onStatusUpdate(StatusUpdate(Origin.YOUTUBE, OriginStatus.DISCONNECTED))
        }

        pollJob.set(job)
    }

    override fun stop() {
        pollJob.get()?.cancel() ?: error("Chat client is not started")
        atomicStatus.value = ChatClientStatus.OFFLINE
    }

    private fun LiveChatResponse.Action.toChatMessage(): YoutubeMessage? {
        val textMessageDto = addChatItemAction?.item?.liveChatTextMessageRenderer ?: run {
            logger.info { "No new messages since the last request" }
            return null
        }

        val youtubeMessage = YoutubeMessage(
                failchatId = messageIdGenerator.generate(),
                author = Author(
                        name = textMessageDto.authorName.simpleText,
                        origin = Origin.YOUTUBE,
                        id = textMessageDto.authorExternalChannelId
                ),
                text = ""
        )

        youtubeMessage.text = textMessageDto.message.runs.fold(StringBuilder()) { acc, run ->
            when {
                run.text != null -> acc.append(Elements.escapeLabelCharacters(run.text))
                run.emoji != null -> {
                    val imageUrl = run.emoji.image.thumbnails.firstOrNull() ?: run {
                        logger.warn { "Null image url for emoji: ${run.emoji} " }
                        return@fold acc
                    }
                    val label = youtubeMessage.addElement(YtEmoticon(
                            code = run.emoji.image.accessibility.accessibilityData.label,
                            url = imageUrl.url,
                            format = ImageFormat.RASTER
                    ))
                    acc.append(label)
                }
                else -> logger.warn { "Unknown MessageRun" }
            }
            acc
        }.toString()

        addChatItemAction.item.liveChatTextMessageRenderer.authorBadges
                .map { it.toBadge() }
                .forEach { youtubeMessage.addBadge(it) }

        // determine role by badge and set author color
        loop@ for (badge in youtubeMessage.badges) {
            val prioritizedRoleColor = roleBadgeToColorMap[badge]
            when {
                prioritizedRoleColor != null -> {
                    youtubeMessage.author.color = prioritizedRoleColor
                    break@loop
                }
                youtubeMessage.badges.first() == RoleBadges.verified -> {
                    // verified author has no special color, do nothing
                }
                else -> {
                    youtubeMessage.author.color = YtColors.member
                    break@loop
                }
            }
        }

        return youtubeMessage
    }

    private fun LiveChatResponse.Action.isModerationAction(): Boolean {
        return markChatItemsByAuthorAsDeletedAction != null
    }

    private fun LiveChatResponse.AuthorBadge.toBadge(): ImageBadge {
        roleToBadgeMap[liveChatAuthorBadgeRenderer.tooltip]?.let {
            return it
        }

        // last thumbnail has bigger resolution
        val thumbnailsUrl = liveChatAuthorBadgeRenderer.customThumbnail?.thumbnails?.lastOrNull()?.url ?: run {
            error("Unexpected badge object")
        }

        return ImageBadge(
                url = thumbnailsUrl,
                format = ImageFormat.RASTER,
                description = liveChatAuthorBadgeRenderer.tooltip
        )
    }

}
