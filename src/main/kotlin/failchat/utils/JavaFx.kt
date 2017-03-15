package failchat.utils

import javafx.scene.paint.Color

fun Color.removeTransparency(): Color {
    if (this.isOpaque) return this
    else return Color.color(this.red, this.green, this.blue)
}
