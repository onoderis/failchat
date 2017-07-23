package failchat.utils

inline fun String.withPrefix(prefix: String): String {
    if (this.startsWith(prefix)) return this
    return prefix + this
}

inline fun String.withSuffix(suffix: String): String {
    if (this.endsWith(suffix)) return this
    return this + suffix
}

inline fun String?.notEmptyOrNull(): String? {
    if (this.isNullOrEmpty()) return null
    return this
}
