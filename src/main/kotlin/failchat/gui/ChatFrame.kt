package failchat.gui

import failchat.ConfigKeys
import failchat.FcServerInfo
import failchat.skin.Skin
import failchat.util.invertBoolean
import failchat.util.urlPattern
import javafx.application.Application
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.CheckMenuItem
import javafx.scene.control.ContextMenu
import javafx.scene.control.CustomMenuItem
import javafx.scene.control.MenuItem
import javafx.scene.control.SeparatorMenuItem
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyCombination
import javafx.scene.input.MouseButton
import javafx.scene.input.MouseEvent
import javafx.scene.layout.HBox
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import javafx.stage.Stage
import javafx.stage.StageStyle
import mu.KLogging
import netscape.javascript.JSObject
import org.apache.commons.configuration2.Configuration
import java.net.MalformedURLException

class ChatFrame(
        private val app: Application,
        private val config: Configuration,
        private val skins: List<Skin>,
        private val guiEventHandler: Lazy<GuiEventHandler>,
        private val ctConfigurator: ClickTransparencyConfigurator?
) {

    private companion object : KLogging()

    private val decoratedStage: Stage = buildChatStage(StageStyle.DECORATED)
    private val transparentStage: Stage = buildChatStage(StageStyle.TRANSPARENT)
    private val webView: WebView = WebView()
    private val webEngine: WebEngine = webView.engine
    private val chatScene: Scene = buildChatScene()

    // context menu
    private val switchDecorationsItem: CheckMenuItem = CheckMenuItem("Show frame")
    private val onTopItem: CheckMenuItem = CheckMenuItem("On top")
    private val clickTransparencyItem: CheckMenuItem = CheckMenuItem("Click through the window")
    private val viewersItem: CheckMenuItem = CheckMenuItem("Show viewers")
    private val zoomValueText = Text("???")
    private val zoomValues = listOf(25, 33, 50, 67, 75, 80, 90, 100, 110, 125, 150, 175, 200, 250, 300, 400, 500) //chrome-alike
    private val showHiddenMessages: CheckMenuItem = CheckMenuItem("Show hidden messages")

    // hot keys
    private val switchDecorationsKey = KeyCode.F
    private val onTopKey = KeyCode.O
    private val clickTransparencyKey = KeyCode.T
    private val viewersKey = KeyCode.V
    private val showHiddenMessagesKey = KeyCode.H
    private val clearChatKey = KeyCode.C
    private val closeChatKey = KeyCode.ESCAPE

    private var currentStage: Stage = decoratedStage
    private var lastOpenedSkinUrl: String? = null

    init {
        if (skins.isEmpty()) throw IllegalArgumentException("Empty skins")
        buildContextMenu()
    }

    fun show() {
        if (config.getBoolean(ConfigKeys.frame)) {
            currentStage = decoratedStage
            chatScene.fill = Color.BLACK
        } else {
            currentStage = transparentStage
            chatScene.fill = Color.TRANSPARENT
        }

        configureChatStage(currentStage)
        updateContextMenu()

        loadSkin()

        showChatStage(currentStage)
    }

    private fun loadSkin() {
        val skinName = config.getString(ConfigKeys.skin)
        try {
            val skin = skins.find { it.name == skinName } ?: skins.first()
            val optionalPortParam = if (FcServerInfo.port != FcServerInfo.defaultPort) {
                "?port=${FcServerInfo.port}"
            } else {
                ""
            }
            val url = "http://${FcServerInfo.host.hostAddress}:${FcServerInfo.port}/chat/${skin.name}" +
                    optionalPortParam

            lastOpenedSkinUrl = url
            webEngine.load(url)
        } catch (e: MalformedURLException) {
            logger.error("Failed to load skin '{}'", skinName, e)
        }
    }

    fun hide() {
        saveChatPosition(currentStage)
        hideChatStage(currentStage)
        clearWebContent()
    }

    fun clearWebContent() {
        webEngine.loadContent("")
    }

    private fun buildChatStage(style: StageStyle): Stage {
        val stage = Stage()
        stage.title = "failchat"
        stage.initStyle(style)

        stage.setOnCloseRequest {
            saveChatPosition(stage)
            guiEventHandler.value.handleShutDown()
        }
        stage.icons.setAll(Images.appIcon)

        return stage
    }

    private fun buildContextMenu() {
        if (ctConfigurator == null) {
            clickTransparencyItem.isDisable = true
        }

        // Build items
        fun Button.configureZoomButton(): Button = this.apply {
            minHeight = 20.0
            maxHeight = 20.0
            minWidth = 20.0
            maxWidth = 20.0
            padding = Insets.EMPTY
        }

        val minusButton = Button("-").configureZoomButton()
        val plusButton = Button("+").configureZoomButton()
        val zoomBox = HBox(Text("Zoom"), minusButton, zoomValueText, Text("%"), plusButton).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(0.0, 0.0, 0.0, 15.0)
        }
        val zoomItem = CustomMenuItem(zoomBox, false)

        val clearChatItem = MenuItem("Clear chat")
        val closeChatItem = MenuItem("Close chat")


        // Shortcuts
        switchDecorationsItem.accelerator = KeyCombination.valueOf(switchDecorationsKey.name)
        onTopItem.accelerator = KeyCombination.valueOf(onTopKey.name)
        clickTransparencyItem.accelerator = KeyCombination.valueOf(clickTransparencyKey.name)
        viewersItem.accelerator = KeyCombination.valueOf(viewersKey.name)
        clearChatItem.accelerator = KeyCombination.valueOf(clearChatKey.name)
        showHiddenMessages.accelerator = KeyCombination.valueOf(showHiddenMessagesKey.name)
        closeChatItem.accelerator = KeyCombination.valueOf(closeChatKey.name)

        // Build context menu
        val contextMenu = ContextMenu(
                switchDecorationsItem, onTopItem, clickTransparencyItem, viewersItem, zoomItem, SeparatorMenuItem(),
                clearChatItem, showHiddenMessages, SeparatorMenuItem(),
                closeChatItem
        )

        // Show/hide context menu
        chatScene.setOnMouseClicked { mouseEvent ->
            if (mouseEvent.button == MouseButton.SECONDARY) {
                contextMenu.show(chatScene.root, mouseEvent.screenX, mouseEvent.screenY)
            } else if (contextMenu.isShowing && mouseEvent.eventType == MouseEvent.MOUSE_CLICKED) {
                contextMenu.hide()
            }
        }

        // Menu items callbacks
        switchDecorationsItem.setOnAction { switchDecorations() }
        onTopItem.setOnAction { toggleOnTop() }
        clickTransparencyItem.setOnAction {
            ctConfigurator?.let { ctf ->
                toggleClickTransparency(ctf)
            }
        }
        viewersItem.setOnAction { toggleShowViewersBar() }
        clearChatItem.setOnAction { guiEventHandler.value.handleClearChat() }
        showHiddenMessages.setOnAction { toggleShowHiddenMessages() }
        closeChatItem.setOnAction { guiEventHandler.value.handleStopChat() }

        // Zoom item callbacks
        fun Button.configureZoomButtonCallback(elementNumberToGet: Int, filter: (Int, List<Int>) -> Boolean) = this.setOnAction {
            val oldValue = config.getInt(ConfigKeys.zoomPercent)
            val newValue = zoomValues.asSequence().windowed(2, 1)
                    .find { filter.invoke(oldValue, it) }
                    ?.get(elementNumberToGet)
                    ?: kotlin.run {
                        if (oldValue <= zoomValues.first()) zoomValues.first()
                        else zoomValues.last()
                    }
            config.setProperty(ConfigKeys.zoomPercent, newValue)
            guiEventHandler.value.handleConfigurationChange()
            zoomValueText.text = newValue.toString()
        }

        minusButton.configureZoomButtonCallback(0) { oldValue, range ->
            oldValue in (range[0] + 1)..range[1]
        }
        plusButton.configureZoomButtonCallback(1) { oldValue, range ->
            oldValue in range[0]..(range[1] - 1)
        }
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
                logger.warn("Exception during setting of the transparency hack", e)
            }
        }

        // hot keys
        chatScene.setOnKeyReleased { key ->
            when (key.code) {
                switchDecorationsKey -> switchDecorations()
                onTopKey -> onTopItem.isSelected = toggleOnTop()
                clickTransparencyKey -> {
                    ctConfigurator?.let {
                        clickTransparencyItem.isSelected = toggleClickTransparency(it)
                    }
                }
                viewersKey -> viewersItem.isSelected = toggleShowViewersBar()
                clearChatKey -> guiEventHandler.value.handleClearChat()
                showHiddenMessagesKey -> showHiddenMessages.isSelected = toggleShowHiddenMessages()
                closeChatKey -> guiEventHandler.value.handleStopChat()
                else -> {}
            }
        }

        // intercept url opening
        webEngine.loadWorker.stateProperty().addListener { _, _, newValue ->
            // WebEngine.locationProperty не изменяется обратно после LoadWorker.cancel()
            // locationProperty заменяется сразу, как и Worker.State
            if (newValue == Worker.State.SCHEDULED) {
                val location = webEngine.location
                val matcher = urlPattern.matcher(location)
                if (matcher.find() && location != lastOpenedSkinUrl) {
                    Platform.runLater { webEngine.loadWorker.cancel() }
                    logger.debug("Opening url in default browser: '{}'", location)
                    app.hostServices.showDocument(location)
                } else {
                    logger.debug("Opening url in web engine: '{}'", location)
                }
            }
        }
        return chatScene
    }

    private fun switchDecorations() {
        val fromStage = currentStage
        val toStage = if (fromStage === decoratedStage) {
            config.setProperty(ConfigKeys.frame, false)
            switchDecorationsItem.isSelected = false
            chatScene.fill = Color.TRANSPARENT
            transparentStage
        } else {
            config.setProperty(ConfigKeys.frame, true)
            switchDecorationsItem.isSelected = true
            chatScene.fill = Color.BLACK
            decoratedStage
        }

        saveChatPosition(fromStage)
        hideChatStage(fromStage)

        configureChatStage(toStage)
        showChatStage(toStage)
        currentStage = toStage

        logger.debug("Chat stage was switched from {} to {}", fromStage.style, toStage.style)
    }

    private fun configureChatStage(stage: Stage) {
        stage.opacity = config.getDouble(ConfigKeys.opacity) / 100
        stage.isAlwaysOnTop = config.getBoolean(ConfigKeys.onTop)
        if (config.getBoolean(ConfigKeys.clickTransparency)) {
            stage.isAlwaysOnTop = true
        }
        stage.width = config.getDouble("chat.width")
        stage.height = config.getDouble("chat.height")
        val x = config.getDouble("chat.x")
        val y = config.getDouble("chat.y")
        if (x != -1.0 && y != -1.0) { // magic numbers
            stage.x = x
            stage.y = y
        }
    }

    private fun hideChatStage(stage: Stage) {
        ctConfigurator?.removeClickTransparency(stage)
        stage.hide()
    }

    private fun showChatStage(stage: Stage) {
        stage.scene = chatScene
        stage.show()

        // handle can be accessed only after a window is shown for the first time
        ctConfigurator?.configureClickTransparency(stage)
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
        switchDecorationsItem.isSelected = config.getBoolean(ConfigKeys.frame)
        onTopItem.isSelected = config.getBoolean(ConfigKeys.onTop)
        viewersItem.isSelected = config.getBoolean(ConfigKeys.showViewers)
        zoomValueText.text = config.getString(ConfigKeys.zoomPercent)
        showHiddenMessages.isSelected = config.getBoolean(ConfigKeys.showHiddenMessages)
    }

    private fun toggleOnTop(): Boolean {
        val newValue = config.invertBoolean(ConfigKeys.onTop)
        currentStage.isAlwaysOnTop = newValue
        return newValue
    }

    private fun toggleClickTransparency(ctf: ClickTransparencyConfigurator): Boolean {
        val clickTransparencyEnabled = config.invertBoolean(ConfigKeys.clickTransparency)

        if (clickTransparencyEnabled) {
            // don't override configuration value for onTop option
            currentStage.isAlwaysOnTop = true
            onTopItem.isDisable = true

            ctf.configureClickTransparency(currentStage)
        } else {
            currentStage.isAlwaysOnTop = config.getBoolean(ConfigKeys.onTop)
            onTopItem.isDisable = false

            ctf.removeClickTransparency(currentStage)
        }

        return clickTransparencyEnabled
    }

    private fun toggleShowViewersBar(): Boolean {
        val newValue = config.invertBoolean(ConfigKeys.showViewers)
        guiEventHandler.value.handleConfigurationChange()
        return newValue

    }

    private fun toggleShowHiddenMessages(): Boolean {
        val newValue = config.invertBoolean(ConfigKeys.showHiddenMessages)
        guiEventHandler.value.handleConfigurationChange()
        return newValue
    }

}
