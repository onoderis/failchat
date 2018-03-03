package failchat.emoticon

import failchat.Origin
import java.util.concurrent.ConcurrentHashMap

class EmoticonStorage : EmoticonFinder {

    private val emoticonLists: MutableMap<Origin, List<Emoticon>> = ConcurrentHashMap()
    private val codeToEmoticon: MutableMap<Origin, Map<String, Emoticon>> = ConcurrentHashMap()
    private val idToEmoticon: MutableMap<Origin, Map<Long, Emoticon>> = ConcurrentHashMap()

    override fun findByCode(origin: Origin, code: String): Emoticon? = codeToEmoticon.get(origin)?.get(code)

    override fun findById(origin: Origin, id: Long): Emoticon? = idToEmoticon.get(origin)?.get(id)

    override fun getList(origin: Origin): List<Emoticon> = emoticonLists.get(origin) ?: emptyList()

    fun putCodeMapping(origin: Origin, emoticons: Map<String, Emoticon>) {
        codeToEmoticon[origin] = emoticons
    }

    fun putIdMapping(origin: Origin, emoticons: Map<Long, Emoticon>) {
        idToEmoticon[origin] = emoticons
    }

    fun putList(origin: Origin, emoticons: List<Emoticon>) {
        emoticonLists[origin] = emoticons
    }
}
