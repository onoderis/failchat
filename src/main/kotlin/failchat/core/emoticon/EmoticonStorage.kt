package failchat.core.emoticon

import failchat.core.Origin
import java.util.EnumMap

class EmoticonStorage : EmoticonFinder {

    private val codeToEmoticon: MutableMap<Origin, Map<String, Emoticon>> = EnumMap(Origin::class.java)
    private val idToEmoticon: MutableMap<Origin, Map<Long, Emoticon>> = EnumMap(Origin::class.java)

    override fun findByCode(origin: Origin, code: String): Emoticon? = codeToEmoticon.get(origin)?.get(code)

    override fun findById(origin: Origin, id: Long): Emoticon? = idToEmoticon.get(origin)?.get(id)

    fun putByCode(origin: Origin, emoticons: Map<String, Emoticon>): Unit {
        codeToEmoticon.put(origin, emoticons)
    }

    fun putById(origin: Origin, emoticons: Map<Long, Emoticon>):Unit {
        idToEmoticon.put(origin, emoticons)
    }

}
