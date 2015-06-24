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
import java.util.regex.Matcher;

public class Gui extends Application {
    private static final Logger logger = Logger.getLogger(MessageManager.class.getName());
    private static final Path skinsPath = Bootstrap.workDir.resolve("skins");

    private Configurator configurator = Configurator.getInstance();
    private Stage settingsStage;
    private Stage decoratedStage;
    private Stage undecoratedChatStage;
    private boolean decorated;
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
        decoratedStage = buildChatStage(true);
        undecoratedChatStage = buildChatStage(false);
        webScene = buildChatScene();
        buildContextMenu(webScene);

        settingsStage.setOnCloseRequest(event -> {
            saveSettingsValues();
            Bootstrap.shutDown();
        });

        settingsStage.show();
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

    // build 2 times: for decorated and undecorated stages
    private Stage buildChatStage(boolean decorated) {
        Stage stage = new Stage();
        stage.setTitle("failchat");
        if (!decorated) {
            stage.initStyle(StageStyle.UNDECORATED);
        }
        stage.setOnCloseRequest(event -> {
            saveChatPosition(stage);
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
                switchStage();
            }
            // space
            else if (key.getCode() == KeyCode.SPACE) {
                switchDecorations();
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
                    Matcher matcher = LinkHandler.URL_PATTERN.matcher(newLocation);
                    if (matcher.find()) {
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

    private ContextMenu buildContextMenu(Scene scene) {
        MenuItem switchDecorationsItem = new MenuItem("Show/hide frame");
        MenuItem toSettingsItem = new MenuItem("Settings");
        ContextMenu contextMenu = new ContextMenu(switchDecorationsItem, toSettingsItem);

        //context menu
        webScene.setOnContextMenuRequested((event) -> {
            contextMenu.show(scene.getRoot(), event.getScreenX(), event.getScreenY());
        });
        webScene.setOnMouseClicked((mouseEvent) -> {
            if (contextMenu.isShowing()) {
                contextMenu.hide();
            }
        });

        //menu items
        switchDecorationsItem.setOnAction((event) -> switchDecorations());
        toSettingsItem.setOnAction((event) -> switchStage());

        return contextMenu;
    }

    //invokes every time when switched to chat stage or switched between decorated and undecorated stages
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

    private void saveChatPosition(Stage stage) {
        Configurator.config.setProperty("chat.width", (int)stage.getWidth());
        Configurator.config.setProperty("chat.height", (int)stage.getHeight());
        Configurator.config.setProperty("chat.x", (int)stage.getX());
        Configurator.config.setProperty("chat.y", (int)stage.getY());
    }

    private void switchStage() {
        //settings -> chat
        if (settingsStage.isShowing()) {
            saveSettingsValues();
            settingsStage.hide();
            if (Configurator.config.getBoolean("frame")) {
                decorated = true;
                decoratedStage.setScene(webScene);
                configureChatStage(decoratedStage);
                decoratedStage.show();
            }
            else {
                decorated = false;
                undecoratedChatStage.setScene(webScene);
                configureChatStage(undecoratedChatStage);
                undecoratedChatStage.show();
            }
            String skin = Configurator.config.getString("skin");
            try {
                webEngine.load(Bootstrap.workDir.resolve("skins").resolve(skin).resolve(skin + ".html").toUri().toURL().toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            configureChatStage(decoratedStage);
            new Thread(configurator::initializeChatClients, "ChatClientsInitializer").start();
        }

        //chat -> settings
        else {
            if (decorated) {
                saveChatPosition(decoratedStage);
            }
            else {
                saveChatPosition(undecoratedChatStage);
            }
            decoratedStage.hide();
            undecoratedChatStage.hide();
            webEngine.loadContent("");
            frame.setSelected(Configurator.config.getBoolean("frame"));
            settingsStage.show();
            configurator.turnOffChatClients();
        }
    }

    private void switchDecorations() {
        // decorated -> undecorated
        if (decorated) {
            decoratedStage.hide();
            saveChatPosition(decoratedStage);
            Configurator.config.setProperty("frame", false);
            configureChatStage(undecoratedChatStage);
            undecoratedChatStage.setScene(webScene);
            undecoratedChatStage.show();
            decorated = false;
            //TODO: edit checkbox value or something
        }

        // undecorated -> decorated
        else {
            undecoratedChatStage.hide();
            saveChatPosition(undecoratedChatStage);
            Configurator.config.setProperty("frame", true);
            configureChatStage(decoratedStage);
            decoratedStage.setScene(webScene);
            decoratedStage.show();
            decorated = true;
        }
        logger.fine("Chat stage switched. Decorated: " + decorated);
    }
}
