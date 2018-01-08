package failchat.twitch

import failchat.Origin
import failchat.emoticon.Emoticon
import failchat.emoticon.EmoticonLoader
import java.util.concurrent.CompletableFuture

class BttvGlobalEmoticonLoader(private val bttvApiClient: BttvApiClient) : EmoticonLoader<Emoticon> {

    override val origin get() = Origin.BTTV_GLOBAL

    override fun loadEmoticons(): CompletableFuture<List<Emoticon>> {
        return bttvApiClient.loadGlobalEmoticons()
    }

    override fun getId(emoticon: Emoticon): Long {
        throw NotImplementedError()
    }

}
