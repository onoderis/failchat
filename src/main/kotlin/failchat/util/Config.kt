package failchat.util

import org.apache.commons.configuration2.Configuration

/**
 * Invert boolean value by specified [key] and return the new value.
 * */
fun Configuration.invertBoolean(key: String): Boolean {
    val newValue = !getBoolean(key)
    setProperty(key, newValue)
    return newValue
}
