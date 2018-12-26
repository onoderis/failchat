package failchat.emoticon

import failchat.Origin
import failchat.util.enumMap
import kotlinx.coroutines.channels.ReceiveChannel
import mu.KLogging

class EmoticonStorage : EmoticonFinder {

    private companion object : KLogging()

    private var originStorages: Map<Origin, OriginEmoticonStorage> = Origin.values
            .map { it to EmptyEmoticonStorage(it) }
            .toMap(enumMap<Origin, OriginEmoticonStorage>())

    fun setStorages(storages: List<OriginEmoticonStorage>) {
        originStorages = Origin.values.minus(storages.map { it.origin })
                .map { EmptyEmoticonStorage(it) }
                .plus(storages)
                .map { it.origin to it }
                .toMap(enumMap<Origin, OriginEmoticonStorage>())
    }

    override fun findByCode(origin: Origin, code: String): Emoticon? {
        return originStorages
                .get(origin)!!
                .findByCode(code)
    }

    override fun findById(origin: Origin, id: String): Emoticon? {
        return originStorages
                .get(origin)!!
                .findById(id)
    }

    override fun getAll(origin: Origin): Collection<Emoticon> {
        return originStorages
                .get(origin)!!
                .getAll()
    }

    fun getCount(origin: Origin): Int {
        return originStorages.get(origin)!!.count()
    }

    fun putMapping(origin: Origin, emoticons: Collection<EmoticonAndId>) {
        originStorages.get(origin)!!
                .putAll(emoticons)
    }

    fun putChannel(origin: Origin, emoticons: ReceiveChannel<EmoticonAndId>) {
        originStorages.get(origin)!!.putAll(emoticons)
    }

    fun clear(origin: Origin) {
        originStorages.get(origin)!!.clear()
    }

}
