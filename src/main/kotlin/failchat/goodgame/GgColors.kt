package failchat.goodgame

import javafx.scene.paint.Color

object GgColors {
    val defaultColor: Color = Color.web("#73adff")

    val byRole = mapOf(
            "bronze" to Color.web("#e7820a"),
            "silver" to Color.web("#b4b4b4"),
            "gold" to Color.web("#eefc08"),
            "diamond" to Color.web("#8781bd"),
            "king" to Color.web("#30d5c8"),
            "top-one" to Color.web("#3bcbff"),
            "premium" to Color.web("#bd70d7"),
            "premium-personal" to Color.web("#31a93a"),
            "moderator" to Color.web("#ec4058"),
            "streamer" to Color.web("#e8bb00"),
            "streamer-helper" to Color.web("#e8bb00")
    )
}
