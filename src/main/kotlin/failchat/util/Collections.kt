package failchat.util

inline fun <reified K : Enum<K>, V> enumMap(): MutableMap<K, V> = java.util.EnumMap<K, V>(K::class.java)
inline fun <reified T : Enum<T>> enumSet(): MutableSet<T> = java.util.EnumSet.noneOf(T::class.java)
