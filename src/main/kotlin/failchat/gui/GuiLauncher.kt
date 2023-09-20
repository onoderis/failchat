package failchat.gui

import failchat.Dependencies
import failchat.failchatEmoticonsDirectory
import failchat.platform.windows.WindowsCtConfigurator
import failchat.util.LateinitVal
import failchat.util.executeWithCatch
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control.ButtonBar.ButtonData.OK_DONE
import javafx.scene.control.ButtonType
import javafx.stage.Stage
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant

class GuiLauncher : Application() {

    companion object {
        val deps = LateinitVal<Dependencies>()
        private val logger = KotlinLogging.logger {}
    }

    override fun start(primaryStage: Stage) {
        val startTime = Instant.now()

        val config = deps.get()!!.configuration
        val isWindows = com.sun.jna.Platform.isWindows()

        val settings = SettingsFrame(
                this,
                primaryStage,
                config,
                deps.get()!!.skinList,
                failchatEmoticonsDirectory,
                isWindows,
                lazy { deps.get()!!.guiEventHandler },
                lazy { deps.get()!!.globalEmoticonUpdater }
        )

        settings.show()

        val showTime = Instant.now()
        logger.debug { "Settings frame showed in ${Duration.between(startTime, showTime).toMillis()} ms" }


        val ctConfigurator: ClickTransparencyConfigurator? = if (isWindows) {
            WindowsCtConfigurator(config)
        } else {
            null
        }

        Platform.runLater {
            val chat = ChatFrame(
                    this,
                    config,
                    deps.get()!!.skinList,
                    lazy { deps.get()!!.guiEventHandler },
                    ctConfigurator
            )

            val backgroundExecutor = deps.get()!!.backgroundExecutorService
            backgroundExecutor.executeWithCatch {
                val eventHandler = deps.get()!!.guiEventHandler
                if (eventHandler is FullGuiEventHandler) {
                    eventHandler.guiFrames.set(GuiFrames(settings, chat))
                }
            }

            // init web engine (fixes flickering)
            chat.clearWebContent()

            showUpdateNotificationOnNewRelease()
        }

        logger.info("GUI loaded")

    }

    private fun showUpdateNotificationOnNewRelease() {
        deps.get()!!.releaseChecker.checkNewRelease { release ->
            Platform.runLater {
                val notification = Alert(AlertType.CONFIRMATION).apply {
                    title = "Update notification"
                    headerText = null
                    graphic = null
                    contentText = "New release available: ${release.version}"
                }
                val stage = notification.dialogPane.scene.window as Stage
                stage.icons.setAll(Images.appIcon)

                val changelogButton = ButtonType("Download", OK_DONE)
                val closeButton = ButtonType("Close", ButtonData.CANCEL_CLOSE)
                notification.buttonTypes.setAll(changelogButton, closeButton)

                val result = notification.showAndWait().get()

                if (result === changelogButton) {
                    hostServices.showDocument(release.releasePageUrl)
                }
            }
        }
    }

}
