package failchat.emoticon

import failchat.Origin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.mapdb.DB
import org.mapdb.HTreeMap
import org.mapdb.Serializer
import java.io.Closeable

/** Search by code is case insensitive. */
class EmoticonCodeIdDbCompactStorage(
        db: DB,
        override val origin: Origin,
        private val emoticonFactory: EmoticonFactory
) : OriginEmoticonStorage, Closeable {

    private val lowerCaseCodeToId: HTreeMap<String, String>
    private val idToNormalCaseCode: HTreeMap<String, String>

    init {
        lowerCaseCodeToId = db
                .hashMap(origin.commonName + "-lowerCaseCodeToId", Serializer.STRING, Serializer.STRING)
                .createOrOpen()
        idToNormalCaseCode = db
                .hashMap(origin.commonName + "-idToNormalCaseCode", Serializer.STRING, Serializer.STRING)
                .createOrOpen()
    }

    override fun findByCode(code: String): Emoticon? {
        val id = lowerCaseCodeToId.get(code.lowercase()) ?: return null
        val normalCaseCode = idToNormalCaseCode.get(id) ?: return null
        return emoticonFactory.create(id, normalCaseCode)
    }

    override fun findById(id: String): Emoticon? {
        val normalCaseCode = idToNormalCaseCode.get(id) ?: return null
        return emoticonFactory.create(id, normalCaseCode)
    }

    override fun getAll(): Collection<Emoticon> {
        throw NotImplementedError()
    }

    override fun count(): Int {
        return idToNormalCaseCode.size
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
        idToNormalCaseCode.put(emoticonAndId.id, emoticonAndId.emoticon.code)
        lowerCaseCodeToId.putIfAbsent(emoticonAndId.emoticon.code.lowercase(), emoticonAndId.id)
    }

    override fun clear() {
        idToNormalCaseCode.clear()
        lowerCaseCodeToId.clear()
    }

    override fun close() {
        lowerCaseCodeToId.close()
        idToNormalCaseCode.close()
    }
}
