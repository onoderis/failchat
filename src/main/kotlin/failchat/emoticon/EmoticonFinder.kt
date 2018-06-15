package failchat.emoticon

import failchat.Origin

interface EmoticonFinder {

    /**
     * Find emoticon by code.
     * @param code case sensitive code of [Emoticon].
     * */
    fun findByCode(origin: Origin, code: String): Emoticon?

    fun findById(origin: Origin, id: Long): Emoticon?

    fun getList(origin: Origin): List<Emoticon>

}
