package failchat.util

import java.time.Duration
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition

inline fun sleep(duration: Duration) = Thread.sleep(duration.toMillis())

inline fun Condition.await(duration: Duration) = this.await(duration.toMillis(), TimeUnit.MILLISECONDS)

inline fun ScheduledExecutorService.schedule(delay: Duration, noinline command: () -> Unit): ScheduledFuture<*> {
    return this.schedule(command, delay.toMillis(), TimeUnit.MILLISECONDS)
}
