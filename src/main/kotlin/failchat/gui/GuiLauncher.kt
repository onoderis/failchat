package failchat.gui

import failchat.emoticon.GlobalEmoticonUpdater
import failchat.github.ReleaseChecker
import failchat.kodein
import failchat.platform.windows.WindowsCtConfigurator
import failchat.skin.Skin
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
import org.apache.commons.configuration2.Configuration
import org.kodein.di.generic.instance
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ScheduledExecutorService

class GuiLauncher : Application() {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    override fun start(primaryStage: Stage) {
        val startTime = Instant.now()

        val config = kodein.instance<Configuration>()
        val isWindows = com.sun.jna.Platform.isWindows()

        val settings = SettingsFrame(
                this,
                primaryStage,
                config,
                kodein.instance<List<Skin>>(),
                kodein.instance<Path>("failchatEmoticonsDirectory"),
                isWindows,
                lazy { kodein.instance<GuiEventHandler>() },
                lazy { kodein.instance<GlobalEmoticonUpdater>() }
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
                    kodein.instance<Configuration>(),
                    kodein.instance<List<Skin>>(),
                    lazy { kodein.instance<GuiEventHandler>() },
                    ctConfigurator
            )

            val backgroundExecutor = kodein.instance<ScheduledExecutorService>("background")
            backgroundExecutor.executeWithCatch {
                val eventHandler = kodein.instance<GuiEventHandler>()
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
        kodein.instance<ReleaseChecker>().checkNewRelease { release ->
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
