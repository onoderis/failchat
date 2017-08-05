package failchat.util

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConcurrentEvictingQueueTest {

    private val queue = ConcurrentEvictingQueue<Any>(3)

    @Test
    fun evictTest() {
        repeat(4) {
            queue.add(Any())
        }

        assertEquals(3, queue.size)
    }

    @Test
    fun orderTest() {
        val elements = listOf(Any(), Any(), Any(), Any())
        elements.forEach { queue.add(it) }

        queue.forEachIndexed { i, element ->
            assertTrue(elements[i + 1] === element)
        }
    }

}
