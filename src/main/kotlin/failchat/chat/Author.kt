package failchat.chat

data class Author(
        /** Author's name. */
        val name: String,

        /** Origin specific id. */
        val id: String = name
)
