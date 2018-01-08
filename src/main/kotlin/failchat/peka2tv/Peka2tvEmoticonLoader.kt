package failchat.peka2tv

import failchat.Origin.PEKA2TV
import failchat.emoticon.EmoticonLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class Peka2tvEmoticonLoader(
        private val apiClient: Peka2tvApiClient
) : EmoticonLoader<Peka2tvEmoticon> {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(Peka2tvEmoticonLoader::class.java)
    }

    override val origin = PEKA2TV

    override fun loadEmoticons(): CompletableFuture<List<Peka2tvEmoticon>> {
        // https://github.com/peka2tv/api/blob/master/smile.md#Смайлы

        return apiClient
                .request("/smile") //todo move to Peka2tvApiClient
                .thenApply {
                    it.map { Peka2tvEmoticon(it.get("code").asText(), it.get("url").asText()) }
                }
    }

    override fun getId(emoticon: Peka2tvEmoticon): Long {
        throw UnsupportedOperationException()
    }
}
