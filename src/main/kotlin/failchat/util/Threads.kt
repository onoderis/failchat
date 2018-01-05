package failchat.util

fun formatStackTraces(stackTraces: Map<Thread, Array<StackTraceElement>>): String {
    return stackTraces
            .map { (thread, stackTraceElements) ->
                with(thread) {
                    "Thread[name=$name; id=$id; state=${state.name}; isDaemon=$isDaemon; isInterrupted=$isInterrupted;" +
                            "priority=$priority; threadGroup=${threadGroup.name}]$ls"
                } +
                        if (stackTraceElements.isEmpty()) ""
                        else stackTraceElements.joinToString(prefix = "\t", separator = ls + '\t')
            }
            .joinToString(separator = ls)
}
