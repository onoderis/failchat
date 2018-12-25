package failchat.chat

import com.google.common.collect.EvictingQueue
import failchat.chat.ChatMessageHistory.Operation.Add
import failchat.chat.ChatMessageHistory.Operation.Clear
import failchat.chat.ChatMessageHistory.Operation.FindAll
import failchat.chat.ChatMessageHistory.Operation.FindFirst
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.Queue

class ChatMessageHistory(capacity: Int) {

    private val history: Queue<ChatMessage> = EvictingQueue.create(capacity)
    private val opChannel: Channel<Operation> = Channel(Channel.UNLIMITED)

    fun start() {
        CoroutineScope(Dispatchers.Default + CoroutineName("ChatMessageHistoryHandler")).launch {
            handleOperations()
        }
    }

    private suspend fun handleOperations() {
        for (op in opChannel) {
            when (op) {
                is Add -> history.add(op.message)
                is FindFirst -> {
                    val message = history.find(op.predicate)
                    op.result.complete(message)
                }
                is FindAll -> {
                    val messages = history.filter(op.predicate)
                    op.result.complete(messages)
                }
                is Clear -> history.clear()
            }
        }
    }

    fun stop() {
        opChannel.close()
    }

    suspend fun add(message: ChatMessage) {
        opChannel.send(Add(message))
    }

    suspend fun findFirst(predicate: (ChatMessage) -> Boolean): ChatMessage? {
        val foundFuture = CompletableDeferred<ChatMessage?>()
        opChannel.send(FindFirst(predicate, foundFuture))
        return foundFuture.await()
    }

    suspend fun find(predicate: (ChatMessage) -> Boolean): List<ChatMessage> {
        val foundFuture = CompletableDeferred<List<ChatMessage>>()
        opChannel.send(FindAll(predicate, foundFuture))
        return foundFuture.await()
    }

    suspend fun clear() {
        opChannel.send(Clear)
    }

    private sealed class Operation {
        class Add(val message: ChatMessage) : Operation()
        class FindFirst(val predicate: (ChatMessage) -> Boolean, val result: CompletableDeferred<ChatMessage?>) : Operation()
        class FindAll(val predicate: (ChatMessage) -> Boolean, val result: CompletableDeferred<List<ChatMessage>>) : Operation()
        object Clear : Operation()
    }

}

suspend inline fun <reified T : ChatMessage> ChatMessageHistory.findFirstTyped(crossinline predicate: (T) -> Boolean): Deferred<T?> {
    @Suppress("UNCHECKED_CAST")
    return findFirst { it is T && predicate(it) }
            as Deferred<T?>
}

suspend inline fun <reified T : ChatMessage> ChatMessageHistory.findTyped(crossinline predicate: (T) -> Boolean): Deferred<List<T>> {
    @Suppress("UNCHECKED_CAST")
    return find { it is T && predicate(it) }
            as Deferred<List<T>>
}
