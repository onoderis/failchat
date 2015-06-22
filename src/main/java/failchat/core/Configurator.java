package failchat.core;

import failchat.goodgame.GGChatClient;
import failchat.sc2tv.Sc2tvChatClient;
import failchat.twitch.TwitchChatClient;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class Configurator {
    private static final Logger logger = Logger.getLogger(Configurator.class.getName());

    private static volatile Configurator instance;

    public static Configurator getInstance() {
        Configurator localInstance = instance;
        if (localInstance == null) {
            synchronized (Configurator.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new Configurator();
                }
            }
        }
        return localInstance;
    }

    public static final CompositeConfiguration config = new CompositeConfiguration();
    private static final Path configPath = Bootstrap.workDir.resolve("config.conf");
    private MessageManager messageManager = MessageManager.getInstance();

    private Map<Source, ChatClient> chatClients = new HashMap<>();
    private PropertiesConfiguration defaultConfig;
    private PropertiesConfiguration myConfig;

    private Configurator() {
        try {
            defaultConfig = new PropertiesConfiguration(getClass().getResource("/default.conf"));
            myConfig = new PropertiesConfiguration(configPath.toFile());
            config.addConfiguration(myConfig, true);
            config.addConfiguration(defaultConfig);
        } catch (ConfigurationException e) {
            logger.severe("Bad configuration file");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void initializeChatClients() {
        if (Configurator.config.getBoolean("sc2tv.enabled") && !Configurator.config.getString("sc2tv.channel").equals("")) {
            ChatClient sc2tvChatClient = new Sc2tvChatClient(Configurator.config.getString("sc2tv.channel"));
            chatClients.put(Source.SC2TV, sc2tvChatClient);
        }

        if (Configurator.config.getBoolean("goodgame.enabled") && !Configurator.config.getString("goodgame.channel").equals("")) {
            GGChatClient ggcc = new GGChatClient(Configurator.config.getString("goodgame.channel"));
            chatClients.put(Source.GOODGAME, ggcc);
        }

        if (Configurator.config.getBoolean("twitch.enabled") && !Configurator.config.getString("twitch.channel").equals("")) {
            TwitchChatClient twitchChatClient = new TwitchChatClient(Configurator.config.getString("twitch.channel"));
            chatClients.put(Source.TWITCH, twitchChatClient);
        }

        if (Configurator.config.getBoolean("test.enabled")) {
            TestChatClient tcc = new TestChatClient(messageManager.getMessagesQueue());
            chatClients.put(Source.TEST, tcc);
        }
        chatClients.values().forEach(ChatClient::goOnline);
    }

    public void turnOffChatClients() {
        chatClients.values().forEach(ChatClient::goOffline);
        chatClients.clear();
    }

    public void saveConfiguration() {
        try {
            myConfig.save();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getSkins() {
        // TODO: добавить фильтр для папок где есть <dirname>.html
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Bootstrap.workDir.resolve("skins"))) {
            ArrayList<String> dirs = new ArrayList<>();
            for (Path dir : stream) {
                dirs.add(dir.getFileName().toString());
            }
            return dirs;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


}
