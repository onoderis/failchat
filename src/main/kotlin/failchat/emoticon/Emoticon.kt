package failchat.emoticon

import failchat.Origin
import failchat.chat.ImageFormat
import failchat.chat.MessageElement
import java.io.Serializable

abstract class Emoticon(
        val origin: Origin,
        val code: String,
        val format: ImageFormat
) : MessageElement, Serializable {

    abstract val url: String

}
