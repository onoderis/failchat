package failchat.emoticon

import failchat.Origin
import failchat.Origin.TWITCH
import failchat.twitch.TwitchEmoticon
import failchat.twitch.TwitchEmoticonUrlFactory
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.runBlocking
import org.mapdb.DB
import org.mapdb.HTreeMap
import org.mapdb.Serializer
import java.io.Closeable

class TwitchEmoticonStorage(
        db: DB,
        private val twitchEmoticonUrlFactory: TwitchEmoticonUrlFactory
) : OriginEmoticonStorage, Closeable {

    private val lowerCaseCodeToId: HTreeMap<String, Long>
    private val idToNormalCaseCode: HTreeMap<Long, String>

    init {
        lowerCaseCodeToId = db
                .hashMap(TWITCH.commonName + "-lowerCaseCodeToId", Serializer.STRING, Serializer.LONG)
                .createOrOpen()
        idToNormalCaseCode = db
                .hashMap(TWITCH.commonName + "-idToNormalCaseCode", Serializer.LONG, Serializer.STRING)
                .createOrOpen()
    }

    override val origin = Origin.TWITCH

    override fun findByCode(code: String): Emoticon? {
        val id = lowerCaseCodeToId.get(code.toLowerCase()) ?: return null
        val normalCaseCode = idToNormalCaseCode.get(id) ?: return null
        return TwitchEmoticon(id, normalCaseCode, twitchEmoticonUrlFactory)
    }

    override fun findById(id: String): Emoticon? {
        val idLong = id.toLong()
        val code = idToNormalCaseCode.get(idLong) ?: return null
        return TwitchEmoticon(idLong, code, twitchEmoticonUrlFactory)
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

    override fun putAll(emoticons: ReceiveChannel<EmoticonAndId>) {
        runBlocking {
            for (emoticonToId in emoticons) {
                putEmoticon(emoticonToId)
            }
        }
    }

    private fun putEmoticon(emoticonAndId: EmoticonAndId) {
        idToNormalCaseCode.put(emoticonAndId.id.toLong(), emoticonAndId.emoticon.code)
        lowerCaseCodeToId.putIfAbsent(emoticonAndId.emoticon.code.toLowerCase(), emoticonAndId.id.toLong())
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
