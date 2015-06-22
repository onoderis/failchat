package failchat.core;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class Gui extends Application {

    private static final Logger logger = Logger.getLogger(MessageManager.class.getName());
    private static final Path skinsPath = Bootstrap.workDir.resolve("skins");

    private Configurator configurator = Configurator.getInstance();

    private Stage settingsStage;
    private Stage chatStage;
    private Scene webScene;
    private WebEngine webEngine;

    //settings nodes
    private TextField sc2tvChannel;
    private TextField goodgameChannel;
    private TextField twitchChannel;
    private CheckBox sc2tvEnabled;
    private CheckBox goodgameEnabled;
    private CheckBox twitchEnabled;
    private ChoiceBox skin;
    private CheckBox frame;
    private CheckBox onTop;
    private Slider opacitySlider;
    private Button applyButton;


    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        settingsStage = primaryStage;
        buildSettingsStage(settingsStage);
        updateSettingsValues();
        chatStage = buildChatStage();
        webScene = buildChatScene();
        chatStage.setScene(webScene);
        settingsStage.show();

        settingsStage.setOnCloseRequest(event -> {
            saveSettingsValues();
            Bootstrap.shutDown();
        });
        logger.info("GUI loaded");
    }

    private void buildSettingsStage(Stage stage) throws Exception {
        stage.setTitle("failchat settings");
        Scene scene = new Scene(FXMLLoader.load(getClass().getResource("/settings.fxml")));
        stage.setScene(scene);

        //channels
        sc2tvChannel = (TextField)scene.lookup("#sc2tv_channel");
        goodgameChannel = (TextField)scene.lookup("#goodgame_channel");
        twitchChannel = (TextField)scene.lookup("#twitch_channel");

        //channels checkboxes
        sc2tvEnabled = (CheckBox)scene.lookup("#sc2tv_enabled");
        goodgameEnabled = (CheckBox)scene.lookup("#goodgame_enabled");
        twitchEnabled = (CheckBox)scene.lookup("#twitch_enabled");

        //skin
        skin = (ChoiceBox)scene.lookup("#skin");
        frame = (CheckBox)scene.lookup("#frame");
        onTop = (CheckBox)scene.lookup("#top");

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
        applyButton.setOnAction((action) -> switchStage());
    }

    private void updateSettingsValues() {
        sc2tvChannel.setText(Configurator.config.getString("sc2tv.channel"));
        goodgameChannel.setText(Configurator.config.getString("goodgame.channel"));
        twitchChannel.setText(Configurator.config.getString("twitch.channel"));

        sc2tvEnabled.setSelected(Configurator.config.getBoolean("sc2tv.enabled"));
        goodgameEnabled.setSelected(Configurator.config.getBoolean("goodgame.enabled"));
        twitchEnabled.setSelected(Configurator.config.getBoolean("twitch.enabled"));

        skin.setItems(FXCollections.observableArrayList(Configurator.getSkins()));
        skin.setValue(Configurator.config.getString("skin"));
        frame.setSelected(Configurator.config.getBoolean("frame"));
        onTop.setSelected(Configurator.config.getBoolean("onTop"));
        opacitySlider.setValue(Configurator.config.getDouble("opacity"));
    }

    private void saveSettingsValues() {
        Configurator.config.setProperty("sc2tv.channel", sc2tvChannel.getText());
        Configurator.config.setProperty("goodgame.channel", goodgameChannel.getText());
        Configurator.config.setProperty("twitch.channel", twitchChannel.getText());

        Configurator.config.setProperty("sc2tv.enabled", sc2tvEnabled.isSelected());
        Configurator.config.setProperty("goodgame.enabled", goodgameEnabled.isSelected());
        Configurator.config.setProperty("twitch.enabled", twitchEnabled.isSelected());

        Configurator.config.setProperty("skin", skin.getValue());
        Configurator.config.setProperty("frame", frame.isSelected());
        Configurator.config.setProperty("onTop", onTop.isSelected());
        Configurator.config.setProperty("opacity", (int)opacitySlider.getValue());
    }

    // could be invoked several times if frame setting changed
    private Stage buildChatStage() {
        Stage stage = new Stage();
        stage.setTitle("failchat");
        if (!Configurator.config.getBoolean("frame")) {
            stage.initStyle(StageStyle.UNDECORATED);
        }
        stage.setOnCloseRequest(event -> {
            saveChatPosition();
            Bootstrap.shutDown();
        });
       return stage;
    }

    // invokes 1 time
    private Scene buildChatScene() {
        WebView webView = new WebView();
        webEngine = webView.getEngine();
        Scene webScene = new Scene(webView);
        webView.setContextMenuEnabled(false);

        //hot keys
        webScene.setOnKeyPressed((key) -> {
            //esc
            if (key.getCode() == KeyCode.ESCAPE) {
                saveChatPosition();
                switchStage();
            }
        });

        // url opening interceptor
        webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
            @Override
            public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) {
                // WebEngine.locationProperty не изменяется обратно после LoadWorker.cancel()
                // locationProperty заменяется сразу, как и Worker.State
                if (newValue == Worker.State.SCHEDULED) {
                    String newLocation = webEngine.getLocation();
                    if (newLocation.contains("http://")) {
                        Platform.runLater(() -> webEngine.getLoadWorker().cancel());
                        getHostServices().showDocument(webEngine.getLocation());
                        logger.fine("Opening url: " + webEngine.getLocation());
                    } else if (newLocation.contains("file:///")) {
                        Path newLocationPath = Paths.get(newLocation.split("file:///")[1]);
                        if (newLocationPath.startsWith(skinsPath)) {
                            logger.fine("Opening skin: " + webEngine.getLocation());
                        }
                    }
                }
            }
        });
        return webScene;
    }

    //invokes every time when switched to chat stage
    private void configureChatStage(Stage stage) {
        stage.setOpacity(Configurator.config.getDouble("opacity") / 100);
        stage.setAlwaysOnTop(Configurator.config.getBoolean("onTop"));
        stage.setWidth(Configurator.config.getDouble("chat.width"));
        stage.setHeight(Configurator.config.getDouble("chat.height"));
        double x = Configurator.config.getDouble("chat.x");
        double y = Configurator.config.getDouble("chat.y");
        if (x != -1 && y != -1) {
            stage.setX(x);
            stage.setY(y);
        }
//        primaryStage.iconifiedProperty().addListener(new ChangeListener<Boolean>() {
//            @Override
//            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
//                if (newValue) {
//                    logger.info("uniconifying");
//                    primaryStage.setIconified(false);
//                }
//            }
//        });
    }

    private void saveChatPosition() {
        Configurator.config.setProperty("chat.width", (int)chatStage.getWidth());
        Configurator.config.setProperty("chat.height", (int)chatStage.getHeight());
        Configurator.config.setProperty("chat.x", (int)chatStage.getX());
        Configurator.config.setProperty("chat.y", (int)chatStage.getY());
    }

    private void switchStage() {
        //settings -> chat
        if (settingsStage.isShowing()) {
            saveSettingsValues();
            settingsStage.hide();
            // if stage style changed
            if (!Configurator.config.getBoolean("frame") == (chatStage.getStyle() == StageStyle.DECORATED)) {
                chatStage = buildChatStage();
                chatStage.setScene(webScene);
                logger.fine("Stage rebuilded");
            }
            String skin = Configurator.config.getString("skin");
            try {
                webEngine.load(Bootstrap.workDir.resolve("skins").resolve(skin).resolve(skin + ".html").toUri().toURL().toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            configureChatStage(chatStage);
            chatStage.show();
            new Thread(configurator::initializeChatClients, "ChatClientsInitializer").start();
        }

        //chat -> settings
        else {
            chatStage.hide();
            webEngine.loadContent("");
            settingsStage.show();
            configurator.turnOffChatClients();
        }
    }
}
