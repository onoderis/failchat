package failchat.gui

import failchat.AppStateManager
import failchat.ConfigLoader
import failchat.chat.ChatMessageSender
import failchat.util.executeWithCatch
import javafx.application.Platform
import java.util.concurrent.Executors

class GuiEventHandler(
        private val appStateManager: AppStateManager,
        private val messageSender: ChatMessageSender,
        private val configLoader: ConfigLoader
) {

    lateinit var settingsFrame: SettingsFrame
    lateinit var chatFrame: ChatFrame

    private val executor = Executors.newSingleThreadExecutor()

    fun handleStartChat() {
        Platform.runLater {
            settingsFrame.hide()
            chatFrame.show()
        }
        executor.executeWithCatch {
            appStateManager.startChat()
        }
    }

    fun handleStopChat() {
        Platform.runLater {
            chatFrame.hide()
            settingsFrame.show()
        }
        executor.executeWithCatch {
            appStateManager.stopChat()
        }
    }

    fun handleShutDown() {
        executor.executeWithCatch {
            appStateManager.shutDown()
        }
        executor.shutdown()
    }

    fun handleResetUserConfiguration() {
        configLoader.resetConfigurableByUserProperties()
        Platform.runLater {
            settingsFrame.updateSettingsValues()
        }
    }

    fun notifyConfigurationChanged() {
        executor.executeWithCatch {
            messageSender.sendClientConfiguration()
        }
    }

    fun notifyEmoticonsAreLoading() {
        if (!::settingsFrame.isInitialized) return
        Platform.runLater {
            settingsFrame.disableRefreshEmoticonsButton()
        }
    }

    fun notifyEmoticonsLoaded() {
        if (!::settingsFrame.isInitialized) return
        Platform.runLater {
            settingsFrame.enableRefreshEmoticonsButton()
        }
    }

}
