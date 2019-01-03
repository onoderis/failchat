package failchat.emoticon

interface EmoticonFactory {
    fun create(id: Long, code: String): Emoticon
}
