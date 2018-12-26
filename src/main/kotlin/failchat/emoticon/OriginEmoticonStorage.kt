package failchat.emoticon

import failchat.Origin
import kotlinx.coroutines.channels.ReceiveChannel

interface OriginEmoticonStorage {

    val origin: Origin

    fun findByCode(code: String): Emoticon?

    fun findById(id: String): Emoticon?

    fun getAll(): Collection<Emoticon>

    fun count(): Int

    fun putAll(emoticons: Collection<EmoticonAndId>)

    /** Put all emoticons in storage from the channel. Blocking call. */
    fun putAll(emoticons: ReceiveChannel<EmoticonAndId>)

    fun clear()
}


