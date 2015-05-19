package failchat.core;


import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Bootstrap {

    public static Path workDir;

    private static Configurator configurator;
    private static MessageManager messageManager;

    public static void main(String[] args) {
        Logger.configure();
        workDir = getWorkDir();
        Logger.info("Work dir: " + workDir.toAbsolutePath());

        new Thread(() -> Gui.main(null)).start(); //TODO: wut?
        messageManager = new MessageManager();
        new Thread(messageManager, "MessageManagerThread").start();
        SmileManager sm = SmileManager.getInstance();
        sm.loadSmiles();
        configurator = new Configurator(messageManager);
        configurator.initializeChatClients();

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
        Logger.info("Shutting down...");
        configurator.turnOffChatClients();
        messageManager.turnOff();
    }
}
