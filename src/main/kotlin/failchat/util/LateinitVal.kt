package failchat.util

/** Thread safe lateinit value. */
class LateinitVal<T : Any> {

    private var v: T? = null

    /**
     * Get the value.
     * @return null if value is is not initialized yet.
     * */
    fun get(): T? {
        if (v != null) return v

        // values is not initialized or not published
        synchronized(this) {
            return v
        }
    }

    /**
     * Set the value.
     * @throws [IllegalStateException] if value already initialized.
     * */
    fun set(value: T) {
        synchronized(this) {
            if (v != null) throw IllegalStateException("Value already initialized")
            v = value
        }
    }
}
