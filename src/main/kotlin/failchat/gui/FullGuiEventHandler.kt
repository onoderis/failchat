package failchat.gui

import failchat.AppStateManager
import failchat.ConfigKeys
import failchat.chat.ChatMessageSender
import failchat.util.LateinitVal
import failchat.util.completedFuture
import failchat.util.executeWithCatch
import javafx.application.Platform
import org.apache.commons.configuration2.Configuration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.Executors
import java.util.function.BiConsumer

class FullGuiEventHandler(
        private val appStateManager: AppStateManager,
        private val messageSender: ChatMessageSender,
        private val config: Configuration
) : GuiEventHandler {

    private val executor = Executors.newSingleThreadExecutor()

    val guiFrames = LateinitVal<GuiFrames>()

    override fun handleStartChat() {
        val guiTransitionFuture = makeGuiTransition {
            it.settingsFrame.hide()
            it.chatFrame.show()
        }

        guiTransitionFuture.whenCompleteAsync(BiConsumer { _, _ ->
            appStateManager.startChat()
        }, executor)
    }

    override fun handleStopChat() {
        val guiTransitionFuture = makeGuiTransition {
            it.chatFrame.hide()
            it.settingsFrame.show()
        }

        guiTransitionFuture.whenCompleteAsync(BiConsumer { _, _ ->
            appStateManager.stopChat()
        }, executor)
    }

    override fun handleShutDown() {
        executor.executeWithCatch {
            appStateManager.shutDown(true)
        }
        executor.shutdown()
    }

    override fun handleResetUserConfiguration() {
        val settingsFrame = guiFrames.get()?.settingsFrame ?: return

        Platform.runLater {
            val resetConfirmed = settingsFrame.confirmConfigReset()
            if (resetConfirmed) {
                settingsFrame.disableResetConfigurationButton()
                config.setProperty(ConfigKeys.resetConfiguration, true)
            }
        }
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
        val settingsFrame = guiFrames.get()?.settingsFrame ?: return

        Platform.runLater {
            settingsFrame.disableRefreshEmoticonsButton()
        }
    }

    override fun notifyEmoticonsLoaded() {
        val settingsFrame = guiFrames.get()?.settingsFrame ?: return

        Platform.runLater {
            settingsFrame.enableRefreshEmoticonsButton()
        }
    }

    private fun makeGuiTransition(framesOperation: (GuiFrames) -> Unit): CompletionStage<Unit> {
        val frames = guiFrames.get()

        return if (frames != null) {
            val guiTransitionFuture = CompletableFuture<Unit>()
            Platform.runLater {
                try {
                    framesOperation(frames)
                } finally {
                    guiTransitionFuture.complete(Unit)
                }
            }
            guiTransitionFuture
        } else {
            completedFuture()
        }
    }

}
