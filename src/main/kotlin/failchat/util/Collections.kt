package failchat.util

import java.util.Queue

fun <T> Queue<T>.synchronized(): Queue<T> = SynchronizedQueue(this)

inline fun <reified K : Enum<K>, V> enumMap(): MutableMap<K, V> = java.util.EnumMap<K, V>(K::class.java)
