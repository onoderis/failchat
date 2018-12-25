package failchat.chat

import failchat.Origin
import failchat.chat.OriginStatus.DISCONNECTED
import failchat.chatOrigins
import failchat.util.enumMap
import java.util.EnumMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class OriginStatusManager(
        private val messageSender: ChatMessageSender
) {

    private companion object {
        val allDisconnected: Map<Origin, OriginStatus> = enumMap<Origin, OriginStatus>().also { map ->
            chatOrigins.forEach { origin ->
                map[origin] = DISCONNECTED
            }
        }
    }

    private val statuses: EnumMap<Origin, OriginStatus> = enumMap()
    private val lock = ReentrantReadWriteLock()

    init {
        chatOrigins.forEach {
            statuses[it] = DISCONNECTED
        }
    }

    fun getStatuses(): Map<Origin, OriginStatus> {
        return lock.read {
            cloneStatuses()
        }
    }

    fun setStatus(origin: Origin, status: OriginStatus) {
        val afterMap = lock.write {
            statuses[origin] = status
            cloneStatuses()
        }
        messageSender.sendConnectedOriginsMessage(afterMap)
    }

    fun reset() {
        lock.write {
            statuses.entries.forEach {
                it.setValue(DISCONNECTED)
            }
        }
        messageSender.sendConnectedOriginsMessage(allDisconnected)
    }

    private fun cloneStatuses(): EnumMap<Origin, OriginStatus> {
        return EnumMap(statuses)
    }

}
