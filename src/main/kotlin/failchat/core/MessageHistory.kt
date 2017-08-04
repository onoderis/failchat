package failchat.core

import com.google.common.collect.EvictingQueue
import java.util.Queue
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

/**
 * Thread-safe [EvictingQueue] that uses [ReentrantReadWriteLock] .
 * */
class ConcurrentEvictingQueue<T> : Queue<T> {

    private val lock: ReadWriteLock = ReentrantReadWriteLock()
    private val rlock: Lock = lock.readLock()
    private val wlock: Lock = lock.writeLock()


    override fun element(): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override val size: Int
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun contains(element: T): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun addAll(elements: Collection<T>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun peek(): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEmpty(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun clear() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun remove(element: T): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun remove(): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun add(element: T): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun poll(): T {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun offer(e: T): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun iterator(): MutableIterator<T> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun retainAll(elements: Collection<T>): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}
