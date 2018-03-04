package failchat.chat

import failchat.Origin
import failchat.emoticon.Emoticon
import java.time.Instant

/**
 * Сообщение из чата какого-либо первоисточника.
 * */
open class ChatMessage(
        /** Внутренний id, генерируемый приложением. */
        val id: Long,

        /** Первоисточник сообщения. */
        val origin: Origin,

        /** Автор сообщения. */
        val author: Author,

        /** Текст сообщения. */
        var text: String,

        /** Время получения сообщения. */
        val timestamp: Instant = Instant.now(),

        /** Initial list of elements. */
        elements: List<MessageElement> = emptyList()
) {

    /**
     * Could contains next elements:
     * - [Emoticon]
     * - [Link]
     * - [Image]
     * */
    val elements: List<MessageElement> get() = mutableElements
    private val mutableElements: MutableList<MessageElement> = elements.toMutableList()

    var highlighted = false

    /**
     * @return formatted string for added element.
     * */
    fun addElement(element: MessageElement): String {
        mutableElements.add(element)
        return Elements.label(mutableElements.size - 1)
    }

    fun replaceElement(index: Int, replacement: MessageElement): Any? {
        return mutableElements.set(index, replacement)
    }

    override fun toString(): String {
        return "ChatMessage(id=$id, origin=$origin, author=$author, text='$text', timestamp=$timestamp, " +
                "highlighted=$highlighted, mutableElements=$mutableElements)"
    }

}
