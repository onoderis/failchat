package failchat.emoticon

import failchat.Origin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class EmoticonCodeIdMemoryStorage(override val origin: Origin) : OriginEmoticonStorage {

    private val idMap: MutableMap<String, Emoticon> = ConcurrentHashMap()
    private val codeMap: MutableMap<String, Emoticon> = ConcurrentHashMap()

    override fun findByCode(code: String): Emoticon? {
        return codeMap.get(code)
    }

    override fun findById(id: String): Emoticon? {
        return idMap.get(id)
    }

    override fun getAll(): Collection<Emoticon> {
        return idMap.values
    }

    override fun count(): Int {
        return idMap.size
    }

    override fun putAll(emoticons: Collection<EmoticonAndId>) {
        emoticons.forEach {
            putEmoticon(it)
        }
    }

    override fun putAll(emoticons: Flow<EmoticonAndId>) {
        runBlocking {
            emoticons.collect {
                putEmoticon(it)
            }
        }
    }

    private fun putEmoticon(emoticonAndId: EmoticonAndId) {
        idMap.put(emoticonAndId.id, emoticonAndId.emoticon)
        codeMap.put(emoticonAndId.emoticon.code, emoticonAndId.emoticon)
    }

    override fun clear() {
        idMap.clear()
        codeMap.clear()
    }

}
