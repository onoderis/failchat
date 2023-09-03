package failchat.emoticon

interface EmoticonFactory {
    fun create(id: String, code: String): Emoticon
}
