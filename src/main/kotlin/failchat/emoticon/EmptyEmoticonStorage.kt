package failchat.emoticon

import failchat.Origin
import kotlinx.coroutines.flow.Flow
import mu.KotlinLogging

class EmptyEmoticonStorage(override val origin: Origin) : OriginEmoticonStorage {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    override fun findByCode(code: String): Emoticon? = null
    override fun findById(id: String): Emoticon? = null
    override fun getAll(): Collection<Emoticon> = emptyList()
    override fun count(): Int = 0
    override fun putAll(emoticons: Collection<EmoticonAndId>) = warnOnPut()
    override fun putAll(emoticons: Flow<EmoticonAndId>) = warnOnPut()
    override fun clear() {}
    private fun warnOnPut() {
        logger.warn("Put operation in not supported by EmptyEmoticonStorage. origin: {}", origin)
    }
}
