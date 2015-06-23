package failchat.core;


import javafx.application.Platform;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class Bootstrap {

    public static Path workDir;

    private static final Logger logger = Logger.getLogger(Bootstrap.class.getName());
    private static Configurator configurator;
    private static MessageManager messageManager;

    public static void main(String[] args) {
        Logging.configure();
        workDir = getWorkDir();
        logger.info("Work dir: " + workDir.toAbsolutePath());

        configurator = Configurator.getInstance();
        messageManager = MessageManager.getInstance();

        new Thread(messageManager, "MessageManagerThread").start();
        new Thread(() -> Gui.main(null), "JavafxLauncher").start(); //TODO: wut?
    }


    private static Path getWorkDir() {
        String path = Bootstrap.class.getResource("").toString();
        if (path.contains(".jar!")) { //production mode
            path = path.split("([\\\\/])([\\w\\-\\.]+?\\.jar!)|(jar:file:[/\\\\])")[1];
            return FileSystems.getDefault().getPath(path);
        } else { //dev mode
            return Paths.get(URI.create(path)).getParent().getParent();
        }
    }

    public static void shutDown() {
        logger.info("Shutting down...");
        configurator.saveConfiguration();
        Platform.exit();
        // чтобы javafx thread'ы смогли завершиться и интерфейс закрывался сразу
        new Thread(() -> {
            configurator.turnOffChatClients();
            messageManager.turnOff();
        }, "ShutdownThread").start();
    }
}
