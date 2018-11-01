package failchat.emoticon

import failchat.Origin

interface EmoticonFinder {

    /**
     * Find emoticon by code.
     * @param code case sensitive code of [Emoticon].
     * */
    fun findByCode(origin: Origin, code: String): Emoticon?

    //todo make id Any?
    fun findById(origin: Origin, id: String): Emoticon?

    fun getAll(origin: Origin): Collection<Emoticon>

}
