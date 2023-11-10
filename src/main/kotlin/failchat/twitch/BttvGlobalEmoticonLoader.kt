package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonLoader
import java.util.concurrent.CompletableFuture

class BttvGlobalEmoticonLoader(private val bttvApiClient: BttvApiClient) : EmoticonLoader<BttvEmoticon> {

    override val origin = Origin.BTTV_GLOBAL

    override fun loadEmoticons(): CompletableFuture<List<BttvEmoticon>> {
        return bttvApiClient.loadGlobalEmoticons()
    }
}
