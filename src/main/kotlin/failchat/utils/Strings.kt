package failchat.utils

fun String.withPrefix(prefix: String): String {
    if (this.startsWith(prefix)) return this
    return prefix + this
}

fun String.withSuffix(suffix: String): String {
    if (this.endsWith(suffix)) return this
    return this + suffix
}
