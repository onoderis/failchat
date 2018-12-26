package failchat.gui

import failchat.FcServerInfo
import javafx.application.Application
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.stage.Stage

class PortBindAlert : Application() {

    override fun start(primaryStage: Stage) {
        val alert = Alert(AlertType.ERROR)

        alert.title = "Launch error"
        alert.headerText = "Looks like failchat is already running."
        alert.contentText = "Failed to create socket at ${FcServerInfo.host.hostAddress}:${FcServerInfo.port}"

        val stage = alert.dialogPane.scene.window as Stage
        stage.icons.setAll(Images.appIcon)

        alert.showAndWait()
    }
}
