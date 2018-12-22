package failchat.gui

import com.github.salomonbrys.kodein.instance
import failchat.emoticon.EmoticonUpdater
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
import org.apache.commons.configuration2.Configuration
import java.nio.file.Path

class GuiLauncher : Application() {

    companion object {
        private val logger = KotlinLogging.logger {}
        val appIcon = Image(GuiLauncher::class.java.getResourceAsStream("/icons/failchat.png"))
    }

    override fun start(primaryStage: Stage) {
        val settings = SettingsFrame(
                this,
                primaryStage,
                kodein.instance<GuiEventHandler>(),
                kodein.instance<Configuration>(),
                kodein.instance<List<Skin>>(),
                kodein.instance<Path>("customEmoticonsDirectory"),
                kodein.instance<EmoticonUpdater>()
        )
        val chat = ChatFrame(
                this,
                kodein.instance<Configuration>(),
                kodein.instance<GuiEventHandler>(),
                kodein.instance<List<Skin>>()
        )

        val eventHandler = kodein.instance<GuiEventHandler>()
        eventHandler.settingsFrame = settings
        eventHandler.chatFrame = chat
        settings.show()

        logger.info("GUI loaded")

        chat.clearWebContent() // init web engine (fixes flickering)

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
