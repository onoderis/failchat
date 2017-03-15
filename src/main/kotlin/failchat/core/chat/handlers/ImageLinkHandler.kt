package failchat.core.chat.handlers

import failchat.core.chat.ChatMessage
import failchat.core.chat.Image
import failchat.core.chat.Link
import failchat.core.chat.MessageHandler
import org.apache.commons.configuration.CompositeConfiguration
import org.apache.commons.lang.StringUtils
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Заменяет элементы типа [Link] на [Image] в зависимости от конфигурации.
 * */
class ImageLinkHandler(private val config: CompositeConfiguration) : MessageHandler<ChatMessage> {

    private companion object {
        val imageFormats = arrayOf(".jpg", ".jpeg", ".png", ".gif")
    }

    var replaceImageLinks = AtomicBoolean(false)

    init {
        reloadConfig()
    }

    override fun handleMessage(message: ChatMessage) {
        if (!replaceImageLinks.get()) return

        message.elements.forEachIndexed { index, element ->
            if (element !is Link) return@forEachIndexed

            if (StringUtils.endsWithAny(element.fullUrl, imageFormats)) {
                message.replaceElement(index, Image(element))
            }
        }
    }

    fun reloadConfig() {
        replaceImageLinks.set(config.getBoolean("show-images"))
    }

}