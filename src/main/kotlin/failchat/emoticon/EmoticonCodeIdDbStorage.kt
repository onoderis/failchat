package failchat.emoticon

import failchat.Origin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.mapdb.DB
import org.mapdb.HTreeMap
import org.mapdb.Serializer
import org.mapdb.serializer.GroupSerializer

class EmoticonCodeIdDbStorage(
        db: DB,
        override val origin: Origin,
        private val caseSensitiveCode: Boolean
) : OriginEmoticonStorage {

    private val codeToId: HTreeMap<String, String>
    private val idToEmoticon: HTreeMap<String, Emoticon>

    init {
        codeToId = db
                .hashMap(origin.commonName + "-codeToId", Serializer.STRING, Serializer.STRING)
                .createOrOpen()
        idToEmoticon = db
                .hashMap(origin.commonName + "-idToEmoticon", Serializer.STRING, Serializer.JAVA as GroupSerializer<Emoticon>)
                .createOrOpen()
    }

    override fun findByCode(code: String): Emoticon? {
        val cCode = if (caseSensitiveCode) code else code.toLowerCase()
        val id = codeToId.get(cCode) ?: return null
        return idToEmoticon.get(id)
    }

    override fun findById(id: String): Emoticon? {
        return idToEmoticon.get(id)
    }

    override fun getAll(): Collection<Emoticon> {
        return idToEmoticon.values.filterNotNull()
    }

    override fun count(): Int {
        return idToEmoticon.size
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
        val code = emoticonAndId.emoticon.code.let { c ->
            if (caseSensitiveCode) c else c.toLowerCase()
        }

        idToEmoticon.put(emoticonAndId.id, emoticonAndId.emoticon)
        codeToId.put(code, emoticonAndId.id)
    }

    override fun clear() {
        idToEmoticon.clear()
        codeToId.clear()
    }
}
