package failchat.reporter

import mu.KLogging

class ToggleEventReporter(
        private val delegate: EventReporter,
        private val enabled: Boolean
) : EventReporter {

    private companion object : KLogging()

    override suspend fun report(category: EventCategory, action: EventAction) {
        if (enabled) {
            delegate.report(category, action)
        } else {
            logger.debug("Event reporter disabled, event {}.{} ignored", category, action)
        }
    }
}
