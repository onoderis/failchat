package failchat.util

fun String.withPrefix(prefix: String): String {
    if (this.startsWith(prefix)) return this
    return prefix + this
}

fun String.withSuffix(suffix: String): String {
    if (this.endsWith(suffix)) return this
    return this + suffix
}

fun String?.notEmptyOrNull(): String? {
    if (this.isNullOrEmpty()) return null
    return this
}

fun String.endsWithAny(suffixes: Iterable<String>): Boolean {
    return suffixes.any { suffix ->
        this.endsWith(suffix)
    }
}

fun Int.toHexString(): String = java.lang.Integer.toHexString(this)

fun toCodePoint(high: Char, low: Char): Int = java.lang.Character.toCodePoint(high, low)
