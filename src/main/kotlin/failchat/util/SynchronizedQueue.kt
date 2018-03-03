package failchat.util

import java.util.Queue
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class SynchronizedQueue<E>(private val delegate: Queue<E>) : Queue<E> {

    private val lock: Lock = ReentrantLock()

    override val size: Int
        get() = lock.withLock { delegate.size }
    override fun isEmpty() = lock.withLock { delegate.isEmpty() }
    override fun remove(element: E) = lock.withLock { delegate.remove(element) }
    override fun remove(): E = lock.withLock { delegate.remove() }
    override fun iterator() = lock.withLock { delegate.iterator() }
    override fun retainAll(elements: Collection<E>) = lock.withLock { delegate.retainAll(elements) }
    override fun peek(): E? = lock.withLock { delegate.peek() }
    override fun poll(): E? = lock.withLock { delegate.poll() }
    override fun clear() = lock.withLock { delegate.clear() }
    override fun add(element: E) = lock.withLock { delegate.add(element) }
    override fun offer(element: E) = lock.withLock { delegate.offer(element) }
    override fun contains(element: E) = lock.withLock { delegate.contains(element) }
    override fun containsAll(elements: Collection<E>) = lock.withLock { delegate.containsAll(elements) }
    override fun addAll(elements: Collection<E>) = lock.withLock { delegate.addAll(elements) }
    override fun removeAll(elements: Collection<E>) = lock.withLock { delegate.removeAll(elements) }
    override fun element(): E = lock.withLock { delegate.element() }
}
