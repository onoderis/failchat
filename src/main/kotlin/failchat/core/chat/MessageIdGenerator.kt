package failchat.core.chat

import java.util.concurrent.atomic.AtomicLong

class MessageIdGenerator(lastId: Long) {

    private val _lastId: AtomicLong = AtomicLong(lastId)

    val lastId: Long get() = _lastId.get()

    fun generate() = _lastId.getAndIncrement()
}
