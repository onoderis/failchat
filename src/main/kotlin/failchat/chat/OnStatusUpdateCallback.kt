package failchat.chat

class OnStatusUpdateCallback(
        private val originStatusManager: OriginStatusManager
) : (StatusUpdate) -> Unit {

    override fun invoke(message: StatusUpdate) {
        originStatusManager.setStatus(message.origin, message.status)
    }
}
