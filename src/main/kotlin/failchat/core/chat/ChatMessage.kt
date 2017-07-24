package failchat.core.chat

import failchat.core.Origin
import java.time.Instant

/**
 * Сообщение из чата какого-либо первоисточника.
 * */
open class ChatMessage(
        /**
         * Внутренний id, генерируемый приложением.
         * */
        val id: Long,

        /**
         * Первоисточник сообщения.
         * */
        val origin: Origin,

        /**
         * Автор сообщения.
         * */
        val author: String,

        /**
         * Текст сообщения.
         * */
        var text: String,

        /**
         * Время получения сообщения.
         * */
        val timestamp: Instant = Instant.now()
) {

    /**
     * Could contains next elements:
     * - [Emoticon]
     * - [Link]
     * - [Image]
     * */
    val elements: List<Any> get() = _elements

    var highlighted = false

    private val _elements: MutableList<Any> = ArrayList()

    /**
     * @return formatted string for added element.
     * */
    fun addElement(element: Any): String {
        _elements.add(element)
        return "{!${_elements.size - 1}}"
    }

    fun replaceElement(index: Int, replacement: Any): Any? {
        return _elements.set(index, replacement)
    }

}
