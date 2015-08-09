package failchat.gui;

import javafx.application.Application;
import javafx.stage.Stage;

import java.util.logging.Logger;

public class GuiBootstrap extends Application {
    private static final Logger logger = Logger.getLogger(GuiBootstrap.class.getName());

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        SettingsFrame settings = new SettingsFrame(primaryStage);
        ChatFrame chat = new ChatFrame();
        settings.chat = chat;
        chat.settings = settings;
        chat.app = this;

        settings.show();
        logger.info("GUI loaded");
    }
}
