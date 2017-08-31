package failchat.twitch

import failchat.core.Origin
import failchat.core.emoticon.Emoticon
import failchat.core.emoticon.EmoticonLoader
import java.util.concurrent.CompletableFuture

class BttvGlobalEmoticonLoader(private val bttvApiClient: BttvApiClient) : EmoticonLoader<Emoticon> {

    override val origin get() = Origin.bttvGlobal

    override fun loadEmoticons(): CompletableFuture<List<Emoticon>> {
        return bttvApiClient.loadGlobalEmoticons()
    }

    override fun getId(emoticon: Emoticon): Long {
        throw NotImplementedError()
    }

}
