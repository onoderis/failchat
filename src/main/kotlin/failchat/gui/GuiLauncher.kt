package failchat.gui

import com.github.salomonbrys.kodein.instance
import failchat.ConfigLoader
import failchat.github.ReleaseChecker
import failchat.kodein
import failchat.skin.Skin
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control.ButtonBar.ButtonData.OK_DONE
import javafx.scene.control.ButtonType
import javafx.scene.image.Image
import javafx.stage.Stage
import mu.KotlinLogging
import java.nio.file.Path

class GuiLauncher : Application() {

    companion object {
        private val logger = KotlinLogging.logger {}
        val appIcon = Image(GuiLauncher::class.java.getResourceAsStream("/icons/failchat.png"))
    }

    override fun start(primaryStage: Stage) {
        val settings = SettingsFrame(
                primaryStage,
                kodein.instance<GuiEventHandler>(),
                kodein.instance<ConfigLoader>().get(),
                kodein.instance<List<Skin>>(),
                kodein.instance<Path>("customEmoticonsDirectory")
        )
        val chat = ChatFrame(
                kodein.instance<ConfigLoader>().get(),
                kodein.instance<GuiEventHandler>(),
                kodein.instance<List<Skin>>()
        )

        settings.chat = chat
        settings.app = this
        chat.settings = settings
        chat.app = this

        settings.show()
        logger.info("GUI loaded")

        chat.clearWebContent() // init web engine (fix flickering)

        showUpdateNotificationOnNewRelease()

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
                stage.icons.setAll(appIcon)

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
