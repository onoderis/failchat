package failchat.gui

import failchat.core.AppStateTransitionManager
import failchat.utils.removeTransparency
import failchat.utils.urlPattern
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
import org.apache.commons.configuration.CompositeConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.nio.file.Path
import java.nio.file.Paths

class ChatFrame(
        private val config: CompositeConfiguration,
        private val appStateTransitionManager: AppStateTransitionManager,
        private val guiEventHandler: GuiEventHandler,
        private val workingDirectory: Path
) {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ChatFrame::class.java)
    }

    lateinit var settings: SettingsFrame
    lateinit var app: Application

    private val skinsDirectory: Path = workingDirectory.resolve("skins") //todo load skin in another way

    private val decoratedChatStage: Stage
    private val undecoratedChatStage: Stage //for opaque background color
    private val transparentChatStage: Stage //for transparent background color
    private val chatScene: Scene
    private val webView: WebView = WebView()
    private val webEngine: WebEngine = webView.engine

    //context menu
    private val switchDecorationsItem: CheckMenuItem = CheckMenuItem("Show frame")
    private val onTopItem: CheckMenuItem = CheckMenuItem("On top")
    private val viewersItem: CheckMenuItem = CheckMenuItem("Show viewers")

    private var currentChatStage: Stage


    init {
        decoratedChatStage = buildChatStage(StageType.decorated)
        undecoratedChatStage = buildChatStage(StageType.undecorated)
        transparentChatStage = buildChatStage(StageType.transparent)
        currentChatStage = decoratedChatStage
        chatScene = buildChatScene()
        buildContextMenu(chatScene)
    }

    internal fun show() {
        val bgColor = Color.web(config.getString("background-color"))
        if (config.getBoolean("frame")) {
            currentChatStage = decoratedChatStage
            chatScene.fill = bgColor.removeTransparency()
        } else {
            if (bgColor.isOpaque) {
                currentChatStage = undecoratedChatStage
            } else {
                currentChatStage = transparentChatStage
            }
            chatScene.fill = bgColor
        }

        currentChatStage.scene = chatScene
        configureChatStage(currentChatStage)
        updateContextMenu()
        currentChatStage.show()
        val skin = config.getString("skin")
        try {
            webEngine.load(skinsDirectory.resolve(skin).resolve(skin + ".html").toUri().toURL().toString())
        } catch (e: MalformedURLException) {
            logger.error("Failed to load skin {}", skin, e)
        }

        Thread({
            //todo подумать как сделать лучше
            appStateTransitionManager.startChat()
        }, "ChatTransitionThread").start()
    }

    private fun buildChatStage(type: StageType): Stage {
        val stage = Stage()
        when (type) {
            StageType.decorated -> {
                stage.title = "failchat"
            }
            StageType.undecorated -> {
                stage.title = "failchat u"
                stage.initStyle(StageStyle.UNDECORATED)
            }
            StageType.transparent -> {
                stage.title = "failchat t"
                stage.initStyle(StageStyle.TRANSPARENT)
            }
        }
        stage.setOnCloseRequest { event ->
            saveChatPosition(stage)
            appStateTransitionManager.shutDown()
        }
        stage.icons.setAll(GuiLauncher.appIcon)
        return stage
    }

    private fun buildChatScene(): Scene {
        webEngine.userAgent = webEngine.userAgent + "/failchat"
        val chatScene = Scene(webView)
        webView.style = "-fx-background-color: transparent;"
        webView.isContextMenuEnabled = false

        //webengine transparent hack
        webEngine.documentProperty().addListener { observable, oldValue, newValue ->
            try {
                val f = webEngine.javaClass.getDeclaredField("page")
                f.isAccessible = true
                val page = f.get(webEngine) as com.sun.webkit.WebPage
                page.setBackgroundColor(0) //full transparent
            } catch (ignored: Exception) {
            }
        }

        //hot keys
        chatScene.setOnKeyReleased { key ->
            //esc
            if (key.code == KeyCode.ESCAPE) {
                toSettings()
            } else if (key.code == KeyCode.SPACE) {
                switchDecorations()
            }// space
        }

        // url opening interceptor
        webEngine.loadWorker.stateProperty().addListener { observable, oldValue, newValue ->
            // WebEngine.locationProperty не изменяется обратно после LoadWorker.cancel()
            // locationProperty заменяется сразу, как и Worker.State
            if (newValue == Worker.State.SCHEDULED) {
                val newLocation = webEngine.location
                val matcher = urlPattern.matcher(newLocation)
                if (matcher.find()) {
                    Platform.runLater { webEngine.loadWorker.cancel() }
                    app.hostServices.showDocument(webEngine.location)
                    logger.debug("Opening url: {}", webEngine.location)
                } else if (newLocation.contains("file:///")) {
                    //todo вспомнить зачем это надо
                    val newLocationPath = Paths.get(newLocation.split("file:///").get(1))
                    if (newLocationPath.startsWith(skinsDirectory)) {
                        logger.debug("Opening skin: {}", webEngine.location)
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
        switchDecorationsItem.setOnAction { event -> switchDecorations() }
        onTopItem.setOnAction { event ->
            val newValue = !config.getBoolean("on-top")
            config.setProperty("onTop", newValue)
            currentChatStage.isAlwaysOnTop = newValue
        }
        toSettingsItem.setOnAction { event -> toSettings() }
        viewersItem.setOnAction { event ->
            val newValue = !config.getBoolean("show-viewers")
            config.setProperty("show-viewers", newValue)
            guiEventHandler.fireViewersCountToggle()
        }

        return contextMenu
    }

    private fun toSettings() {
        saveChatPosition(currentChatStage)
        currentChatStage.hide()
        webEngine.loadContent("")
        settings.show()

        appStateTransitionManager.stopChat()
    }

    private fun switchDecorations() {
        val toDecorated = currentChatStage === undecoratedChatStage || currentChatStage === transparentChatStage
        val fromChatStage = currentChatStage
        val bgColor = Color.web(config.getString("background-color"))
        if (toDecorated) {
            currentChatStage = decoratedChatStage
            chatScene.fill = bgColor.removeTransparency()
            config.setProperty("frame", true)
        } else {
            //to undecorated
            if (bgColor.isOpaque) {
                currentChatStage = undecoratedChatStage
            } else {
                currentChatStage = transparentChatStage
            }//to transparent
            chatScene.fill = bgColor
            config.setProperty("frame", false)
        }

        fromChatStage.hide()
        saveChatPosition(fromChatStage)
        configureChatStage(currentChatStage)
        currentChatStage.scene = chatScene
        currentChatStage.show()
        logger.debug("Chat stage switched. Decorated: {}", toDecorated)
    }

    private fun configureChatStage(stage: Stage) {
        stage.opacity = config.getDouble("opacity") / 100
        stage.isAlwaysOnTop = config.getBoolean("on-top")
        stage.width = config.getDouble("chat.width")
        stage.height = config.getDouble("chat.height")
        val x = config.getDouble("chat.x")
        val y = config.getDouble("chat.y")
        if (x != -1.0 && y != -1.0) {
            stage.x = x
            stage.y = y
        }
    }

    private fun saveChatPosition(stage: Stage) {
        config.setProperty("chat.width", stage.width.toInt())
        config.setProperty("chat.height", stage.height.toInt())
        if (stage.x >= -10000 && stage.y >= -10000) { // -32k x -32k fix //todo check for error
            config.setProperty("chat.x", stage.x.toInt())
            config.setProperty("chat.y", stage.y.toInt())
        }
    }

    private fun updateContextMenu() {
        switchDecorationsItem.isSelected = config.getBoolean("frame")
        onTopItem.isSelected = config.getBoolean("onTop")
        viewersItem.isSelected = config.getBoolean("show-viewers")
    }

}
