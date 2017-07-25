package failchat.peka2tv

import failchat.core.Origin.peka2tv
import failchat.core.emoticon.EmoticonLoader
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class Peka2tvEmoticonLoader(
        private val apiClient: Peka2tvApiClient
) : EmoticonLoader<Peka2tvEmoticon> {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(Peka2tvEmoticonLoader::class.java)
    }

    override val origin = peka2tv

    override fun loadEmoticons(): CompletableFuture<Map<out Any, Peka2tvEmoticon>> {
        // https://github.com/peka2tv/api/blob/master/smile.md#Смайлы

        return apiClient
                .request("/smile")
                .thenApply {
                    it.asSequence()
                            .map { Peka2tvEmoticon(it.get("code").asText().toLowerCase(), it.get("url").asText()) }
                            .map { it.code to it }
                            .toMap(HashMap())
                }
    }

}
