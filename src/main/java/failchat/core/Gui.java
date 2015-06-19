package failchat.core;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.logging.Logger;

public class Gui extends Application {

    private static final Logger logger = Logger.getLogger(MessageManager.class.getName());
    private static final Path skinsPath = Bootstrap.workDir.resolve("skins");

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("failchat");
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        Scene webScene = new Scene(webView, 350, 500);

        webEngine.load(Bootstrap.workDir.resolve("skins/default/default.html").toAbsolutePath().toUri().toURL().toString());

        //url opening interceptor
        webEngine.locationProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
                if (!newValue.contains(skinsPath.toAbsolutePath().toString())) {
                    logger.fine("Opening url: " + newValue);
                    Platform.runLater(() -> webEngine.getLoadWorker().cancel());
                    getHostServices().showDocument(newValue);
                }
            }
        });

        primaryStage.setScene(webScene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> Bootstrap.shutDown());
        logger.fine("GUI loaded");
    }

    public static void main(String[] args) {
        launch();
    }
}
