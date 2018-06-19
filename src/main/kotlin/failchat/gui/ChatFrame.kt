package failchat.gui

import failchat.util.urlPattern
import javafx.application.Application
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.scene.Scene
import javafx.scene.control.CheckMenuItem
import javafx.scene.control.ContextMenu
import javafx.scene.control.MenuItem
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.paint.Color
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import javafx.stage.Stage
import javafx.stage.StageStyle
import netscape.javascript.JSObject
import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.nio.file.Path
import java.nio.file.Paths

class ChatFrame(
        private val config: Configuration,
        private val guiEventHandler: GuiEventHandler,
        private val workingDirectory: Path
) {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ChatFrame::class.java)
    }

    lateinit var settings: SettingsFrame
    lateinit var app: Application

    private val skinsDirectory: Path = workingDirectory.resolve("skins") //todo load skin in another way

    private val decoratedChatStage: Stage = buildChatStage(StageType.DECORATED)
    private val undecoratedChatStage: Stage = buildChatStage(StageType.UNDECORATED) //for opaque background color
    private val transparentChatStage: Stage = buildChatStage(StageType.TRANSPARENT) //for transparent background color
    private val webView: WebView = WebView()
    private val webEngine: WebEngine = webView.engine
    private val chatScene: Scene = buildChatScene()

    //context menu
    private val switchDecorationsItem: CheckMenuItem = CheckMenuItem("Show frame")
    private val onTopItem: CheckMenuItem = CheckMenuItem("On top")
    private val viewersItem: CheckMenuItem = CheckMenuItem("Show viewers")

    private var currentChatStage: Stage = decoratedChatStage


    init {
        buildContextMenu(chatScene)
    }

    fun show() {
        val nativeBgColor = Color.web(config.getString("background-color.native"))

        if (config.getBoolean("frame")) {
            currentChatStage = decoratedChatStage
            chatScene.fill = Color.BLACK
        } else {
            if (nativeBgColor.isOpaque) {
                currentChatStage = undecoratedChatStage
                chatScene.fill = Color.BLACK
            } else {
                currentChatStage = transparentChatStage
                chatScene.fill = Color.TRANSPARENT
            }
        }

        currentChatStage.scene = chatScene
        configureChatStage(currentChatStage)
        updateContextMenu()

        val skin = config.getString("skin")
        try {
            webEngine.load(skinsDirectory.resolve(skin).resolve(skin + ".html").toUri().toURL().toString())
        } catch (e: MalformedURLException) {
            log.error("Failed to load skin {}", skin, e)
        }

        currentChatStage.show()

        guiEventHandler.startChat()
    }

    fun clearWebContent() {
        webEngine.loadContent("")
    }

    private fun buildChatStage(type: StageType): Stage {
        val stage = Stage()
        when (type) {
            StageType.DECORATED -> {
                stage.title = "failchat"
            }
            StageType.UNDECORATED -> {
                stage.title = "failchat u"
                stage.initStyle(StageStyle.UNDECORATED)
            }
            StageType.TRANSPARENT -> {
                stage.title = "failchat t"
                stage.initStyle(StageStyle.TRANSPARENT)
            }
        }
        stage.setOnCloseRequest {
            saveChatPosition(stage)
            guiEventHandler.shutDown()
        }
        stage.icons.setAll(GuiLauncher.appIcon)
        return stage
    }

    private fun buildChatScene(): Scene {
        webEngine.userAgent = webEngine.userAgent + "/failchat"
        val chatScene = Scene(webView)
        webView.style = "-fx-background-color: transparent;"
        webView.isContextMenuEnabled = false

        // logging
        webEngine.loadWorker.stateProperty().addListener { _, _, _ ->
            val window = webEngine.executeScript("window") as JSObject
            window.setMember("javaLogger", WebViewLogger)
            webEngine.executeScript("""
                console.log = function (message) {
                    javaLogger.log(message.toString())
                };
                console.error = function (message) {
                    javaLogger.error(message.toString())
                };
            """.trimIndent())
        }

        // web engine transparency hack
        webEngine.documentProperty().addListener { _, _, _ ->
            try {
                val f = webEngine.javaClass.getDeclaredField("page")
                f.isAccessible = true
                val page = f.get(webEngine) as com.sun.webkit.WebPage
                page.setBackgroundColor(0) //fully transparent
            } catch (e: Exception) {
                log.debug("Exception during setting of the transparency hack", e)
            }
        }

        // hot keys
        chatScene.setOnKeyReleased { key ->
            when (key.code) {
                KeyCode.ESCAPE -> toSettings()
                KeyCode.SPACE -> switchDecorations()
                else -> {}
            }
        }

        // url opening interceptor
        webEngine.loadWorker.stateProperty().addListener { _, _, newValue ->
            // WebEngine.locationProperty не изменяется обратно после LoadWorker.cancel()
            // locationProperty заменяется сразу, как и Worker.State
            if (newValue == Worker.State.SCHEDULED) {
                val newLocation = webEngine.location
                val matcher = urlPattern.matcher(newLocation)
                if (matcher.find()) {
                    Platform.runLater { webEngine.loadWorker.cancel() }
                    app.hostServices.showDocument(webEngine.location)
                    log.debug("Opening url: {}", webEngine.location)
                } else if (newLocation.contains("file:///")) {
                    val newLocationPath = Paths.get(newLocation.split("file:///").get(1))
                    if (newLocationPath.startsWith(skinsDirectory)) {
                        log.debug("Opening skin: {}", webEngine.location)
                    }
                }
            }
        }
        return chatScene
    }

    private fun buildContextMenu(scene: Scene): ContextMenu {
        val toSettingsItem = MenuItem("To settings")
        val contextMenu = ContextMenu(switchDecorationsItem, onTopItem, viewersItem, toSettingsItem)

        //context menu
        scene.setOnMouseClicked { mouseEvent ->
            if (mouseEvent.button == MouseButton.SECONDARY) {
                contextMenu.show(scene.root, mouseEvent.screenX, mouseEvent.screenY)
            } else if (contextMenu.isShowing && mouseEvent.eventType == MouseEvent.MOUSE_CLICKED) {
                contextMenu.hide()
            }
        }

        //menu items
        switchDecorationsItem.setOnAction { switchDecorations() }
        onTopItem.setOnAction {
            val newValue = !config.getBoolean("on-top")
            config.setProperty("on-top", newValue)
            currentChatStage.isAlwaysOnTop = newValue
        }
        toSettingsItem.setOnAction { toSettings() }
        viewersItem.setOnAction {
            val newValue = !config.getBoolean("show-viewers")
            config.setProperty("show-viewers", newValue)
            guiEventHandler.notifyViewersCountToggled()
        }

        return contextMenu
    }

    private fun toSettings() {
        saveChatPosition(currentChatStage)
        currentChatStage.hide()
        clearWebContent()
        settings.show()
        guiEventHandler.stopChat()
    }

    private fun switchDecorations() {
        val toDecorated = currentChatStage === undecoratedChatStage || currentChatStage === transparentChatStage
        val fromChatStage = currentChatStage

        val bgColor = Color.web(config.getString("background-color.native"))
        val toChatStage = if (toDecorated) {
            config.setProperty("frame", true)
            chatScene.fill = Color.BLACK
            decoratedChatStage
        } else {
            config.setProperty("frame", false)
            if (bgColor.isOpaque) {
                chatScene.fill = Color.BLACK
                undecoratedChatStage
            } else {
                chatScene.fill = Color.TRANSPARENT
                transparentChatStage
            }
        }

        saveChatPosition(fromChatStage)
        configureChatStage(toChatStage)
        fromChatStage.hide()
        toChatStage.scene = chatScene
        toChatStage.show()
        currentChatStage = toChatStage
        log.debug("Chat stage switched. Decorated: {}", toDecorated)
    }

    private fun configureChatStage(stage: Stage) {
        stage.opacity = config.getDouble("opacity") / 100
        stage.isAlwaysOnTop = config.getBoolean("on-top")
        stage.width = config.getDouble("chat.width")
        stage.height = config.getDouble("chat.height")
        val x = config.getDouble("chat.x")
        val y = config.getDouble("chat.y")
        if (x != -1.0 && y != -1.0) { // magic numbers
            stage.x = x
            stage.y = y
        }
    }

    private fun saveChatPosition(stage: Stage) {
        config.setProperty("chat.width", stage.width.toInt())
        config.setProperty("chat.height", stage.height.toInt())

        // Иногда по какой-то причине окно передвигается на позицию -32k x -32k
        if (stage.x >= -10000 && stage.y >= -10000) {
            config.setProperty("chat.x", stage.x.toInt())
            config.setProperty("chat.y", stage.y.toInt())
        }
    }

    private fun updateContextMenu() {
        switchDecorationsItem.isSelected = config.getBoolean("frame")
        onTopItem.isSelected = config.getBoolean("on-top")
        viewersItem.isSelected = config.getBoolean("show-viewers")
    }

}
