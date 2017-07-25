package failchat.gui

import com.github.salomonbrys.kodein.instance
import failchat.core.ConfigLoader
import failchat.core.kodein
import javafx.application.Application
import javafx.scene.image.Image
import javafx.stage.Stage
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class GuiLauncher : Application() {

    companion object {
        val appIcon = Image(GuiLauncher::class.java.getResourceAsStream("/icons/failchat.png"))
        val log: Logger = LoggerFactory.getLogger(GuiLauncher::class.java)
    }

    @Throws(Exception::class)
    override fun start(primaryStage: Stage) {
        val settings = SettingsFrame(primaryStage, kodein.instance(),
                kodein.instance<ConfigLoader>().get(), kodein.instance())
        val chat = ChatFrame(kodein.instance<ConfigLoader>().get(), kodein.instance(),
                kodein.instance(), kodein.instance("workingDirectory"))

        settings.chat = chat
        chat.settings = settings
        chat.app = this

        settings.show()
        log.info("GUI loaded")
    }

}
