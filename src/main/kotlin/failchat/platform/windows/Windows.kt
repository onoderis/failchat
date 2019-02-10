package failchat.platform.windows

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import failchat.exception.NativeCallException
import failchat.util.binary
import javafx.stage.Stage
import mu.KotlinLogging

object Windows {

    private val logger = KotlinLogging.logger {}

    /** @throws [NativeCallException] */
    fun makeWindowClickTransparent(windowHandle: HWND) {
        changeWindowStyle(windowHandle) { currentStyle ->
            currentStyle or User32.WS_EX_LAYERED or User32.WS_EX_TRANSPARENT
        }
    }

    /** @throws [NativeCallException] */
    fun makeWindowClickOpaque(windowHandle: HWND, removeLayeredStyle: Boolean) {
        changeWindowStyle(windowHandle) { currentStyle ->
            val notTransparentStyle = currentStyle and User32.WS_EX_TRANSPARENT.inv()
            if (removeLayeredStyle) {
                notTransparentStyle and User32.WS_EX_LAYERED.inv()
            } else {
                notTransparentStyle
            }
        }
    }

    /** @throws [NativeCallException] */
    private fun changeWindowStyle(windowHandle: HWND, changeOperation: (Int) -> Int) {
        val currentStyle = User32.INSTANCE.GetWindowLong(windowHandle, User32.GWL_EXSTYLE)
                .ifError { errorCode ->
                    throw NativeCallException("Failed to get window style, error code: $errorCode, handle: $windowHandle")
                }

        val newStyle = changeOperation.invoke(currentStyle)
        logger.debug { "Changing window style, current: ${currentStyle.binary()}, new: ${newStyle.binary()}; window: $windowHandle" }

        User32.INSTANCE.SetWindowLong(windowHandle, User32.GWL_EXSTYLE, newStyle)
                .ifError { errorCode ->
                    throw NativeCallException("Failed to set window style, error code: $errorCode, handle: $windowHandle")
                }
    }

    private inline fun Int.ifError(operation: (errorCode: Int) -> Nothing): Int {
        if (this != 0) return this

        val errorCode = Native.getLastError()
        operation(errorCode)
    }


    fun getWindowHandle(stage: Stage): HWND {
        @Suppress("DEPRECATION")
        val tkStage = stage.impl_getPeer()

        val getPlatformWindow = tkStage.javaClass.getDeclaredMethod("getPlatformWindow")
        getPlatformWindow.isAccessible = true
        val platformWindow = getPlatformWindow.invoke(tkStage)

        val getNativeHandle = platformWindow.javaClass.getMethod("getNativeHandle")
        getNativeHandle.isAccessible = true
        val nativeHandle = getNativeHandle.invoke(platformWindow)

        return HWND(Pointer(nativeHandle as Long))
    }
}
