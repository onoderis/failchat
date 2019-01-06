package failchat.experiment

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import javafx.application.Application
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.util.concurrent.Executors


fun main(args: Array<String>) {
    Application.launch(App::class.java)
}

private val user32 = User32.INSTANCE
private val executorService = Executors.newSingleThreadScheduledExecutor()

class App: Application() {
    override fun start(primaryStage: Stage) {
//        primaryStage.height = 300.0
//        primaryStage.width = 300.0
//        primaryStage.isAlwaysOnTop = true
//        primaryStage.show()

//        val hWnd = HWND(getWindowPointer(primaryStage))

//        makeWindowClickTransparent(hWnd)

//        executorService.schedule({
//            makeWindowClickNonTransparent(hWnd)
//            executorService.shutdown()
//        }, 5, SECONDS)

        val stages = buildStages()

        stages.forEach {
            it.show()
            val hWnd = HWND(getWindowPointer(it))
            val style = getWindowStyle(hWnd)
            println("style for ${it.style} stage: $style")

            it.isAlwaysOnTop = true
            val styleOnTop = getWindowStyle(hWnd)
            println("style with onTop option for ${it.style} stage: $styleOnTop")
        }

    }

    private fun buildStages(): List<Stage> {
        return listOf(StageStyle.DECORATED, StageStyle.UNDECORATED, StageStyle.TRANSPARENT)
                .map {
                    val stage = Stage(it)
                    stage.height = 300.0
                    stage.width = 300.0
                    stage.show()
                    stage
                }
    }
}


private fun getWindowStyle(hWnd: HWND): Int {
    return user32.GetWindowLong(hWnd, User32.GWL_EXSTYLE)
}

private fun makeWindowClickTransparent(hWnd: HWND) {
    val mask = user32.GetWindowLong(hWnd, User32.GWL_EXSTYLE)
    println("mask: $mask")

    val newMask = (mask or User32.WS_EX_LAYERED or User32.WS_EX_TRANSPARENT)
    println("new mask: $newMask")

    user32.SetWindowLong(hWnd, User32.GWL_EXSTYLE, newMask)
}

private fun makeWindowClickNonTransparent(hWnd: HWND) {
    val mask = user32.GetWindowLong(hWnd, User32.GWL_EXSTYLE)
    println("mask: $mask")

    val newMask = (mask and User32.WS_EX_LAYERED.inv() and User32.WS_EX_TRANSPARENT.inv())
    println("new mask: $newMask")

    user32.SetWindowLong(hWnd, User32.GWL_EXSTYLE, newMask)
}



private fun getWindowPointer(stage: Stage): Pointer? {
    try {
        @Suppress("DEPRECATION")
        val tkStage = stage.impl_getPeer()

        val getPlatformWindow = tkStage.javaClass.getDeclaredMethod("getPlatformWindow")
        getPlatformWindow.isAccessible = true
        val platformWindow = getPlatformWindow.invoke(tkStage)

        val getNativeHandle = platformWindow.javaClass.getMethod("getNativeHandle")
        getNativeHandle.isAccessible = true
        val nativeHandle = getNativeHandle.invoke(platformWindow)

        return Pointer(nativeHandle as Long)
    } catch (t: Throwable) {
        System.err.println("Error getting Window Pointer. $t")
        return null
    }

}
