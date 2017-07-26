package failchat.gui

import com.github.salomonbrys.kodein.instance
import failchat.core.ConfigLoader
import failchat.core.kodein
import failchat.core.skin.Skin
import failchat.github.ReleaseChecker
import javafx.application.Application
import javafx.application.Platform
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control.ButtonBar.ButtonData.OK_DONE
import javafx.scene.control.ButtonType
import javafx.scene.image.Image
import javafx.stage.Stage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path


class GuiLauncher : Application() {

    companion object {
        val appIcon = Image(GuiLauncher::class.java.getResourceAsStream("/icons/failchat.png"))
        val log: Logger = LoggerFactory.getLogger(GuiLauncher::class.java)
    }

    override fun start(primaryStage: Stage) {
        val settings = SettingsFrame(primaryStage, kodein.instance<GuiEventHandler>(),
                kodein.instance<ConfigLoader>().get(), kodein.instance<List<Skin>>())
        val chat = ChatFrame(kodein.instance<ConfigLoader>().get(), kodein.instance<GuiEventHandler>(),
                kodein.instance<Path>("workingDirectory"))

        settings.chat = chat
        chat.settings = settings
        chat.app = this

        settings.show()
        log.info("GUI loaded")

        showUpdateNotificationOnNewRelease()

    }

    private fun showUpdateNotificationOnNewRelease() {
        kodein.instance<ReleaseChecker>().checkNewRelease { release ->
            Platform.runLater {
                val alert = Alert(AlertType.CONFIRMATION).apply {
                    title = "Update notification"
                    headerText = null
                    graphic = null
                    contentText = "New release available: version ${release.version}"
                }

                val changelogButton = ButtonType("Changelog", OK_DONE)
                val closeButton = ButtonType("Close", ButtonData.CANCEL_CLOSE)
                alert.buttonTypes.setAll(changelogButton, closeButton)

                val result = alert.showAndWait().get()

                if (result === changelogButton) {
                    hostServices.showDocument(release.releasePageUrl)
                }
            }
        }
    }

}
