package failchat.chat

import failchat.Origin
import javafx.scene.paint.Color

data class Author(
        /** Author's name. */
        val name: String,

        /** Author's origin. */
        val origin: Origin,

        /** Origin specific id. */
        val id: String = name,

        /** Author's nickname color. */
        var color: Color? = null
)
