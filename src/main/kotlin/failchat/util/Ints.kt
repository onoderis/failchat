package failchat.util

fun Int.binary(): String {
    val binaryInt = Integer.toBinaryString(this)
    if (binaryInt.length >= 32) return binaryInt

    val sb = StringBuilder(32)
    repeat(32 - binaryInt.length) {
        sb.append('0')
    }
    sb.append(binaryInt)

    return sb.toString()
}
