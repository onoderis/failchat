package failchat.peka2tv

import failchat.Origin.PEKA2TV
import failchat.emoticon.EmoticonLoader
import java.util.concurrent.CompletableFuture

class Peka2TvEmoticonLoader(
        private val apiClient: Peka2tvApiClient
) : EmoticonLoader<Peka2tvEmoticon> {

    override val origin = PEKA2TV

    override fun loadEmoticons(): CompletableFuture<List<Peka2tvEmoticon>> {
        return apiClient.requestEmoticons()
    }
}
