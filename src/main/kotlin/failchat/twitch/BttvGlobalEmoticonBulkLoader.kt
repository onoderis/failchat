package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonBulkLoader
import java.util.concurrent.CompletableFuture

class BttvGlobalEmoticonBulkLoader(private val bttvApiClient: BttvApiClient) : EmoticonBulkLoader<BttvEmoticon> {

    override val origin = Origin.BTTV_GLOBAL

    override fun loadEmoticons(): CompletableFuture<List<BttvEmoticon>> {
        return bttvApiClient.loadGlobalEmoticons()
    }
}
