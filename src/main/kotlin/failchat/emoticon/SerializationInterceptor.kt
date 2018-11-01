package failchat.emoticon

interface SerializationInterceptor<K, V> {

    fun serialize(emoticon: Emoticon): Any

    fun dezerialize(storedValue: Any): Emoticon

}
