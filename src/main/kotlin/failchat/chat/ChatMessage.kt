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
        val timestamp: Instant = Instant.now()
) {

    /**
     * Could contains next elements:
     * - [Emoticon]
     * - [Link]
     * - [Image]
     * */
    val elements: List<MessageElement>
        get() = mutableElements
    private val mutableElements: MutableList<MessageElement> = ArrayList(5)

    /** Badges of the message. */
    val badges: List<Badge>
        get() = mutableBadges
    private val mutableBadges: MutableList<Badge> = ArrayList(3)

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

    fun addBadge(badge: Badge) {
        mutableBadges.add(badge)
    }

    override fun toString(): String {
        return "ChatMessage(id=$id, origin=$origin, author=$author, text='$text', timestamp=$timestamp, " +
                "badges=$badges, mutableElements=$mutableElements, highlighted=$highlighted)"
    }

}
