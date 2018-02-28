package failchat

import failchat.util.sleep
import java.time.Duration

fun <T> doAwhile(tries: Int, retryDelay: Duration, operation: () -> T): T {
    lateinit var lastException: Throwable

    repeat(tries) {
        try {
            return operation.invoke()
        } catch (t: Throwable) {
            lastException = t
            sleep(retryDelay)
        }
    }

    throw lastException
}

fun Long.s(): Duration = Duration.ofSeconds(this)
fun Int.s(): Duration = Duration.ofSeconds(this.toLong())

fun Long.ms(): Duration = Duration.ofMillis(this)
fun Int.ms(): Duration = Duration.ofMillis(this.toLong())