package failchat.emoticon

import failchat.Origin
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

class EmoticonCodeMemoryStorage(
        override val origin: Origin,
        private val caseSensitiveCode: Boolean
) : OriginEmoticonStorage {

    private val codeMap: MutableMap<String, Emoticon> = ConcurrentHashMap()

    override fun findByCode(code: String): Emoticon? {
        val cCode = if (caseSensitiveCode) code else code.toLowerCase()
        return codeMap.get(cCode)
    }

    override fun findById(id: String): Emoticon? {
        val cId = if (caseSensitiveCode) id else id.toLowerCase()
        return codeMap.get(cId)
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
        val code = emoticonAndId.emoticon.code.let {
            if (caseSensitiveCode) it else it.toLowerCase()
        }

        codeMap.put(code, emoticonAndId.emoticon)
    }

    override fun clear() {
        codeMap.clear()
    }
}
