package failchat.emoticon

import failchat.Origin.FAILCHAT

class FailchatEmoticonUpdater(
        private val storage: EmoticonStorage,
        private val scanner: FailchatEmoticonScanner
) {

    fun update() {
        val emoticons = scanner.scan()
                .map { EmoticonAndId(it, it.code) }

        storage.clear(FAILCHAT)
        storage.putMapping(FAILCHAT, emoticons)
    }
}
