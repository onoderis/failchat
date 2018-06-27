package failchat.util

import mu.KotlinLogging
import java.time.Duration
import java.util.concurrent.ExecutorService
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.Condition

private val logger = KotlinLogging.logger {}

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
            logger.error("Uncaught exception during executing scheduled task $command", t)
        }
    }
}

inline var <T> AtomicReference<T>.value
    get(): T = this.get()
    set(value: T) = this.set(value)

fun ExecutorService.executeWithCatch(task: () -> Unit) {
    return execute {
        try {
            task.invoke()
        } catch (t: Throwable) {
            logger.error("Uncaught exception", t)
        }
    }
}

inline var AtomicBoolean.value
    get(): Boolean = this.get()
    set(value: Boolean) = this.set(value)
