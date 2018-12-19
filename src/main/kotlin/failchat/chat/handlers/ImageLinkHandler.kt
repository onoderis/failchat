package failchat.chat.handlers

import failchat.ConfigKeys
import failchat.chat.ChatMessage
import failchat.chat.Image
import failchat.chat.Link
import failchat.chat.MessageHandler
import org.apache.commons.configuration2.Configuration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Заменяет элементы типа [Link] на [Image] в зависимости от конфигурации.
 * */
class ImageLinkHandler(private val config: Configuration) : MessageHandler<ChatMessage> {

    private companion object {
        val imageFormats = listOf(".jpg", ".jpeg", ".png", ".gif")
    }

    private val replaceImageLinks = AtomicBoolean(false)

    init {
        reloadConfig()
    }

    override fun handleMessage(message: ChatMessage) {
        if (!replaceImageLinks.get()) return

        message.elements.forEachIndexed { index, element ->
            if (element !is Link) return@forEachIndexed

            val imageFormat = imageFormats.firstOrNull {
                element.fullUrl.endsWith(it, ignoreCase = true)
            }

            if (imageFormat != null) {
                message.replaceElement(index, Image(element))
            }
        }
    }

    fun reloadConfig() {
        replaceImageLinks.set(config.getBoolean(ConfigKeys.showImages))
    }

}
