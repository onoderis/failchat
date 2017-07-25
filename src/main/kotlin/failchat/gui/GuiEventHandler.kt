package failchat.gui

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.core.AppStateTransitionManager
import failchat.core.ws.server.WsServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

/**
 * Перехватывает задачи от JavaFX треда и выполняет их в отдельном пуле. Позволяет снизить время отклика
 * графического интерфейса.
 * */
class GuiEventHandler(
        private val wsServer: WsServer,
        private val appStateTransitionManager: AppStateTransitionManager,
        private val objectMapper: ObjectMapper = ObjectMapper()
) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GuiEventHandler::class.java)
    }

    private val executor = Executors.newSingleThreadExecutor()

    fun startChat() {
        executor.submit {
            appStateTransitionManager.startChat()
        }
    }

    fun stopChat() {
        executor.submit {
            appStateTransitionManager.stopChat()
        }
    }

    fun shutDown() {
        executor.submit {
            appStateTransitionManager.shutDown()
        }
        executor.shutdown()
    }

    fun notifyViewersCountToggled(show: Boolean) {
        executor.submit {
            val messageNode = objectMapper.createObjectNode().apply {
                put("type", "show-viewers-count")
                putObject("content").apply {
                    put("show", show)
                }
            }

            wsServer.sendToAll(messageNode.toString())
        }
    }

}
