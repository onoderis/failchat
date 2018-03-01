package failchat.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Condition

private val log: Logger = LoggerFactory.getLogger("failchat.util.ConcurrentKt")

fun sleep(duration: Duration) = Thread.sleep(duration.toMillis())

fun Condition.await(duration: Duration) = this.await(duration.toMillis(), TimeUnit.MILLISECONDS)

fun ScheduledExecutorService.schedule(delay: Duration, command: () -> Unit): ScheduledFuture<*> {
    return this.schedule(command, delay.toMillis(), TimeUnit.MILLISECONDS)
}

fun ScheduledExecutorService.scheduleWithCatch(delay: Duration, command: () -> Unit): ScheduledFuture<*> {
    return schedule(delay) {
        try {
            command.invoke()
        } catch (t: Throwable) {
            log.warn("Uncaught exception during executing scheduled task $command", t)
        }
    }
}

inline var <T> AtomicReference<T>.value
    get(): T = this.get()
    set(value: T) = this.set(value)

fun ExecutorService.submitWithCatch(task: () -> Unit): Future<*> {
    return submit {
        try {
            task.invoke()
        } catch (t: Throwable) {
            log.error("Uncaught exception", t)
        }
    }
}
