package failchat.gui

import failchat.core.chat.StatusMessageMode
import failchat.core.skin.Skin
import javafx.collections.FXCollections
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.control.ColorPicker
import javafx.scene.control.Slider
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.stage.Stage
import org.apache.commons.configuration2.Configuration

class SettingsFrame(
        private val stage: Stage,
        private val guiEventHandler: GuiEventHandler,
        private val config: Configuration,
        private val skinList: List<Skin>
) {

    lateinit var chat: ChatFrame

    private val scene = Scene(FXMLLoader.load<Parent>(javaClass.getResource("/fx/settings.fxml")))

    //channels
    private val peka2tvChannel = scene.lookup("#peka2tv_channel") as TextField
    private val goodgameChannel = scene.lookup("#goodgame_channel") as TextField
    private val twitchChannel = scene.lookup("#twitch_channel") as TextField
    private val youtubeChannel = scene.lookup("#youtube_channel") as TextField

    //channels checkboxes
    private val peka2tvEnabled = scene.lookup("#peka2tv_enabled") as CheckBox
    private val goodgameEnabled = scene.lookup("#goodgame_enabled") as CheckBox
    private val twitchEnabled = scene.lookup("#twitch_enabled") as CheckBox
    private val youtubeEnabled = scene.lookup("#youtube_enabled") as CheckBox

    private val skin = scene.lookup("#skin") as ChoiceBox<Skin>
    private val frame = scene.lookup("#frame") as CheckBox
    private val onTop = scene.lookup("#top") as CheckBox
    private val showViewers = scene.lookup("#show_viewers") as CheckBox
    private val showImages = scene.lookup("#show_images") as CheckBox

    //second tab
    private val bgColorPicker = scene.lookup("#bgcolor") as ColorPicker
    private val statusMessagesMode = scene.lookup("#status_messages") as ChoiceBox<StatusMessageMode>
    private val ignoreList = scene.lookup("#ignore_list") as TextArea

    //opacity
    private val opacitySlider = scene.lookup("#opacity") as Slider

    private val startButton = scene.lookup("#start_button") as Button


    private val statusMessagesModeConverter = StatusMessageModeConverter()


    init {
        stage.scene = scene
        stage.title = "failchat v" + config.getString("version")
        stage.icons.setAll(GuiLauncher.appIcon)

        peka2tvEnabled.selectedProperty().addListener { _, _, newValue ->
            peka2tvChannel.configureChannelField(newValue)
        }
        goodgameEnabled.selectedProperty().addListener { _, _, newValue ->
            goodgameChannel.configureChannelField(newValue)
        }
        twitchEnabled.selectedProperty().addListener { _, _, newValue ->
            twitchChannel.configureChannelField(newValue)
        }
        youtubeEnabled.selectedProperty().addListener { _, _, newValue ->
            youtubeChannel.configureChannelField(newValue)
        }

        skin.converter = SkinConverter(skinList)
        skin.items = FXCollections.observableArrayList(skinList)

        statusMessagesMode.converter = statusMessagesModeConverter
        statusMessagesMode.items = FXCollections.observableList(StatusMessageMode.values().toList())

        stage.setOnCloseRequest {
            saveSettingsValues()
            guiEventHandler.shutDown()
        }

        val opacityText = scene.lookup("#opacity_text") as Text
        opacitySlider.valueProperty().addListener { _, _, newValue ->
            opacityText.text = Integer.toString(newValue.toInt())
        }

        startButton.setOnAction { toChat() }
    }

    internal fun show() {
        updateSettingsValues()
        stage.show()
    }

    private fun toChat() {
        saveSettingsValues()
        stage.hide()
        chat.show()
    }

    private fun updateSettingsValues() {
        peka2tvChannel.text = config.getString("peka2tv.channel")
        goodgameChannel.text = config.getString("goodgame.channel")
        twitchChannel.text = config.getString("twitch.channel")
        youtubeChannel.text = config.getString("youtube.channel")

        config.getBoolean("peka2tv.enabled").let {
            peka2tvEnabled.isSelected = it
            peka2tvChannel.configureChannelField(it)
        }
        config.getBoolean("goodgame.enabled").let {
            goodgameEnabled.isSelected = it
            goodgameChannel.configureChannelField(it)
        }
        config.getBoolean("twitch.enabled").let {
            twitchEnabled.isSelected = it
            twitchChannel.configureChannelField(it)
        }
        config.getBoolean("youtube.enabled").let {
            youtubeEnabled.isSelected = it
            youtubeChannel.configureChannelField(it)
        }

        skin.value = skin.converter.fromString(config.getString("skin"))
        frame.isSelected = config.getBoolean("frame")
        showViewers.isSelected = config.getBoolean("show-viewers")
        showImages.isSelected = config.getBoolean("show-images")
        onTop.isSelected = config.getBoolean("on-top")

        bgColorPicker.value = Color.web(config.getString("background-color"))
        opacitySlider.value = config.getDouble("opacity")
        statusMessagesMode.value = statusMessagesModeConverter.fromString(config.getString("status-message-mode"))

        ignoreList.text = config.getList("ignore").joinToString(separator = "\n", postfix = "\n")
    }

    //todo optimize
    private fun saveSettingsValues() {
        config.setProperty("peka2tv.channel", peka2tvChannel.text)
        config.setProperty("goodgame.channel", goodgameChannel.text)
        config.setProperty("twitch.channel", twitchChannel.text)
        config.setProperty("youtube.channel", youtubeChannel.text)

        config.setProperty("peka2tv.enabled", peka2tvEnabled.isSelected)
        config.setProperty("goodgame.enabled", goodgameEnabled.isSelected)
        config.setProperty("twitch.enabled", twitchEnabled.isSelected)
        config.setProperty("youtube.enabled", youtubeEnabled.isSelected)

        config.setProperty("skin", skin.value.name)
        config.setProperty("frame", frame.isSelected)
        config.setProperty("on-top", onTop.isSelected)
        config.setProperty("show-viewers", showViewers.isSelected)
        config.setProperty("show-images", showImages.isSelected)

        config.setProperty("background-color", bgColorPicker.value.toString())
        config.setProperty("opacity", opacitySlider.value.toInt())
        config.setProperty("status-message-mode", statusMessagesModeConverter.toString(statusMessagesMode.value))
        config.setProperty("ignore", ignoreList.text.split("\n").dropLastWhile { it.isEmpty() }.toTypedArray())
    }

    private fun TextField.configureChannelField(editable: Boolean) {
        if (editable) {
            this.style = ""
            this.isEditable = true
        } else {
            this.style = "-fx-background-color: lightgrey"
            this.isEditable = false
        }
    }

}
