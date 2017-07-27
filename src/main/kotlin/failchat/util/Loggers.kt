package failchat.util

import org.slf4j.Logger

//todo remove copypaste?
inline fun Logger.error(e: Throwable? = null, logMessageSupplier: () -> String) {
    if (!this.isErrorEnabled) return
    if (e == null) this.error(logMessageSupplier.invoke())
    else this.error(logMessageSupplier.invoke(), e)
}

inline fun Logger.warn(e: Throwable? = null, logMessageSupplier: () -> String) {
    if (!this.isWarnEnabled) return
    if (e == null) this.warn(logMessageSupplier.invoke())
    else this.warn(logMessageSupplier.invoke(), e)
}

inline fun Logger.info(e: Throwable? = null, logMessageSupplier: () -> String) {
    if (!this.isInfoEnabled) return
    if (e == null) this.info(logMessageSupplier.invoke())
    else this.info(logMessageSupplier.invoke(), e)
}

inline fun Logger.debug(e: Throwable? = null, logMessageSupplier: () -> String) {
    if (!this.isDebugEnabled) return
    if (e == null) this.debug(logMessageSupplier.invoke())
    else this.debug(logMessageSupplier.invoke(), e)
}
