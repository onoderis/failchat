package failchat.peka2tv

import failchat.Origin.PEKA2TV
import failchat.emoticon.EmoticonBulkLoader
import java.util.concurrent.CompletableFuture

class Peka2tvEmoticonBulkLoader(
        private val apiClient: Peka2tvApiClient
) : EmoticonBulkLoader<Peka2tvEmoticon> {

    override val origin = PEKA2TV

    override fun loadEmoticons(): CompletableFuture<List<Peka2tvEmoticon>> {
        return apiClient.requestEmoticons()
    }
}
