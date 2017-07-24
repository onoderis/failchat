package failchat.gui

import failchat.core.viewers.ViewersCounter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

//todo volatile?
class GuiEventHandler(var viewersCounter: ViewersCounter?) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GuiEventHandler::class.java)
    }

    fun fireViewersCountToggle() {
        viewersCounter
                ?.sendViewersCountWsMessage()
                ?: log.debug("Viewers count toggle event won't be handled, cause: viewersCounter is null")
    }

}
