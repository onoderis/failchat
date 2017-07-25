package failchat.gui

import com.github.salomonbrys.kodein.instance
import failchat.core.ConfigLoader
import failchat.core.kodein
import failchat.core.skin.Skin
import javafx.application.Application
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
    }

}
