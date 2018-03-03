package failchat.util

val hotspotThreads: Set<String> = setOf("Attach Listener", "Disposer", "Finalizer", "Prism Font Disposer",
        "Reference Handler", "Signal Dispatcher","DestroyJavaVM")

fun formatStackTraces(stackTraces: Map<Thread, Array<StackTraceElement>>): String {
    return stackTraces
            .map { (thread, stackTraceElements) ->
                with(thread) {
                    "Thread[name=$name; id=$id; state=$state; isDaemon=$isDaemon; isInterrupted=$isInterrupted; " +
                            "priority=$priority; threadGroup=${threadGroup.name}]"
                } +
                        if (stackTraceElements.isEmpty()) ""
                        else stackTraceElements.joinToString(prefix = ls + "\t", separator = ls + "\t")
            }
            .joinToString(separator = ls)
}
