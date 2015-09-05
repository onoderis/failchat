package failchat.gui;

import failchat.core.Bootstrap;
import failchat.core.Configurator;
import failchat.handlers.LinkHandler;
import failchat.utils.ColorUtil;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.regex.Matcher;

public class ChatFrame {
    private static final Logger logger = Logger.getLogger(ChatFrame.class.getName());

    SettingsFrame settings;
    Application app;

    private Stage decoratedChatStage;
    private Stage undecoratedChatStage; //for opaque background color
    private Stage transparentChatStage; //for transparent background color
    private Stage currentChatStage;
    private Scene chatScene;
    private WebEngine webEngine;

    private Configurator configurator = Configurator.getInstance();
    private Path skinsPath = Bootstrap.workDir.resolve("skins");

    ChatFrame() {
        decoratedChatStage = buildChatStage(0);
        undecoratedChatStage = buildChatStage(1);
        transparentChatStage = buildChatStage(2);
        chatScene = buildChatScene();
        buildContextMenu(chatScene);
    }

    void show() {
        Color bgColor = Color.web(Configurator.config.getString("bgcolor"));
        if (Configurator.config.getBoolean("frame")) {
            currentChatStage = decoratedChatStage;
            chatScene.setFill(ColorUtil.getOpaque(bgColor));
        }
        else {
            if (bgColor.isOpaque()) {
                currentChatStage = undecoratedChatStage;
            } else {
                currentChatStage = transparentChatStage;
            }
            chatScene.setFill(bgColor);
        }

        currentChatStage.setScene(chatScene);
        configureChatStage(currentChatStage);
        currentChatStage.show();
        String skin = Configurator.config.getString("skin");
        try {
            webEngine.load(Bootstrap.workDir.resolve("skins").resolve(skin).resolve(skin + ".html").toUri().toURL().toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            configurator.initializeChatClients();
            configurator.configureViewersManager();
        }, "ChatClientsInitializer").start();
    }

    private Stage buildChatStage(int type) { // 0 - decorated, 1 - undecorated, 2 - transparent
        Stage stage = new Stage();
        switch (type) {
            case 0: {
                stage.setTitle("failchat");
                break;
            }
            case 1: {
                stage.setTitle("failchat u");
                stage.initStyle(StageStyle.UNDECORATED);
                break;
            }
            case 2: {
                stage.setTitle("failchat t");
                stage.initStyle(StageStyle.TRANSPARENT);
                break;
            }
        }
        stage.setOnCloseRequest(event -> {
            saveChatPosition(stage);
            Bootstrap.shutDown();
        });
        return stage;
    }

    private Scene buildChatScene() {
        WebView webView = new WebView();
        webEngine = webView.getEngine();
        webEngine.setUserAgent(webEngine.getUserAgent().concat("/failchat"));
        Scene chatScene = new Scene(webView);
        webView.setStyle("-fx-background-color: transparent;");
        webView.setContextMenuEnabled(false);

        //webengine transparent hack
        webEngine.documentProperty().addListener((observable, oldValue, newValue) -> {
            try {
                Field f = webEngine.getClass().getDeclaredField("page");
                f.setAccessible(true);
                com.sun.webkit.WebPage page = (com.sun.webkit.WebPage) f.get(webEngine);
                page.setBackgroundColor(0); //full transparent
            } catch (Exception ignored) {}
        });

        //hot keys
        chatScene.setOnKeyReleased((key) -> {
            //esc
            if (key.getCode() == KeyCode.ESCAPE) {
                toSettings();
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
                        app.getHostServices().showDocument(webEngine.getLocation());
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
        return chatScene;
    }

    private ContextMenu buildContextMenu(Scene scene) {
        MenuItem switchDecorationsItem = new MenuItem("Show/hide frame");
        MenuItem toSettingsItem = new MenuItem("Settings");
        MenuItem viewersItem = new MenuItem("Show/hide viewers");
        ContextMenu contextMenu = new ContextMenu(switchDecorationsItem, viewersItem, toSettingsItem);

        //context menu
        scene.setOnMouseClicked((mouseEvent) -> {
            if (mouseEvent.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(scene.getRoot(), mouseEvent.getScreenX(), mouseEvent.getScreenY());
            } else if (contextMenu.isShowing() && mouseEvent.getEventType() == MouseEvent.MOUSE_CLICKED) {
                contextMenu.hide();
            }
        });

        //menu items
        switchDecorationsItem.setOnAction((event) -> switchDecorations());
        toSettingsItem.setOnAction((event) -> toSettings());
        viewersItem.setOnAction((event) -> {
            Configurator.config.setProperty("showViewers", !Configurator.config.getBoolean("showViewers"));
            Configurator.getInstance().configureViewersManager();
        });

        return contextMenu;
    }

    private void toSettings() {
        saveChatPosition(currentChatStage);
        currentChatStage.hide();
        webEngine.loadContent("");
        settings.show();
        configurator.getViewersManager().stop();
        configurator.turnOffChatClients();
    }

    private void switchDecorations() {
        boolean toDecorated = currentChatStage == undecoratedChatStage ||  currentChatStage == transparentChatStage;
        Stage fromChatStage = currentChatStage;
        Color bgColor = Color.web(Configurator.config.getString("bgcolor"));
        if (toDecorated) {
            currentChatStage = decoratedChatStage;
            chatScene.setFill(ColorUtil.getOpaque(bgColor));
            Configurator.config.setProperty("frame", true);
        }
        else {
            //to undecorated
            if (bgColor.isOpaque()) {
                currentChatStage = undecoratedChatStage;
            }
            //to transparent
            else {
                currentChatStage = transparentChatStage;
            }
            chatScene.setFill(bgColor);
            Configurator.config.setProperty("frame", false);
        }

        fromChatStage.hide();
        saveChatPosition(fromChatStage);
        configureChatStage(currentChatStage);
        currentChatStage.setScene(chatScene);
        currentChatStage.show();
        logger.fine("Chat stage switched. Decorated: " + toDecorated);
    }

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
    }

    private void saveChatPosition(Stage stage) {
        Configurator.config.setProperty("chat.width", (int) stage.getWidth());
        Configurator.config.setProperty("chat.height", (int) stage.getHeight());
        if (stage.getX() >= -10000 && stage.getY() >= -10000) { // -32k x -32k fix
            Configurator.config.setProperty("chat.x", (int) stage.getX());
            Configurator.config.setProperty("chat.y", (int) stage.getY());
        }
    }
}
