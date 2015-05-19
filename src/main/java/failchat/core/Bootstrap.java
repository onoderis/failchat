package failchat.core;


import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

public class Bootstrap {

    public static Path workDir;

    private static Configurator configurator;
    private static MessageManager messageManager;

    public static void main(String[] args) {
        String path = Bootstrap.class.getResource("").toString();
        if (path.contains(".jar!")) { //production mode
            path = path.split("([\\\\/])([\\w\\-\\.]+?\\.jar!)|(jar:file:[/\\\\])")[1];
            workDir = FileSystems.getDefault().getPath(path);
        } else { //dev mode
            workDir = Paths.get(URI.create(path)).getParent().getParent();
        }
        System.out.println("Work dir: " + workDir.toAbsolutePath());

        new Thread(() -> Gui.main(null)).start();
        messageManager = new MessageManager();
        SmileManager sm = SmileManager.getInstance();
        sm.loadSmiles();
        configurator = new Configurator(messageManager);
        configurator.initializeChatClients();

        new Thread(messageManager, "MessageManagerThread").start();
    }

    public static void shutDown() {
        System.out.println("Shutting down...");
        configurator.turnOffChatClients();
        messageManager.turnOff();
    }
}
