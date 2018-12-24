package failchat.gui

import failchat.ConfigKeys
import failchat.emoticon.GlobalEmoticonUpdater
import failchat.skin.Skin
import failchat.util.toHexFormat
import javafx.application.Application
import javafx.collections.FXCollections
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType.WARNING
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE
import javafx.scene.control.ButtonBar.ButtonData.OK_DONE
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.ChoiceBox
import javafx.scene.control.ColorPicker
import javafx.scene.control.Hyperlink
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.Slider
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.paint.Color
import javafx.scene.text.Text
import javafx.stage.Stage
import mu.KLogging
import org.apache.commons.configuration2.Configuration
import java.nio.file.Path

class SettingsFrame(
        private val app: Application,
        private val stage: Stage,
        private val guiEventHandler: GuiEventHandler,
        private val config: Configuration,
        private val skinList: List<Skin>,
        private val customEmoticonsDirectory: Path,
        private val emoticonUpdater: GlobalEmoticonUpdater
) {

    private companion object : KLogging()

    private val scene = Scene(FXMLLoader.load<Parent>(javaClass.getResource("/fx/settings.fxml")))

    //channels
    private val peka2tvChannel = scene.lookup("#peka2tv_channel") as TextField
    private val goodgameChannel = scene.lookup("#goodgame_channel") as TextField
    private val twitchChannel = scene.lookup("#twitch_channel") as TextField
    private val youtubeChannel = scene.lookup("#youtube_channel") as TextField
    private val cybergameChannel = scene.lookup("#cybergame_channel") as TextField

    //channels checkboxes
    private val peka2tvEnabled = scene.lookup("#peka2tv_enabled") as CheckBox
    private val goodgameEnabled = scene.lookup("#goodgame_enabled") as CheckBox
    private val twitchEnabled = scene.lookup("#twitch_enabled") as CheckBox
    private val youtubeEnabled = scene.lookup("#youtube_enabled") as CheckBox
    private val cybergameEnabled = scene.lookup("#cybergame_enabled") as CheckBox

    @Suppress("UNCHECKED_CAST")
    private val skin = scene.lookup("#skin") as ChoiceBox<Skin>
    private val frame = scene.lookup("#frame") as CheckBox
    private val onTop = scene.lookup("#top") as CheckBox
    private val showViewers = scene.lookup("#show_viewers") as CheckBox
    private val showImages = scene.lookup("#show_images") as CheckBox

    // Additional settings tab
    // native client
    private val nativeBgColorPicker = scene.lookup("#bgcolor_native") as ColorPicker
    private val hideMessagesNative = scene.lookup("#hide_messages_native") as CheckBox
    private val hideMessagesNativeAfter = scene.lookup("#hide_messages_native_after") as TextField
    private val showStatusMessagesNative = scene.lookup("#show_status_messages_native") as CheckBox

    // external client
    private val externalBgColorPicker = scene.lookup("#bgcolor_external") as ColorPicker
    private val hideMessagesExternal = scene.lookup("#hide_messages_external") as CheckBox
    private val hideMessagesExternalAfter = scene.lookup("#hide_messages_external_after") as TextField
    private val showStatusMessagesExternal = scene.lookup("#show_status_messages_external") as CheckBox

    private val opacitySlider = scene.lookup("#opacity") as Slider
    private val showOriginBadges = scene.lookup("#show_origin_badges") as CheckBox
    private val showUserBadges = scene.lookup("#show_user_badges") as CheckBox
    private val zoomPercent = scene.lookup("#zoom_percent") as TextField


    // Actions tab
    private val customEmoticonsButton = scene.lookup("#custom_emoticons") as Button
    private val reloadEmoticonsButton = scene.lookup("#reload_emoticons_button") as Button
    private val reloadEmoticonsIndicator = scene.lookup("#reload_emoticons_indicator") as ProgressIndicator
    private val resetConfigurationButton = scene.lookup("#reset_configuration") as Button

    // Ignore list tab
    private val ignoreList = scene.lookup("#ignore_list") as TextArea


    private val startButton = scene.lookup("#start_button") as Button


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
        cybergameEnabled.selectedProperty().addListener { _, _, newValue ->
            cybergameChannel.configureChannelField(newValue)
        }

        skin.converter = SkinConverter(skinList)
        skin.items = FXCollections.observableArrayList(skinList)

        stage.setOnCloseRequest {
            saveSettingsValues()
            guiEventHandler.handleShutDown()
        }


        val opacityText = scene.lookup("#opacity_text") as Text
        opacitySlider.valueProperty().addListener { _, _, newValue ->
            opacityText.text = Integer.toString(newValue.toInt())
        }

        customEmoticonsButton.setOnAction {
            app.hostServices.showDocument(customEmoticonsDirectory.toUri().toString())
        }

        disableRefreshEmoticonsButton()
        reloadEmoticonsButton.setOnAction {
            emoticonUpdater.reloadEmoticonsAsync()
        }

        resetConfigurationButton.setOnAction {
            guiEventHandler.handleResetUserConfiguration()
        }

        val githubLink = scene.lookup("#github_link") as Hyperlink
        githubLink.setOnAction {
            app.hostServices.showDocument(config.getString("about.github-repo"))
        }

        val discordLink = scene.lookup("#discord_link") as Hyperlink
        discordLink.setOnAction {
            app.hostServices.showDocument(config.getString("about.discord-server"))
        }


        startButton.setOnAction { guiEventHandler.handleStartChat() }
    }

    fun show() {
        updateSettingsValues()
        stage.show()
    }

    fun hide() {
        saveSettingsValues()
        stage.hide()
    }

    fun enableRefreshEmoticonsButton() {
        reloadEmoticonsIndicator.isVisible = false
        reloadEmoticonsButton.isDisable = false
        reloadEmoticonsButton.text = "Reload emoticons"
    }

    fun disableRefreshEmoticonsButton() {
        reloadEmoticonsIndicator.isVisible = true
        reloadEmoticonsButton.isDisable = true
        reloadEmoticonsButton.text = "Loading emoticons"
    }

    /** @return true if user confirmed the reset. */
    fun confirmConfigReset(): Boolean {
        val notification = Alert(WARNING).apply {
            title = "Reset confirmation"
            headerText = "Are you sure you want to reset the configuration?"
        }
        val stage = notification.dialogPane.scene.window as Stage
        stage.icons.setAll(GuiLauncher.appIcon)

        val okButton = ButtonType("OK", OK_DONE)
        val closeButton = ButtonType("Cancel", CANCEL_CLOSE)
        notification.buttonTypes.setAll(okButton, closeButton)

        val result = notification.showAndWait().get()

        return result === okButton
    }

    fun disableResetConfigurationButton() {
        resetConfigurationButton.apply {
            isDisable = true
            text = "Restart the application"
            textFill = Color.ORANGERED
        }
    }

    fun updateSettingsValues() {
        peka2tvChannel.text = config.getString(ConfigKeys.peka2tv.channel)
        goodgameChannel.text = config.getString(ConfigKeys.goodgame.channel)
        twitchChannel.text = config.getString(ConfigKeys.twitch.channel)
        youtubeChannel.text = config.getString(ConfigKeys.youtube.channel)
        cybergameChannel.text = config.getString(ConfigKeys.cybergame.channel)

        config.getBoolean(ConfigKeys.peka2tv.enabled).let {
            peka2tvEnabled.isSelected = it
            peka2tvChannel.configureChannelField(it)
        }
        config.getBoolean(ConfigKeys.goodgame.enabled).let {
            goodgameEnabled.isSelected = it
            goodgameChannel.configureChannelField(it)
        }
        config.getBoolean(ConfigKeys.twitch.enabled).let {
            twitchEnabled.isSelected = it
            twitchChannel.configureChannelField(it)
        }
        config.getBoolean(ConfigKeys.youtube.enabled).let {
            youtubeEnabled.isSelected = it
            youtubeChannel.configureChannelField(it)
        }
        config.getBoolean(ConfigKeys.cybergame.enabled).let {
            cybergameEnabled.isSelected = it
            cybergameChannel.configureChannelField(it)
        }

        skin.value = skin.converter.fromString(config.getString(ConfigKeys.skin))
        frame.isSelected = config.getBoolean(ConfigKeys.frame)
        showViewers.isSelected = config.getBoolean(ConfigKeys.showViewers)
        showImages.isSelected = config.getBoolean(ConfigKeys.showImages)
        onTop.isSelected = config.getBoolean(ConfigKeys.onTop)
        showOriginBadges.isSelected = config.getBoolean(ConfigKeys.showOriginBadges)
        showUserBadges.isSelected = config.getBoolean(ConfigKeys.showUserBadges)
        zoomPercent.text = config.getInt(ConfigKeys.zoomPercent).toString()

        hideMessagesNative.isSelected = config.getBoolean(ConfigKeys.nativeClient.hideMessages)
        hideMessagesNativeAfter.text = config.getInt(ConfigKeys.nativeClient.hideMessagesAfter).toString()
        showStatusMessagesNative.isSelected = config.getBoolean(ConfigKeys.nativeClient.showStatusMessages)
        nativeBgColorPicker.value = Color.web(config.getString(ConfigKeys.nativeClient.backgroundColor))

        hideMessagesExternal.isSelected = config.getBoolean(ConfigKeys.externalClient.hideMessages)
        hideMessagesExternalAfter.text = config.getInt(ConfigKeys.externalClient.hideMessagesAfter).toString()
        showStatusMessagesExternal.isSelected = config.getBoolean(ConfigKeys.externalClient.showStatusMessages)
        externalBgColorPicker.value = Color.web(config.getString(ConfigKeys.externalClient.backgroundColor))

        opacitySlider.value = config.getDouble(ConfigKeys.opacity)

        val userIds = config.getStringArray(ConfigKeys.ignore)
        ignoreList.text = if (userIds.isEmpty()) {
            ""
        } else {
            userIds.joinToString(separator = "\n", postfix = "\n")
        }
    }

    private fun saveSettingsValues() {
        //todo use loop for origins
        config.setProperty(ConfigKeys.peka2tv.channel, peka2tvChannel.text)
        config.setProperty(ConfigKeys.goodgame.channel, goodgameChannel.text)
        config.setProperty(ConfigKeys.twitch.channel, twitchChannel.text)
        config.setProperty(ConfigKeys.youtube.channel, youtubeChannel.text)
        config.setProperty(ConfigKeys.cybergame.channel, cybergameChannel.text)

        config.setProperty(ConfigKeys.peka2tv.enabled, peka2tvEnabled.isSelected)
        config.setProperty(ConfigKeys.goodgame.enabled, goodgameEnabled.isSelected)
        config.setProperty(ConfigKeys.twitch.enabled, twitchEnabled.isSelected)
        config.setProperty(ConfigKeys.youtube.enabled, youtubeEnabled.isSelected)
        config.setProperty(ConfigKeys.cybergame.enabled, cybergameEnabled.isSelected)

        config.setProperty(ConfigKeys.skin, skin.value.name)
        config.setProperty(ConfigKeys.frame, frame.isSelected)
        config.setProperty(ConfigKeys.onTop, onTop.isSelected)
        config.setProperty(ConfigKeys.showViewers, showViewers.isSelected)
        config.setProperty(ConfigKeys.showImages, showImages.isSelected)

        config.setProperty(ConfigKeys.opacity, opacitySlider.value.toInt())
        config.setProperty(ConfigKeys.showOriginBadges, showOriginBadges.isSelected)
        config.setProperty(ConfigKeys.showUserBadges, showUserBadges.isSelected)
        config.setProperty(ConfigKeys.zoomPercent, parseZoomPercent(zoomPercent.text))

        config.setProperty(ConfigKeys.nativeClient.backgroundColor, nativeBgColorPicker.value.toHexFormat())
        config.setProperty(ConfigKeys.nativeClient.hideMessages, hideMessagesNative.isSelected)
        config.setProperty(ConfigKeys.nativeClient.hideMessagesAfter, parseHideMessagesAfter(hideMessagesNativeAfter.text))
        config.setProperty(ConfigKeys.nativeClient.showStatusMessages, showStatusMessagesNative.isSelected)

        config.setProperty(ConfigKeys.externalClient.backgroundColor, externalBgColorPicker.value.toHexFormat())
        config.setProperty(ConfigKeys.externalClient.hideMessages, hideMessagesExternal.isSelected)
        config.setProperty(ConfigKeys.externalClient.hideMessagesAfter, parseHideMessagesAfter(hideMessagesExternalAfter.text))
        config.setProperty(ConfigKeys.externalClient.showStatusMessages, showStatusMessagesExternal.isSelected)

        config.setProperty(ConfigKeys.ignore, ignoreList.text.split("\n").dropLastWhile { it.isEmpty() }.toTypedArray())
    }

    private fun parseZoomPercent(zoomPercent: String): Int {
        val percent = try {
            zoomPercent.toInt()
        } catch (e: Exception) {
            logger.warn("Failed to parse zoom percent as Int", e)
            return 100
        }

        if (percent !in 25..500) {
            logger.warn("Zoom percent '{}' not in range [25..500]", percent)
            return 100
        }

        return percent
    }
    
    private fun parseHideMessagesAfter(hideMessagesAfter: String): Int {
        val intValue = try {
            hideMessagesAfter.toInt()
        } catch (e: Exception) {
            logger.warn("Failed to parse 'hide messages after' as Int", e)
            return 60
        }

        if (intValue < 0) {
            logger.warn("'hide messages after' value  {} < 0", intValue)
            return 60
        }

        return intValue
    }

}
