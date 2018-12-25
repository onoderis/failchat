package failchat.util

import java.util.EnumMap
import java.util.EnumSet

inline fun <reified K : Enum<K>, V> enumMap(): EnumMap<K, V> = java.util.EnumMap<K, V>(K::class.java)
inline fun <reified T : Enum<T>> enumSet(): EnumSet<T> = java.util.EnumSet.noneOf(T::class.java)
