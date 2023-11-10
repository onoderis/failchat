package failchat.emoticon

import failchat.Origin
import java.util.concurrent.CompletableFuture

interface EmoticonLoader<T : Emoticon> {
    val origin: Origin
    fun loadEmoticons(): CompletableFuture<List<T>>
}
