package failchat.core;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class Gui extends Application {

    private static final Logger logger = Logger.getLogger(MessageManager.class.getName());
    private static final Path skinsPath = Bootstrap.workDir.resolve("skins");

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("failchat");
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        webView.setContextMenuEnabled(false);
        Scene webScene = new Scene(webView, 350, 500);
        webEngine.load(Bootstrap.workDir.resolve("skins/default/default.html").toAbsolutePath().toUri().toURL().toString());

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
        primaryStage.setScene(webScene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> Bootstrap.shutDown());
        logger.fine("GUI loaded");
    }

    public static void main(String[] args) {
        launch();
    }
}
