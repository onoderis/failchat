package failchat.util

import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition

inline fun sleep(duration: Duration) = Thread.sleep(duration.toMillis())

inline fun Condition.await(duration: Duration) = this.await(duration.toMillis(), TimeUnit.MILLISECONDS)
