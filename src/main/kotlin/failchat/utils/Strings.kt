package failchat.utils

inline fun String.withPrefix(prefix: String): String {
    if (this.startsWith(prefix)) return this
    return prefix + this
}

inline fun String.withSuffix(suffix: String): String {
    if (this.endsWith(suffix)) return this
    return this + suffix
}

inline fun String?.notEmptyOrNull(): String? {
    if (this.isNullOrEmpty()) return null
    return this
}

fun formatStackTraces(stackTraces: Map<Thread, Array<StackTraceElement>>): String {
    return stackTraces
            .map { (thread, stackTraceElements) ->
                thread.run {
                    //kotlin's run function
                    "Thread[name=$name; id=$id; state=${state.name}; isDaemon=$isDaemon; isInterrupted=$isInterrupted;" +
                            "priority=$priority; threadGroup=${threadGroup.name}]"
                } + ls + stackTraceElements.joinToString(separator = ls)
            }
            .joinToString(separator = ls)
}
