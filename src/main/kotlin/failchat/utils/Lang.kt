package failchat.utils

inline fun <T> whileNotNull(supplier: () -> T, operation: (T) -> Unit) {
    var value = supplier.invoke()
    while (value != null) {
        operation.invoke(value)
        value = supplier.invoke()
    }
}
