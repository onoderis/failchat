package failchat.emoticon

import failchat.Origin
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import org.mapdb.DB
import org.mapdb.HTreeMap
import org.mapdb.Serializer
import org.mapdb.serializer.GroupSerializer

open class EmoticonCodeIdDbStorage(
        db: DB,
        override val origin: Origin
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
        val id = codeToId.get(code) ?: return null
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
            idToEmoticon.put(it.id, it.emoticon)
            codeToId.put(it.emoticon.code, it.id)
        }
    }

    override fun putAll(emoticons: ReceiveChannel<EmoticonAndId>) {
        runBlocking {
            for (emoticonToId in emoticons) {
                idToEmoticon.put(emoticonToId.id, emoticonToId.emoticon)
                codeToId.putIfAbsent(emoticonToId.emoticon.code, emoticonToId.id)
            }
        }
    }

    override fun clear() {
        idToEmoticon.clear()
        codeToId.clear()
    }
}
