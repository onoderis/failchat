package failchat.gui

import failchat.AppStateManager
import failchat.ConfigKeys
import failchat.chat.ChatMessageSender
import failchat.util.executeWithCatch
import javafx.application.Platform
import org.apache.commons.configuration2.Configuration
import java.util.concurrent.Executors

class GuiEventHandler(
        private val appStateManager: AppStateManager,
        private val messageSender: ChatMessageSender,
        private val config: Configuration
) {

    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var gui: Gui? = null

    fun setGui(settingsFrame: SettingsFrame, chatFrame: ChatFrame) {
        gui = Gui(settingsFrame, chatFrame)
    }

    fun handleStartChat() {
        gui?.let {
            Platform.runLater {
                it.settingsFrame.hide()
                it.chatFrame.show()
            }
        }

        executor.executeWithCatch {
            appStateManager.startChat()
        }
    }

    fun handleStopChat() {
        gui?.let {
            Platform.runLater {
                it.chatFrame.hide()
                it.settingsFrame.show()
            }
        }

        executor.executeWithCatch {
            appStateManager.stopChat()
        }
    }

    fun handleShutDown() {
        executor.executeWithCatch {
            appStateManager.shutDown(true)
        }
        executor.shutdown()
    }

    fun handleResetUserConfiguration() {
        val settingsFrame = gui?.settingsFrame ?: return

        Platform.runLater {
            val resetConfirmed = settingsFrame.confirmConfigReset()
            if (resetConfirmed) {
                settingsFrame.disableResetConfigurationButton()
                config.setProperty(ConfigKeys.resetConfiguration, true)
            }
        }
    }

    fun handleConfigurationChange() {
        executor.executeWithCatch {
            messageSender.sendClientConfiguration()
        }
    }

    fun handleClearChat() {
        executor.executeWithCatch {
            messageSender.sendClearChat()
        }
    }

    fun notifyEmoticonsAreLoading() {
        val settingsFrame = gui?.settingsFrame ?: return

        Platform.runLater {
            settingsFrame.disableRefreshEmoticonsButton()
        }
    }

    fun notifyEmoticonsLoaded() {
        val settingsFrame = gui?.settingsFrame ?: return

        Platform.runLater {
            settingsFrame.enableRefreshEmoticonsButton()
        }
    }

    private class Gui(
            val settingsFrame: SettingsFrame,
            val chatFrame: ChatFrame
    )

}
