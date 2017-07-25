package failchat.core.emoticon

import failchat.core.Origin
import java.util.concurrent.CompletableFuture

interface EmoticonLoader<T : Emoticon> {

    val origin: Origin

    fun loadEmoticons(): CompletableFuture<List<T>>

    fun getId(emoticon: T): Long

}
