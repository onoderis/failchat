package failchat.youtube

data class LiveChatResponse(
        val continuationContents: ContinuationContents
) {

    data class ContinuationContents(
            val liveChatContinuation: LiveChatContinuation
    )

    data class LiveChatContinuation(
            val continuations: List<Continuation>,
            val actions: List<Action> = listOf()
    )

    data class Continuation(
            val timedContinuationData: ContinuationData?,
            val invalidationContinuationData: ContinuationData?
    )

    data class ContinuationData(
            val timeoutMs: Int,
            val continuation: String
    )

    data class Action(
            val addChatItemAction: AddChatItemAction?,
            val markChatItemsByAuthorAsDeletedAction: MarkChatItemsByAuthorAsDeletedAction?
    )

    data class AddChatItemAction(
            val item: Item
    )

    data class Item(
            val liveChatTextMessageRenderer: LiveChatTextMessageRenderer? //todo is nullable?
    )

    data class AuthorBadge(
            val liveChatAuthorBadgeRenderer: LiveChatAuthorBadgeRenderer
    )

    data class LiveChatAuthorBadgeRenderer(
            val customThumbnail: CustomThumbnail?,
            val tooltip: String
    )

    data class CustomThumbnail(
            val thumbnails: List<Thumbnail>
    )

    data class Thumbnail(
            val url: String
    )

    data class LiveChatTextMessageRenderer(
            val message: Message,
            val authorName: AuthorName,
            val authorExternalChannelId: String,
            val authorBadges: List<AuthorBadge> = listOf()
    )

    data class Message(
            val runs: List<MessageRun>
    )

    data class MessageRun(
            val text: String?,
            val emoji: Emoji?
    )

    data class AuthorName(
            val simpleText: String
    )

    data class Emoji(
            val image: EmojiImage //todo nullable?
    )

    data class EmojiImage(
            val thumbnails: List<EmojiThumbnail>, //todo nullable?
            val accessibility: EmojiAccessibility
    )

    data class EmojiThumbnail(
            val url: String
    )

    data class EmojiAccessibility(
            val accessibilityData: EmojiAccessibilityData
    )

    data class EmojiAccessibilityData(
            val label: String
    )

    data class MarkChatItemsByAuthorAsDeletedAction(
            val externalChannelId: String
    )

}

fun LiveChatResponse.Continuation.anyContinuation(): LiveChatResponse.ContinuationData {
    return invalidationContinuationData ?: timedContinuationData
    ?: error("No invalidationContinuationData and timedContinuationData")
}
