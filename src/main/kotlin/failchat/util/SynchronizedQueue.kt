package failchat.util

import java.util.Queue

class SynchronizedQueue<E>(private val delegate: Queue<E>) : Queue<E> {
    
    private val mutex = Any()
    
    override val size: Int
        get() = synchronized(mutex) { delegate.size }
    override fun isEmpty() = synchronized(mutex) { delegate.isEmpty() }
    override fun remove(element: E) = synchronized(mutex) { delegate.remove(element) }
    override fun remove(): E = synchronized(mutex) { delegate.remove() }
    override fun iterator() = synchronized(mutex) { delegate.iterator() }
    override fun retainAll(elements: Collection<E>) = synchronized(mutex) { delegate.retainAll(elements) }
    override fun peek(): E? = synchronized(mutex) { delegate.peek() }
    override fun poll(): E? = synchronized(mutex) { delegate.poll() }
    override fun clear() = synchronized(mutex) { delegate.clear() }
    override fun add(element: E) = synchronized(mutex) { delegate.add(element) }
    override fun offer(element: E) = synchronized(mutex) { delegate.offer(element) }
    override fun contains(element: E) = synchronized(mutex) { delegate.contains(element) }
    override fun containsAll(elements: Collection<E>) = synchronized(mutex) { delegate.containsAll(elements) }
    override fun addAll(elements: Collection<E>) = synchronized(mutex) { delegate.addAll(elements) }
    override fun removeAll(elements: Collection<E>) = synchronized(mutex) { delegate.removeAll(elements) }
    override fun element(): E = synchronized(mutex) { delegate.element() }
}
