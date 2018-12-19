package failchat.util

import ch.qos.logback.core.util.FileSize

fun Long.bytesToMegabytes() = this / FileSize.MB_COEFFICIENT
