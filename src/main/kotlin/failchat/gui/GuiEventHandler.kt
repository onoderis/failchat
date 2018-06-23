package failchat.gui

import failchat.AppStateManager
import failchat.chat.ChatMessageSender
import failchat.util.executeWithCatch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

/**
 * Перехватывает задачи от JavaFX треда и выполняет их в отдельном пуле. Позволяет снизить время отклика
 * графического интерфейса.
 * */
class GuiEventHandler(
        private val appStateManager: AppStateManager,
        private val messageSender: ChatMessageSender
) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(GuiEventHandler::class.java)
    }

    private val executor = Executors.newSingleThreadExecutor()

    fun startChat() {
        executor.executeWithCatch {
            appStateManager.startChat()
        }
    }

    fun stopChat() {
        executor.executeWithCatch {
            appStateManager.stopChat()
        }
    }

    fun shutDown() {
        executor.executeWithCatch {
            appStateManager.shutDown()
        }
        executor.shutdown()
    }

    fun notifyConfigurationChanged() {
        executor.executeWithCatch {
            messageSender.sendClientConfiguration()
        }
    }

}
