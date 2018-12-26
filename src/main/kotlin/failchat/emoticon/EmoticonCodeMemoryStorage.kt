package failchat.emoticon

import failchat.Origin
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class EmoticonCodeMemoryStorage(override val origin: Origin) : OriginEmoticonStorage {

    private val codeMap: MutableMap<String, Emoticon> = ConcurrentHashMap()

    override fun findByCode(code: String): Emoticon? {
        return codeMap.get(code)
    }

    override fun findById(id: String): Emoticon? {
        return codeMap.get(id)
    }

    override fun getAll(): Collection<Emoticon> {
        return codeMap.values
    }

    override fun count(): Int {
        return codeMap.size
    }

    override fun putAll(emoticons: Collection<EmoticonAndId>) {
        emoticons.forEach {
            putEmoticon(it)
        }
    }

    override fun putAll(emoticons: ReceiveChannel<EmoticonAndId>) {
        runBlocking {
            for (emoticon in emoticons) {
                putEmoticon(emoticon)
            }
        }
    }

    private fun putEmoticon(emoticonAndId: EmoticonAndId) {
        codeMap.put(emoticonAndId.emoticon.code, emoticonAndId.emoticon)
    }

    override fun clear() {
        codeMap.clear()
    }
}
