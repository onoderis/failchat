package failchat.gui

import failchat.core.viewers.ViewersCounter

class GuiEventHandler(private val viewersCounter: ViewersCounter) {

    fun fireViewersCountToggle() {
        viewersCounter.sendViewersCountWsMessage()
    }

}