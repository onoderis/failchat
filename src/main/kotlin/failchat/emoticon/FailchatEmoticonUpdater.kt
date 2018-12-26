package failchat.emoticon

import failchat.Origin.FAILCHAT
import failchat.util.executeWithCatch
import java.util.concurrent.ExecutorService

class FailchatEmoticonUpdater(
        private val storage: EmoticonStorage,
        private val scanner: FailchatEmoticonScanner,
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
