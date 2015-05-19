package failchat.core;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class Gui extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("failchat");
        WebView webView = new WebView();
        WebEngine webEngine = webView.getEngine();
        Scene webScene = new Scene(webView, 350, 500);

        webEngine.load(Bootstrap.workDir.resolve("skins/default/default.html").toAbsolutePath().toUri().toURL().toString());

        primaryStage.setScene(webScene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> Bootstrap.shutDown());
    }

    public static void main(String[] args) {
        launch();
    }
}
