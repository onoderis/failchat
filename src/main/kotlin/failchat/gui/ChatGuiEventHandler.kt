package failchat.gui

import failchat.AppStateManager
import failchat.chat.ChatMessageSender
import failchat.util.executeWithCatch
import java.util.concurrent.Executors

class ChatGuiEventHandler(
        private val appStateManager: AppStateManager,
        private val messageSender: ChatMessageSender
) : GuiEventHandler {

    private val executor = Executors.newSingleThreadExecutor()

    override fun handleStartChat() {
    }

    override fun handleStopChat() {
        executor.executeWithCatch {
            appStateManager.shutDown(true)
        }
    }

    override fun handleShutDown() {
        executor.executeWithCatch {
            appStateManager.shutDown(true)
        }
    }

    override fun handleResetUserConfiguration() {
    }

    override fun handleConfigurationChange() {
        executor.executeWithCatch {
            messageSender.sendClientConfiguration()
        }
    }

    override fun handleClearChat() {
        executor.executeWithCatch {
            messageSender.sendClearChat()
        }
    }

    override fun notifyEmoticonsAreLoading() {
    }

    override fun notifyEmoticonsLoaded() {
    }

}
