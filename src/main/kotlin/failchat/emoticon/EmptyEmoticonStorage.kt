package failchat.emoticon

import failchat.Origin
import kotlinx.coroutines.channels.ReceiveChannel
import mu.KLogging

class EmptyEmoticonStorage(override val origin: Origin) : OriginEmoticonStorage {
    private companion object : KLogging()

    override fun findByCode(code: String): Emoticon? = null
    override fun findById(id: String): Emoticon? = null
    override fun getAll(): Collection<Emoticon> = emptyList()
    override fun count(): Int = 0
    override fun putAll(emoticons: Collection<EmoticonAndId>) = warnOnPut()
    override fun putAll(emoticons: ReceiveChannel<EmoticonAndId>) = warnOnPut()
    override fun clear() {}
    private fun warnOnPut() {
        logger.warn("Put operation in not supported by EmptyEmoticonStorage. origin: {}", origin)
    }
}
