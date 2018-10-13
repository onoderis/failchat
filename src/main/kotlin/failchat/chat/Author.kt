package failchat.chat

import failchat.Origin

data class Author(
        /** Author's name. */
        val name: String,

        /** Author's origin. */
        val origin: Origin,

        /** Origin specific id. */
        val id: String = name
)
