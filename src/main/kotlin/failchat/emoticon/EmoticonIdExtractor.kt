package failchat.emoticon

import failchat.Origin

interface EmoticonIdExtractor<T : Emoticon> {
    val origin: Origin
    fun extractId(emoticon: T): String
}
