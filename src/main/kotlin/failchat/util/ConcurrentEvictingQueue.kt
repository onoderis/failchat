package failchat.util

import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

class ConcurrentEvictingQueue<T>(
        private val maxSize: Int,
        private val queue: ConcurrentLinkedQueue<T> = ConcurrentLinkedQueue()
) : Queue<T> by queue {

    override fun add(element: T): Boolean {
        if (queue.size >= maxSize) queue.poll()
        return queue.add(element)
    }

    override fun addAll(elements: Collection<T>): Boolean {
        val totalElements = queue.size + elements.size
        if (totalElements > maxSize) repeat(totalElements - maxSize) { queue.poll() }
        return queue.addAll(elements)
    }

    override fun offer(element: T): Boolean {
        if (queue.size >= maxSize) queue.poll()
        return queue.offer(element)
    }

}
