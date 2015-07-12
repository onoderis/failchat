package failchat.core;


import javafx.application.Platform;

import java.io.File;
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

        Logging.configureLoggingInFile();

        new Thread(messageManager, "MessageManagerThread").start();
        new Thread(() -> Gui.main(null), "JavafxLauncher").start(); //TODO: wut?

        Thread smlThread = new Thread(SmileManager::loadSmilesInfo, "SmileLoaderThread");
        smlThread.setPriority(3);
        smlThread.start();
    }


    private static Path getWorkDir() {
        String path = Bootstrap.class.getResource("").toString();
        if (path.contains(".jar!")) { //production mode
            logger.info("Dev mode");
            File cp = new File(System.getProperty("java.class.path"));
            File dir = cp.getAbsoluteFile().getParentFile();
            return FileSystems.getDefault().getPath(dir.toString());
        } else { //dev mode
            logger.info("Dev mode");
            return Paths.get(URI.create(path)).getParent().getParent();
        }
    }

    public static void shutDown() {
        logger.info("Shutting down...");
        Thread terminationThread = new Thread(() -> {
            try {
                Thread.sleep(20000);
                logger.info("Process terminated...");
                System.exit(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "TerminationThread");
        terminationThread.setDaemon(true);
        terminationThread.start();
        configurator.saveConfiguration();
        Platform.exit();
        // чтобы javafx thread'ы смогли завершиться и интерфейс закрывался сразу
        new Thread(() -> {
            configurator.turnOffChatClients();
            messageManager.turnOff();
        }, "ShutdownThread").start();
    }
}
