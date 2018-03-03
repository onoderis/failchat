package failchat.gui

import javafx.scene.control.TextField

fun TextField.configureChannelField(editable: Boolean) {
    if (editable) {
        style = ""
        isEditable = true
    } else {
        style = "-fx-background-color: lightgrey"
        isEditable = false
    }
}