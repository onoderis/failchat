package failchat.emoticon

sealed class ReplaceDecision {
    object Skip : ReplaceDecision()
    class Replace(val replacement: String) : ReplaceDecision()
}
