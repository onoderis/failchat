package failchat.gui

import failchat.kodein
import failchat.platform.windows.WindowsCtConfigurator
import failchat.skin.Skin
import javafx.application.Application
import javafx.stage.Stage
import org.apache.commons.configuration2.Configuration
import org.kodein.di.instance

class ChatFrameLauncher : Application() {

    override fun start(primaryStage: Stage) {
        //todo remove copypaste
        val config = kodein.instance<Configuration>()

        val isWindows = com.sun.jna.Platform.isWindows()
        val ctConfigurator: ClickTransparencyConfigurator? = if (isWindows) {
            WindowsCtConfigurator(config)
        } else {
            null
        }

        val chat = ChatFrame(
                this,
                kodein.instance<Configuration>(),
                kodein.instance<List<Skin>>(),
                lazy { kodein.instance<GuiEventHandler>() },
                ctConfigurator
        )

        // init web engine (fixes flickering)
        chat.clearWebContent()

        chat.show()
    }

}
