package failchat.util

import java.util.Queue

fun <T> Queue<T>.synchronized(): Queue<T> = SynchronizedQueue(this)
