package failchat.emoticon

import failchat.Origin
import java.util.concurrent.CompletableFuture

interface EmoticonBulkLoader<T : Emoticon> {
    val origin: Origin
    fun loadEmoticons(): CompletableFuture<List<T>>
}
