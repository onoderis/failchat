package failchat.gui

import javafx.stage.Stage

interface ClickTransparencyConfigurator {

    fun configureClickTransparency(stage: Stage)

    fun removeClickTransparency(stage: Stage)
}
