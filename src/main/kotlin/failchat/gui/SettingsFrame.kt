package failchat.gui

import failchat.core.AppStateTransitionManager
import failchat.core.chat.InfoMessageMode
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
import org.apache.commons.configuration.CompositeConfiguration

class SettingsFrame(
        private val stage: Stage,
        private val appStateTransitionManager: AppStateTransitionManager,
        private val config: CompositeConfiguration,
        private val skinList: List<Skin>
) {

    lateinit var chat: ChatFrame

    //settings nodes
    private val peka2tvChannel: TextField
    private val goodgameChannel: TextField
    private val twitchChannel: TextField
    private val cybergameChannel: TextField
    private val peka2tvEnabled: CheckBox
    private val goodgameEnabled: CheckBox
    private val twitchEnabled: CheckBox
    private val cybergameEnabled: CheckBox
    private val skin: ChoiceBox<Skin>
    private val frame: CheckBox
    private val onTop: CheckBox
    private val showViewers: CheckBox
    private val showImages: CheckBox
    private val startButton: Button

    //second tab
    private val bgColorPicker: ColorPicker
    private val opacitySlider: Slider
    private val infoMessagesMode: ChoiceBox<String>
    private val ignoreList: TextArea

    init {
        stage.title = "failchat v" + config.getString("version")
        stage.icons.setAll(GuiLauncher.appIcon)
        val scene = Scene(FXMLLoader.load<Parent>(javaClass.getResource("/settings.fxml")))
        stage.scene = scene

        //channels
        peka2tvChannel = scene.lookup("#peka2tv_channel") as TextField
        goodgameChannel = scene.lookup("#goodgame_channel") as TextField
        twitchChannel = scene.lookup("#twitch_channel") as TextField
        cybergameChannel = scene.lookup("#cybergame_channel") as TextField

        //channels checkboxes
        peka2tvEnabled = scene.lookup("#peka2tv_enabled") as CheckBox
        goodgameEnabled = scene.lookup("#goodgame_enabled") as CheckBox
        twitchEnabled = scene.lookup("#twitch_enabled") as CheckBox
        cybergameEnabled = scene.lookup("#cybergame_enabled") as CheckBox

        skin = scene.lookup("#skin") as ChoiceBox<Skin>
        skin.converter = SkinConverter(skinList)
        skin.items = FXCollections.observableArrayList(skinList)
        frame = scene.lookup("#frame") as CheckBox
        onTop = scene.lookup("#top") as CheckBox
        showViewers = scene.lookup("#show_viewers") as CheckBox
        showImages = scene.lookup("#show_images") as CheckBox

        //second tab
        bgColorPicker = scene.lookup("#bgcolor") as ColorPicker
        infoMessagesMode = scene.lookup("#info_messages") as ChoiceBox<String>
        infoMessagesMode.setItems(FXCollections.observableList(InfoMessageMode.values().map { it.toString() }))
        ignoreList = scene.lookup("#ignore_list") as TextArea

        //opacity
        opacitySlider = scene.lookup("#opacity") as Slider
        val opacityText = scene.lookup("#opacity_text") as Text
        opacitySlider.valueProperty().addListener { observable, oldValue, newValue -> opacityText.text = Integer.toString(newValue.toInt()) }

        startButton = scene.lookup("#start_button") as Button
        startButton.setOnAction { action -> toChat() }

        stage.setOnCloseRequest { event ->
            saveSettingsValues()
            appStateTransitionManager.shutDown()
        }
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
        cybergameChannel.text = config.getString("cybergame.channel")

        peka2tvEnabled.isSelected = config.getBoolean("peka2tv.enabled")
        goodgameEnabled.isSelected = config.getBoolean("goodgame.enabled")
        twitchEnabled.isSelected = config.getBoolean("twitch.enabled")
        cybergameEnabled.isSelected = config.getBoolean("cybergame.enabled")

        skin.value = skin.converter.fromString(config.getString("skin"))
        frame.isSelected = config.getBoolean("frame")
        showViewers.isSelected = config.getBoolean("show-viewers")
        showImages.isSelected = config.getBoolean("show-images")
        onTop.isSelected = config.getBoolean("on-top")

        bgColorPicker.value = Color.web(config.getString("background-color"))
        opacitySlider.value = config.getDouble("opacity")
        infoMessagesMode.setValue(config.getString("info-message-mode"))

        val sb = StringBuilder()
        for (o in config.getList("ignore")) {
            sb.append(o).append("\n")
        }
        if (sb.length > 0) {
            sb.deleteCharAt(sb.length - 1)
        }
        ignoreList.text = sb.toString()
    }

    //todo optimize
    private fun saveSettingsValues() {
        config.setProperty("peka2tv.channel", peka2tvChannel.text)
        config.setProperty("goodgame.channel", goodgameChannel.text)
        config.setProperty("twitch.channel", twitchChannel.text)
        config.setProperty("cybergame.channel", cybergameChannel.text)

        config.setProperty("peka2tv.enabled", peka2tvEnabled.isSelected)
        config.setProperty("goodgame.enabled", goodgameEnabled.isSelected)
        config.setProperty("twitch.enabled", twitchEnabled.isSelected)
        config.setProperty("cybergame.enabled", cybergameEnabled.isSelected)

        config.setProperty("skin", skin.value.name)
        config.setProperty("frame", frame.isSelected)
        config.setProperty("on-top", onTop.isSelected)
        config.setProperty("show-viewers", showViewers.isSelected)
        config.setProperty("show-images", showImages.isSelected)

        config.setProperty("background-color", bgColorPicker.value.toString())
        config.setProperty("opacity", opacitySlider.value.toInt())
        config.setProperty("info-message-mode", infoMessagesMode.value.toString())
        config.setProperty("ignore", ignoreList.text.split("\n").dropLastWhile { it.isEmpty() }.toTypedArray())
    }
}
