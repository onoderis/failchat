package failchat.emoticon

import failchat.gui.GuiEventHandler
import failchat.resetEmoticonsUpdatedTime
import failchat.util.executeWithCatch
import mu.KLogging
import org.apache.commons.configuration2.Configuration
import java.util.concurrent.ExecutorService

class GlobalEmoticonUpdater(
        private val emoticonManager: EmoticonManager,
        private val emoticonLoadConfigurations: List<EmoticonLoadConfiguration<out Emoticon>>,
        private val backgroundExecutor: ExecutorService,
        private val guiEventHandler: GuiEventHandler,
        private val config: Configuration
) {

    private companion object : KLogging()

    fun reloadEmoticonsAsync() {
        config.resetEmoticonsUpdatedTime()
        actualizeEmoticonsAsync()
    }

    fun actualizeEmoticonsAsync() {
        backgroundExecutor.executeWithCatch {
            guiEventHandler.notifyEmoticonsAreLoading()

            emoticonManager.actualizeEmoticons(emoticonLoadConfigurations)

            guiEventHandler.notifyEmoticonsLoaded()
        }
    }

}
