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

    /** Should be invoked in javafx thread. */
    fun handleStartChat() {
        settingsFrame.hide()
        chatFrame.show()

        executor.executeWithCatch {
            appStateManager.startChat()
        }
    }

    /** Should be invoked in javafx thread. */
    fun handleStopChat() {
        chatFrame.hide()
        settingsFrame.show()

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

    /** Should be invoked in javafx thread. */
    fun handleResetUserConfiguration() {
        configLoader.resetConfigurableByUserProperties()
        Platform.runLater {
            settingsFrame.updateSettingsValues()
        }
    }

    /** Should be invoked in javafx thread. */
    fun handleConfigurationChange() {
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
