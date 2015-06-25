package failchat.twitch;

import failchat.core.*;
import org.pircbotx.Configuration;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.MessageEvent;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.logging.Logger;

public class TwitchChatClient implements ChatClient {

    private static final Logger logger = Logger.getLogger(TwitchChatClient.class.getName());
    private static final String TWITCH_IRC = "irc.twitch.tv";
    private static final int TWITCH_IRC_PORT = 6667;
    private static final String BOT_NAME = "fail_chatbot";
    private static final String BOT_PASSWORD = "oauth:59tune21e6ymz4xg57snr77tcsbg2y"; //don't touch it mate :]
    private static final int RECONNECT_TIMEOUT = 5000;

    private MessageManager messageManager = MessageManager.getInstance();
    private final Queue<Message> messageQueue = messageManager.getMessagesQueue();
    private PircBotX twitchIrcClient;
    private String channelName;
    private List<MessageHandler<TwitchMessage>> messageHandlers;
    private ChatClientStatus status;

    public TwitchChatClient(String channelName) {
        this.channelName = channelName;
        messageHandlers = new ArrayList<>();
        messageHandlers.add(MessageObjectCleaner.getInstance());
        messageHandlers.add(new TwitchSmileHandler());
        messageHandlers.add(new TwitchHighlightHandler(channelName));
        status = ChatClientStatus.READY;
    }

    @Override
    public void goOnline() {
        if (status != ChatClientStatus.READY) {
            return;
        }
        Configuration.ServerEntry[] srv = {new Configuration.ServerEntry(TWITCH_IRC, TWITCH_IRC_PORT)};
        List<Configuration.ServerEntry> srvList = Arrays.asList(srv);
        Configuration configuration = new Configuration.Builder()
                .setName(BOT_NAME)
                .setServerPassword(BOT_PASSWORD)
                .setServers(srvList)
                .addAutoJoinChannel("#" + Configurator.config.getString("twitch.channel").toLowerCase())
                .addListener(new TwitchIrcClient())
                .setAutoReconnect(false)
                .setAutoReconnectDelay(10000)
//                .setAutoReconnectAttempts(20) // bugged, 5 attempts
                .setEncoding(Charset.forName("UTF-8"))
                .setCapEnabled(false)
                .buildConfiguration();

        twitchIrcClient = new PircBotX(configuration);

        new Thread(() -> {
            try {
                twitchIrcClient.startBot();
            } catch (IOException | IrcException e) {
                e.printStackTrace();
            }
        }, "TwitchIrcClientThread").start();
    }

    @Override
    public void goOffline() {
        status = ChatClientStatus.SHUTDOWN;
        twitchIrcClient.close();
    }

    @Override
    public ChatClientStatus getStatus() {
        return status;
    }

    private class TwitchIrcClient extends ListenerAdapter {
        @Override
        public void onConnect(ConnectEvent event) throws Exception {
            twitchIrcClient.sendCAP().request("twitch.tv/tags");
            logger.info("Connected to irc channel: " + channelName);
            messageManager.sendInfoMessage(new InfoMessage(Source.TWITCH, "connected"));
            status = ChatClientStatus.WORKING;
        }

        @Override
        public void onDisconnect(DisconnectEvent event) throws Exception {
            if (status == ChatClientStatus.SHUTDOWN) {
                return;
            } else if (status == ChatClientStatus.WORKING) {
                status = ChatClientStatus.CONNECTING;
                logger.info("disconnected");
                messageManager.sendInfoMessage(new InfoMessage(Source.TWITCH, "disconnected"));
            }
        }

        @Override
        public void onMessage(MessageEvent event) throws Exception {
            TwitchMessage m = new TwitchMessage(event);
            for (MessageHandler<TwitchMessage> mh : messageHandlers) {
                mh.handleMessage(m);
            }
            messageQueue.add(m);
            synchronized (messageQueue) {
                messageQueue.notify();
            }
        }
    }
}

