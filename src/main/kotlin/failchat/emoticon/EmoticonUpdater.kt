package failchat.emoticon

import failchat.gui.GuiEventHandler
import failchat.resetEmoticonsUpdatedTime
import failchat.twitch.BttvEmoticonHandler
import failchat.util.executeWithCatch
import org.apache.commons.configuration2.Configuration
import java.util.concurrent.ExecutorService

class EmoticonUpdater(
        private val emoticonManager: EmoticonManager,
        private val emoticonLoadConfigurations: List<EmoticonLoadConfiguration<out Emoticon>>,
        private val bttvEmoticonHandler: BttvEmoticonHandler,
        private val backgroundExecutor: ExecutorService,
        private val guiEventHandler: GuiEventHandler,
        private val config: Configuration
) {

    fun reloadEmoticonsAsync() {
        config.resetEmoticonsUpdatedTime()
        actualizeEmoticonsAsync()
    }

    fun actualizeEmoticonsAsync() {
        backgroundExecutor.executeWithCatch {
            guiEventHandler.notifyEmoticonsAreLoading()

            emoticonManager.actualizeAllEmoticons(emoticonLoadConfigurations)
            bttvEmoticonHandler.compileGlobalEmoticonsPattern()

            guiEventHandler.notifyEmoticonsLoaded()
        }
    }
}
