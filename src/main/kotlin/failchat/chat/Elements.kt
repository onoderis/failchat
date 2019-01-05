package failchat.chat

object Elements {

    /** Get string representation of element by it's number. */
    fun label(number: Int) = "{!$number}"

    fun escapeBraces(text: String): String {
        return text
                .replace("<", "&lt;")
                .replace(">", "&gt;")
    }

    fun escapeLabelCharacters(text: String): String {
        return text
                .replace("{", "&#123;")
                .replace("}", "&#125;")
    }
}
