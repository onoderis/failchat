package failchat.gui

import failchat.ConfigKeys
import failchat.emoticon.GlobalEmoticonUpdater
import failchat.skin.Skin
import failchat.util.toHexFormat
import javafx.application.Application
import javafx.collections.FXCollections
import javafx.fxml.FXMLLoader
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
import mu.KotlinLogging
import org.apache.commons.configuration2.Configuration
import java.nio.file.Path

class SettingsFrame(
    private val app: Application, //todo replace with LinkOpener
    private val stage: Stage,
    private val config: Configuration,
    private val skinList: List<Skin>,
    private val failchatEmoticonsDirectory: Path,
    private val clickTransparencyEnabled: Boolean,
    private val guiEventHandler: Lazy<GuiEventHandler>,
    private val emoticonUpdater: Lazy<GlobalEmoticonUpdater>
) {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    private val loader = FXMLLoader(javaClass.getResource("/fx/settings.fxml"))
    private val scene = Scene(loader.load())
    private val namespace = loader.namespace

    //channels
    private val goodgameChannel = namespace["goodgame_channel"] as TextField
    private val twitchChannel = namespace["twitch_channel"] as TextField
    private val youtubeChannel = namespace["youtube_channel"] as TextField

    //channels checkboxes
    private val goodgameEnabled = namespace["goodgame_enabled"] as CheckBox
    private val twitchEnabled = namespace["twitch_enabled"] as CheckBox
    private val youtubeEnabled = namespace["youtube_enabled"] as CheckBox

    @Suppress("UNCHECKED_CAST")
    private val skin = namespace["skin"] as ChoiceBox<Skin>
    private val frame = namespace["frame"] as CheckBox
    private val onTop = namespace["top"] as CheckBox
    private val clickTransparency = namespace["click_transparency"] as CheckBox
    private val showViewers = namespace["show_viewers"] as CheckBox
    private val showImages = namespace["show_images"] as CheckBox

    // Additional settings tab
    // common settings
    private val opacitySlider = namespace["opacity"] as Slider
    private val showOriginBadges = namespace["show_origin_badges"] as CheckBox
    private val showUserBadges = namespace["show_user_badges"] as CheckBox
    private val hideDeletedMessages = namespace["hide_deleted_messages"] as CheckBox
    private val zoomPercent = namespace["zoom_percent"] as TextField
    private val deletedMessagePlaceholder = namespace["deleted_message_placeholder"] as TextField
    private val showClickTransparencyIcon = namespace["show_click_transparency_icon"] as CheckBox
    private val saveMessageHistory = namespace["save_message_history"] as CheckBox

    // native client
    private val nativeBgColorPicker = namespace["bgcolor_native"] as ColorPicker
    private val coloredNicknamesNative = namespace["colored_nicknames_native"] as CheckBox
    private val hideMessagesNative = namespace["hide_messages_native"] as CheckBox
    private val hideMessagesNativeAfter = namespace["hide_messages_native_after"] as TextField
    private val showStatusMessagesNative = namespace["show_status_messages_native"] as CheckBox

    // external client
    private val externalBgColorPicker = namespace["bgcolor_external"] as ColorPicker
    private val coloredNicknamesExternal = namespace["colored_nicknames_external"] as CheckBox
    private val hideMessagesExternal = namespace["hide_messages_external"] as CheckBox
    private val hideMessagesExternalAfter = namespace["hide_messages_external_after"] as TextField
    private val showStatusMessagesExternal = namespace["show_status_messages_external"] as CheckBox


    // Actions tab
    private val failchatEmoticonsButton = namespace["failchat_emoticons"] as Button
    private val reloadEmoticonsButton = namespace["reload_emoticons_button"] as Button
    private val reloadEmoticonsIndicator = namespace["reload_emoticons_indicator"] as ProgressIndicator
    private val resetConfigurationButton = namespace["reset_configuration"] as Button

    // Ignore list tab
    private val ignoreList = namespace["ignore_list"] as TextArea


    private val startButton = namespace["start_button"] as Button


    init {
        stage.scene = scene
        stage.title = "failchat v" + config.getString("version")
        stage.icons.setAll(Images.appIcon)

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

        clickTransparency.isDisable = !clickTransparencyEnabled
        showClickTransparencyIcon.isDisable = !clickTransparencyEnabled

        stage.setOnCloseRequest {
            saveSettingsValues()
            guiEventHandler.value.handleShutDown()
        }


        val opacityText = namespace["opacity_text"] as Text
        opacitySlider.valueProperty().addListener { _, _, newValue ->
            opacityText.text = Integer.toString(newValue.toInt())
        }

        failchatEmoticonsButton.setOnAction {
            app.hostServices.showDocument(failchatEmoticonsDirectory.toUri().toString())
        }

        disableRefreshEmoticonsButton()
        reloadEmoticonsButton.setOnAction {
            emoticonUpdater.value.reloadEmoticonsAsync()
        }

        resetConfigurationButton.setOnAction {
            guiEventHandler.value.handleResetUserConfiguration()
        }

        val githubLink = namespace["github_link"] as Hyperlink
        githubLink.setOnAction {
            app.hostServices.showDocument(config.getString("about.github-repo"))
        }

        val discordLink = namespace["discord_link"] as Hyperlink
        discordLink.setOnAction {
            app.hostServices.showDocument(config.getString("about.discord-server"))
        }


        startButton.setOnAction { guiEventHandler.value.handleStartChat() }
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
        stage.icons.setAll(Images.appIcon)

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
        goodgameChannel.text = config.getString(ConfigKeys.Goodgame.channel)
        twitchChannel.text = config.getString(ConfigKeys.Twitch.channel)
        youtubeChannel.text = config.getString(ConfigKeys.Youtube.channel)

        config.getBoolean(ConfigKeys.Goodgame.enabled).let {
            goodgameEnabled.isSelected = it
            goodgameChannel.configureChannelField(it)
        }
        config.getBoolean(ConfigKeys.Twitch.enabled).let {
            twitchEnabled.isSelected = it
            twitchChannel.configureChannelField(it)
        }
        config.getBoolean(ConfigKeys.Youtube.enabled).let {
            youtubeEnabled.isSelected = it
            youtubeChannel.configureChannelField(it)
        }

        skin.value = skin.converter.fromString(config.getString(ConfigKeys.skin))
        frame.isSelected = config.getBoolean(ConfigKeys.frame)
        onTop.isSelected = config.getBoolean(ConfigKeys.onTop)
        if (clickTransparencyEnabled) {
            clickTransparency.isSelected = config.getBoolean(ConfigKeys.clickTransparency)
            showClickTransparencyIcon.isSelected = config.getBoolean(ConfigKeys.showClickTransparencyIcon)
        }
        showViewers.isSelected = config.getBoolean(ConfigKeys.showViewers)
        showImages.isSelected = config.getBoolean(ConfigKeys.showImages)

        showOriginBadges.isSelected = config.getBoolean(ConfigKeys.showOriginBadges)
        showUserBadges.isSelected = config.getBoolean(ConfigKeys.showUserBadges)
        hideDeletedMessages.isSelected = config.getBoolean(ConfigKeys.hideDeletedMessages)
        saveMessageHistory.isSelected = config.getBoolean(ConfigKeys.saveMessageHistory)
        zoomPercent.text = config.getInt(ConfigKeys.zoomPercent).toString()
        deletedMessagePlaceholder.text = config.getString(ConfigKeys.deletedMessagePlaceholder)

        nativeBgColorPicker.value = Color.web(config.getString(ConfigKeys.NativeClient.backgroundColor))
        coloredNicknamesNative.isSelected = config.getBoolean(ConfigKeys.NativeClient.coloredNicknames)
        hideMessagesNative.isSelected = config.getBoolean(ConfigKeys.NativeClient.hideMessages)
        hideMessagesNativeAfter.text = config.getInt(ConfigKeys.NativeClient.hideMessagesAfter).toString()
        showStatusMessagesNative.isSelected = config.getBoolean(ConfigKeys.NativeClient.showStatusMessages)

        externalBgColorPicker.value = Color.web(config.getString(ConfigKeys.ExternalClient.backgroundColor))
        coloredNicknamesExternal.isSelected = config.getBoolean(ConfigKeys.ExternalClient.coloredNicknames)
        hideMessagesExternal.isSelected = config.getBoolean(ConfigKeys.ExternalClient.hideMessages)
        hideMessagesExternalAfter.text = config.getInt(ConfigKeys.ExternalClient.hideMessagesAfter).toString()
        showStatusMessagesExternal.isSelected = config.getBoolean(ConfigKeys.ExternalClient.showStatusMessages)

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
        config.setProperty(ConfigKeys.Goodgame.channel, goodgameChannel.text)
        config.setProperty(ConfigKeys.Twitch.channel, twitchChannel.text)
        config.setProperty(ConfigKeys.Youtube.channel, youtubeChannel.text)

        config.setProperty(ConfigKeys.Goodgame.enabled, goodgameEnabled.isSelected)
        config.setProperty(ConfigKeys.Twitch.enabled, twitchEnabled.isSelected)
        config.setProperty(ConfigKeys.Youtube.enabled, youtubeEnabled.isSelected)

        config.setProperty(ConfigKeys.skin, skin.value.name)
        config.setProperty(ConfigKeys.frame, frame.isSelected)
        config.setProperty(ConfigKeys.onTop, onTop.isSelected)
        if (clickTransparencyEnabled) {
            config.setProperty(ConfigKeys.clickTransparency, clickTransparency.isSelected)
            config.setProperty(ConfigKeys.showClickTransparencyIcon, showClickTransparencyIcon.isSelected)

        }
        config.setProperty(ConfigKeys.showViewers, showViewers.isSelected)
        config.setProperty(ConfigKeys.showImages, showImages.isSelected)

        config.setProperty(ConfigKeys.opacity, opacitySlider.value.toInt())
        config.setProperty(ConfigKeys.showOriginBadges, showOriginBadges.isSelected)
        config.setProperty(ConfigKeys.showUserBadges, showUserBadges.isSelected)
        config.setProperty(ConfigKeys.hideDeletedMessages, hideDeletedMessages.isSelected)
        config.setProperty(ConfigKeys.saveMessageHistory, saveMessageHistory.isSelected)
        config.setProperty(ConfigKeys.zoomPercent, parseZoomPercent(zoomPercent.text))
        config.setProperty(ConfigKeys.deletedMessagePlaceholder, deletedMessagePlaceholder.text)

        config.setProperty(ConfigKeys.NativeClient.backgroundColor, nativeBgColorPicker.value.toHexFormat())
        config.setProperty(ConfigKeys.NativeClient.coloredNicknames, coloredNicknamesNative.isSelected)
        config.setProperty(ConfigKeys.NativeClient.hideMessages, hideMessagesNative.isSelected)
        config.setProperty(
            ConfigKeys.NativeClient.hideMessagesAfter,
            parseHideMessagesAfter(hideMessagesNativeAfter.text)
        )
        config.setProperty(ConfigKeys.NativeClient.showStatusMessages, showStatusMessagesNative.isSelected)

        config.setProperty(ConfigKeys.ExternalClient.backgroundColor, externalBgColorPicker.value.toHexFormat())
        config.setProperty(ConfigKeys.ExternalClient.coloredNicknames, coloredNicknamesExternal.isSelected)
        config.setProperty(ConfigKeys.ExternalClient.hideMessages, hideMessagesExternal.isSelected)
        config.setProperty(
            ConfigKeys.ExternalClient.hideMessagesAfter,
            parseHideMessagesAfter(hideMessagesExternalAfter.text)
        )
        config.setProperty(ConfigKeys.ExternalClient.showStatusMessages, showStatusMessagesExternal.isSelected)

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
