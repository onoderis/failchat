package failchat.emoticon

import failchat.Origin.FAILCHAT
import failchat.util.executeWithCatch
import java.util.concurrent.ExecutorService

class CustomEmoticonUpdater(
        private val storage: EmoticonStorage,
        private val scanner: CustomEmoticonScanner,
        private val backgroundExecutor: ExecutorService
) {

    fun update() {
        backgroundExecutor.executeWithCatch {
            val emoticons = scanner.scan()
                    .map { EmoticonAndId(it, it.code) }

            storage.clear(FAILCHAT)
            storage.putMapping(FAILCHAT, emoticons)
        }
    }
}
