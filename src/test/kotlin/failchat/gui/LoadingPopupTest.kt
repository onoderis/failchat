package failchat.gui

import javafx.application.Application
import javafx.stage.Popup
import javafx.stage.Stage

class App : Application() {
    override fun start(primaryStage: Stage) {
        val loadingPopup = Popup()
        loadingPopup.scene



        loadingPopup.show(primaryStage)
        primaryStage.show()

    }

}
