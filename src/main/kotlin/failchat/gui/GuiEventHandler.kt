package failchat.gui

import failchat.AppStateManager
import failchat.ConfigKeys
import failchat.chat.ChatMessageSender
import failchat.util.LateinitVal
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

    val gui = LateinitVal<GuiFrames>()

    fun handleStartChat() {
        gui.get()?.let {
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
        gui.get()?.let {
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
        val settingsFrame = gui.get()?.settingsFrame ?: return

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
        val settingsFrame = gui.get()?.settingsFrame ?: return

        Platform.runLater {
            settingsFrame.disableRefreshEmoticonsButton()
        }
    }

    fun notifyEmoticonsLoaded() {
        val settingsFrame = gui.get()?.settingsFrame ?: return

        Platform.runLater {
            settingsFrame.enableRefreshEmoticonsButton()
        }
    }

}
