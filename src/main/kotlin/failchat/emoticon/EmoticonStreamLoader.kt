package failchat.emoticon

import failchat.Origin
import kotlinx.coroutines.experimental.channels.ReceiveChannel

interface EmoticonStreamLoader<T : Emoticon> {
    val origin: Origin
    fun loadEmoticons(): ReceiveChannel<T>
}
