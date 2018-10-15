package failchat.util

import java.util.stream.Stream

fun <T : Any> Stream<T?>.filterNotNull(): Stream<T> {
    @Suppress("UNCHECKED_CAST")
    return filter { it != null } as Stream<T>
}
