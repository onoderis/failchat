package failchat.util

import javafx.scene.paint.Color

fun Color.toHexFormat(): String {
    val r = Math.round(red * 255.0).toInt()
    val g = Math.round(green * 255.0).toInt()
    val b = Math.round(blue * 255.0).toInt()
    val o = Math.round(opacity * 255.0).toInt()
    return String.format("#%02x%02x%02x%02x", r, g, b, o)
}
