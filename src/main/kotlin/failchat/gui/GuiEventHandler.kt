package failchat.gui

import com.fasterxml.jackson.databind.ObjectMapper
import failchat.AppStateManager
import failchat.ws.server.WsServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

/**
 * Перехватывает задачи от JavaFX треда и выполняет их в отдельном пуле. Позволяет снизить время отклика
 * графического интерфейса.
 * */
class GuiEventHandler(
        private val wsServer: WsServer,
        private val appStateManager: AppStateManager,
        private val objectMapper: ObjectMapper = ObjectMapper()
) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GuiEventHandler::class.java)
    }

    private val executor = Executors.newSingleThreadExecutor()

    fun startChat() {
        executor.submit {
            appStateManager.startChat()
        }
    }

    fun stopChat() {
        executor.submit {
            appStateManager.stopChat()
        }
    }

    fun shutDown() {
        executor.submit {
            appStateManager.shutDown()
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

            wsServer.send(messageNode.toString())
        }
    }

}
