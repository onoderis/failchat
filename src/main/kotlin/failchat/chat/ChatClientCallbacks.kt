package failchat.chat

class ChatClientCallbacks(
        val onChatMessage: (ChatMessage) -> Unit,
        val onStatusUpdate: (StatusUpdate) -> Unit,
        val onChatMessageDeleted: (ChatMessage) -> Unit
)
