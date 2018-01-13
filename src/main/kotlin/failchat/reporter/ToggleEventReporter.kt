package failchat.reporter

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ToggleEventReporter(
        private val delegate: EventReporter,
        private val enabled: Boolean
) : EventReporter {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ToggleEventReporter::class.java)
    }

    suspend override fun report(category: EventCategory, action: EventAction) {
        if (enabled) {
            delegate.report(category, action)
        } else {
            log.debug("Event reporter disabled, event {}.{} ignored", category, action)
        }
    }
}
