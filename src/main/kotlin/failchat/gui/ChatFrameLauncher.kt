package failchat.gui

import failchat.Dependencies
import failchat.platform.windows.WindowsCtConfigurator
import failchat.util.LateinitVal
import javafx.application.Application
import javafx.stage.Stage

class ChatFrameLauncher : Application() {

    companion object {
        val deps = LateinitVal<Dependencies>()
    }

    override fun start(primaryStage: Stage) {
        //todo remove copypaste
        val config = deps.get()!!.configuration

        val isWindows = com.sun.jna.Platform.isWindows()
        val ctConfigurator: ClickTransparencyConfigurator? = if (isWindows) {
            WindowsCtConfigurator(config)
        } else {
            null
        }

        val chat = ChatFrame(
                this,
                config,
                deps.get()!!.skinList,
                lazy { deps.get()!!.guiEventHandler },
                ctConfigurator
        )

        // init web engine (fixes flickering)
        chat.clearWebContent()

        chat.show()
    }

}
