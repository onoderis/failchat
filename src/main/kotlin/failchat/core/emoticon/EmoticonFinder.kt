package failchat.core.emoticon

import failchat.core.Origin

interface EmoticonFinder {

    /**
     * Find by emoticon code.
     * @param code case sensitive code of [Emoticon].
     * */
    fun findByCode(origin: Origin, code: String): Emoticon?

    fun findById(origin: Origin, id: Long): Emoticon?

}
