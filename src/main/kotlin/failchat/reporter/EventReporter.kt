package failchat.reporter

interface EventReporter {
    suspend fun report(category: EventCategory, action: EventAction)
}
