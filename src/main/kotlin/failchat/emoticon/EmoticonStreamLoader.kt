package failchat.emoticon

import failchat.Origin
import kotlinx.coroutines.channels.ReceiveChannel

interface EmoticonStreamLoader<T : Emoticon> {
    val origin: Origin
    fun loadEmoticons(): ReceiveChannel<T>
}
