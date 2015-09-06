package failchat.gui;

import failchat.core.Bootstrap;
import failchat.core.Configurator;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class SettingsFrame {
    Stage stage;
    ChatFrame chat;

    //settings nodes
    private TextField sc2tvChannel;
    private TextField goodgameChannel;
    private TextField twitchChannel;
    private TextField cybergameChannel;
    private CheckBox sc2tvEnabled;
    private CheckBox goodgameEnabled;
    private CheckBox twitchEnabled;
    private CheckBox cybergameEnabled;
    private ChoiceBox skin;
    private ColorPicker bgColorPicker;
    private CheckBox frame;
    private CheckBox onTop;
    private CheckBox showViewers;
    private CheckBox showImages;
    private Slider opacitySlider;
    private Button applyButton;

    SettingsFrame(Stage stage) throws Exception {
        this.stage = stage;
        stage.setTitle("failchat v" + Configurator.config.getString("version"));
        stage.getIcons().setAll(GuiBootstrap.APP_ICON);
        Scene scene = new Scene(FXMLLoader.load(getClass().getResource("/settings.fxml")));
        stage.setScene(scene);

        //channels
        sc2tvChannel = (TextField)scene.lookup("#sc2tv_channel");
        goodgameChannel = (TextField)scene.lookup("#goodgame_channel");
        twitchChannel = (TextField)scene.lookup("#twitch_channel");
        cybergameChannel = (TextField)scene.lookup("#cybergame_channel");

        //channels checkboxes
        sc2tvEnabled = (CheckBox)scene.lookup("#sc2tv_enabled");
        goodgameEnabled = (CheckBox)scene.lookup("#goodgame_enabled");
        twitchEnabled = (CheckBox)scene.lookup("#twitch_enabled");
        cybergameEnabled = (CheckBox)scene.lookup("#cybergame_enabled");

        //appearance
        skin = (ChoiceBox)scene.lookup("#skin");
        bgColorPicker = (ColorPicker)scene.lookup("#bgcolor");
        frame = (CheckBox)scene.lookup("#frame");
        onTop = (CheckBox)scene.lookup("#top");
        showViewers= (CheckBox)scene.lookup("#show_viewers");
        showImages= (CheckBox)scene.lookup("#show_images");

        //opacity
        opacitySlider = (Slider)scene.lookup("#opacity");
        Text opacityText = (Text)scene.lookup("#opacity_text");
        opacitySlider.valueProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                opacityText.setText(Integer.toString(newValue.intValue()));
            }
        });

        applyButton = (Button)scene.lookup("#apply_button");
        applyButton.setOnAction((action) -> toChat());

        stage.setOnCloseRequest(event -> {
            saveSettingsValues();
            Bootstrap.shutDown();
        });
    }

    void show() {
        updateSettingsValues();
        stage.show();
    }

    private void toChat() {
        saveSettingsValues();
        stage.hide();
        chat.show();
    }

    private void updateSettingsValues() {
        sc2tvChannel.setText(Configurator.config.getString("sc2tv.channel"));
        goodgameChannel.setText(Configurator.config.getString("goodgame.channel"));
        twitchChannel.setText(Configurator.config.getString("twitch.channel"));
        cybergameChannel.setText(Configurator.config.getString("cybergame.channel"));

        sc2tvEnabled.setSelected(Configurator.config.getBoolean("sc2tv.enabled"));
        goodgameEnabled.setSelected(Configurator.config.getBoolean("goodgame.enabled"));
        twitchEnabled.setSelected(Configurator.config.getBoolean("twitch.enabled"));
        cybergameEnabled.setSelected(Configurator.config.getBoolean("cybergame.enabled"));

        skin.setItems(FXCollections.observableArrayList(Configurator.getSkins()));
        skin.setValue(Configurator.config.getString("skin"));
        bgColorPicker.setValue(Color.web(Configurator.config.getString("bgcolor")));
        frame.setSelected(Configurator.config.getBoolean("frame"));
        showViewers.setSelected(Configurator.config.getBoolean("showViewers"));
        showImages.setSelected(Configurator.config.getBoolean("showImages"));
        onTop.setSelected(Configurator.config.getBoolean("onTop"));
        opacitySlider.setValue(Configurator.config.getDouble("opacity"));
    }

    private void saveSettingsValues() {
        Configurator.config.setProperty("sc2tv.channel", sc2tvChannel.getText());
        Configurator.config.setProperty("goodgame.channel", goodgameChannel.getText());
        Configurator.config.setProperty("twitch.channel", twitchChannel.getText());
        Configurator.config.setProperty("cybergame.channel", cybergameChannel.getText());

        Configurator.config.setProperty("sc2tv.enabled", sc2tvEnabled.isSelected());
        Configurator.config.setProperty("goodgame.enabled", goodgameEnabled.isSelected());
        Configurator.config.setProperty("twitch.enabled", twitchEnabled.isSelected());
        Configurator.config.setProperty("cybergame.enabled", cybergameEnabled.isSelected());

        Configurator.config.setProperty("skin", skin.getValue());
        Configurator.config.setProperty("bgcolor", bgColorPicker.getValue().toString());
        Configurator.config.setProperty("frame", frame.isSelected());
        Configurator.config.setProperty("onTop", onTop.isSelected());
        Configurator.config.setProperty("showViewers", showViewers.isSelected());
        Configurator.config.setProperty("showImages", showImages.isSelected());
        Configurator.config.setProperty("opacity", (int) opacitySlider.getValue());
    }
}
