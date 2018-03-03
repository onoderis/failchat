package failchat.util

/**
 * Line separator shortcut.
 * */
inline val ls: String get() = System.lineSeparator()

/**
 * Shortcut for [System.getProperty].
 * */
fun sp(key: String): String? = System.getProperty(key)
