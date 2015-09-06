package failchat.gui;

import javafx.application.Application;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.logging.Logger;

public class GuiBootstrap extends Application {
    public static final Image APP_ICON = new Image(GuiBootstrap.class.getResourceAsStream("/icons/failchat.png"));

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
