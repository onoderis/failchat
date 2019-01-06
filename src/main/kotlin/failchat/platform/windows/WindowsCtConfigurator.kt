package failchat.platform.windows

import com.sun.jna.platform.win32.WinDef.HWND
import failchat.ConfigKeys
import failchat.gui.ChatStage
import failchat.gui.ClickTransparencyConfigurator
import failchat.gui.StageType.TRANSPARENT
import mu.KLogging
import org.apache.commons.configuration2.Configuration

class WindowsCtConfigurator(private val config: Configuration) : ClickTransparencyConfigurator {

    private companion object : KLogging()

    override fun configureClickTransparency(chatStage: ChatStage) {
        if (!config.getBoolean(ConfigKeys.clickTransparency)) return

        val handle = getWindowHandle(chatStage) ?: return

        try {
            Windows.makeWindowClickTransparent(handle)
        } catch (t: Throwable) {
            logger.error("Failed to make clicks transparent for {} frame", chatStage.type, t)
        }
    }

    override fun removeClickTransparency(chatStage: ChatStage) {
        val handle = getWindowHandle(chatStage) ?: return

        try {
            val removeLayeredStyle = when (chatStage.type) {
                TRANSPARENT -> false
                else -> true
            }

            Windows.makeWindowClickOpaque(handle, removeLayeredStyle)
        } catch (t: Throwable) {
            logger.error("Failed to make clicks opaque for {} frame", chatStage.type, t)
        }
    }

    private fun getWindowHandle(chatStage: ChatStage): HWND? {
        return try {
            Windows.getWindowHandle(chatStage.stage)
        } catch (t: Throwable) {
            logger.error("Failed to get handle for {} window", chatStage.type, t)
            null
        }
    }
}
