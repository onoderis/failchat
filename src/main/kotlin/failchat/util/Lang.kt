package failchat.util

inline fun <T> whileNotNull(supplier: () -> T, operation: (T) -> Unit) {
    var value = supplier.invoke()
    while (value != null) {
        operation.invoke(value)
        value = supplier.invoke()
    }
}

fun Collection<*>?.isNullOrEmpty(): Boolean {
    if (this == null) return true
    if (this.isEmpty()) return true
    return false
}
