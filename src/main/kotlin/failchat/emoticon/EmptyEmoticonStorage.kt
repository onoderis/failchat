package failchat.emoticon

import failchat.Origin
import kotlinx.coroutines.experimental.channels.ReceiveChannel

class EmptyEmoticonStorage(override val origin: Origin) : OriginEmoticonStorage {
    override fun findByCode(code: String): Emoticon? = null
    override fun findById(id: String): Emoticon? = null
    override fun getAll(): Collection<Emoticon> = emptyList()
    override fun count(): Int = 0
    override fun putAll(emoticons: List<EmoticonAndId>) = throwOnPut()
    override fun putAll(emoticons: ReceiveChannel<EmoticonAndId>) = throwOnPut()
    override fun clear() {}
    private fun throwOnPut(): Nothing = throw IllegalStateException("Emoticons should not be put in EmptyEmoticonStorage")
}
