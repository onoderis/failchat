package failchat.platform.windows

import com.sun.jna.platform.win32.WinDef.HWND
import failchat.ConfigKeys
import failchat.gui.ClickTransparencyConfigurator
import javafx.stage.Stage
import javafx.stage.StageStyle.DECORATED
import javafx.stage.StageStyle.TRANSPARENT
import mu.KotlinLogging
import org.apache.commons.configuration2.Configuration

class WindowsCtConfigurator(private val config: Configuration) : ClickTransparencyConfigurator {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    override fun configureClickTransparency(stage: Stage) {
        if (!config.getBoolean(ConfigKeys.clickTransparency)) return

        val handle = getWindowHandle(stage) ?: return

        try {
            Windows.makeWindowClickTransparent(handle)
        } catch (t: Throwable) {
            logger.error("Failed to make clicks transparent for {} frame", stage.style, t)
        }
    }

    override fun removeClickTransparency(stage: Stage) {
        val handle = getWindowHandle(stage) ?: return

        try {
            val removeLayeredStyle = when (stage.style) {
                DECORATED -> true
                TRANSPARENT -> false
                else -> throw IllegalArgumentException("StageStyle: ${stage.style}")
            }

            Windows.makeWindowClickOpaque(handle, removeLayeredStyle)
        } catch (t: Throwable) {
            logger.error("Failed to make clicks opaque for {} frame", stage.style, t)
        }
    }

    private fun getWindowHandle(stage: Stage): HWND? {
        return try {
            Windows.getWindowHandle(stage)
        } catch (t: Throwable) {
            logger.error("Failed to get handle for {} window", stage.style, t)
            null
        }
    }
}
